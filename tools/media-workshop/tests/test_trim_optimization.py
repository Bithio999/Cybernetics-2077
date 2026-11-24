"""Exercise actual fast/exact cuts, keyframes, timestamps and playable copied packets."""

import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import unittest
from unittest.mock import patch

from test_workshop import Fixture, app
from test_download_optimization import packet_hashes


def video_packets(path):
    return json.loads(
        subprocess.check_output(
            [
                app.executable("ffprobe"),
                "-v",
                "error",
                "-select_streams",
                "v:0",
                "-show_packets",
                "-show_data_hash",
                "sha256",
                "-show_entries",
                "packet=pts_time,flags,data_hash",
                "-of",
                "json",
                str(path),
            ]
        )
    )["packets"]


class TrimOptimization(Fixture, unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.indexed = Path(cls.fixture.name) / "indexed.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "testsrc2=size=320x180:rate=10",
                "-f",
                "lavfi",
                "-i",
                "sine=sample_rate=48000",
                "-t",
                "8",
                "-c:v",
                "libx264",
                "-g",
                "20",
                "-keyint_min",
                "20",
                "-sc_threshold",
                "0",
                "-threads",
                "2",
                "-c:a",
                "aac",
                str(cls.indexed),
            ],
            check=True,
        )

    def import_source(self, source):
        path = self.workshop.directory / source.name
        shutil.copy2(source, path)
        return self.workshop.register(path, source.stem)

    def export(self, source, fmt="mp4", start=3.25, end=5.25, mode="fast"):
        asset = self.import_source(source)
        ids = self.workshop.trim([asset["id"]], fmt, start, end, mode)
        result = self.assert_success(ids)[0]
        return self.workshop.jobs[ids[0]], self.workshop.asset_path(result), asset

    def test_fast_cut_starts_at_real_keyframe_and_preserves_compressed_video(self):
        original = hashlib.sha256(self.indexed.read_bytes()).hexdigest()
        job, output, asset = self.export(self.indexed)
        self.assertEqual(job["trim_start"], 2)
        self.assertIn("stream copy", job["method"])
        source_packets = video_packets(self.indexed)
        output_packets = video_packets(output)
        first_key = next(
            p for p in source_packets if "K" in p["flags"] and float(p["pts_time"]) == 2
        )
        self.assertEqual(output_packets[0]["data_hash"], first_key["data_hash"])
        self.assertAlmostEqual(float(output_packets[0]["pts_time"]), 0, delta=0.001)
        self.assertTrue(
            all(
                p["data_hash"] in {s["data_hash"] for s in source_packets}
                for p in output_packets
            )
        )
        decoded = subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(output),
                "-f",
                "null",
                "-",
            ],
            capture_output=True,
        )
        self.assertEqual(decoded.returncode, 0, decoded.stderr)
        self.assertEqual(decoded.stderr, b"")
        self.assertEqual(
            hashlib.sha256(self.workshop.asset_path(asset).read_bytes()).hexdigest(),
            original,
        )

    def test_exact_cut_retains_requested_start_and_duration(self):
        job, output, _ = self.export(self.indexed, mode="exact")
        self.assertEqual(job["trim_start"], 3.25)
        self.assertEqual(job["method"], "Exact trim")
        self.assertAlmostEqual(app.probe(output)["duration"], 2, delta=0.11)

    def test_fast_mp3_preserves_original_audio_packets(self):
        source = Path(self.temp.name) / "native.mp3"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.indexed),
                "-vn",
                "-c:a",
                "libmp3lame",
                "-b:a",
                "128k",
                str(source),
            ],
            check=True,
        )
        job, output, _ = self.export(source, fmt="mp3", start=1.2, end=2.7)
        self.assertIn("stream copy", job["method"])
        before, after = packet_hashes(source, "a:0"), packet_hashes(output, "a:0")
        self.assertTrue(after)
        self.assertTrue(
            any(before[i : i + len(after)] == after for i in range(len(before)))
        )
        self.assertAlmostEqual(app.probe(output)["duration"], 1.5, delta=0.12)

    def test_fast_aac_to_mp3_still_converts_audio(self):
        job, output, _ = self.export(self.indexed, fmt="mp3", start=1.2, end=2.7)
        self.assertIn("audio conversion", job["method"])
        self.assertEqual(app.probe(output)["audio_codec"], "mp3")
        self.assertAlmostEqual(app.probe(output)["duration"], 1.5, delta=0.12)

    def test_nonzero_source_timestamps_are_normalized_for_keyframe_seek(self):
        source = Path(self.temp.name) / "offset.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.indexed),
                "-c",
                "copy",
                "-output_ts_offset",
                "100",
                str(source),
            ],
            check=True,
        )
        job, output, _ = self.export(source)
        self.assertAlmostEqual(
            job["trim_start"], 102 - app.probe(source)["start_time"], delta=0.001
        )
        packet = next(
            p
            for p in video_packets(source)
            if "K" in p["flags"] and float(p["pts_time"]) == 102
        )
        self.assertEqual(video_packets(output)[0]["data_hash"], packet["data_hash"])
        self.assertAlmostEqual(
            float(video_packets(output)[0]["pts_time"]), 0, delta=0.001
        )

    def test_no_usable_keyframe_falls_back_to_exact_instead_of_wrong_cut(self):
        with patch.object(app, "keyframe_before", return_value=None):
            job, output, _ = self.export(self.indexed)
        self.assertIn("Exact fallback", job["method"])
        self.assertEqual(job["trim_start"], 3.25)
        self.assertAlmostEqual(app.probe(output)["duration"], 2, delta=0.11)

    def test_fast_batch_keeps_each_source_and_records_mode(self):
        first = self.import_source(self.indexed)
        second = self.workshop.register(
            self.workshop.asset_path(first), "Second source"
        )
        ids = self.workshop.trim([first["id"], second["id"]], "mp4", 3.25, 5.25, "fast")
        outputs = self.assert_success(ids)
        self.assertEqual(len({a["path"] for a in outputs}), 2)
        self.assertTrue(
            all(self.workshop.jobs[i]["spec"]["mode"] == "fast" for i in ids)
        )
        self.assertTrue(self.workshop.asset_path(first).exists())

    def test_invalid_mode_is_rejected_before_creating_jobs(self):
        with self.assertRaises(app.MediaError):
            self.workshop.trim([self.media["id"]], "mp4", 0, 1, "turbo")
        self.assertFalse(self.workshop.jobs)

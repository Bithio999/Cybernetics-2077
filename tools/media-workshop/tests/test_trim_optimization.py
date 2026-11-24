"""One accurate workflow, automatic copying of interior GOPs, and safe fallbacks."""

import hashlib
import importlib.util
from pathlib import Path
import shutil
import subprocess
import unittest
from unittest.mock import patch

from test_workshop import Fixture, app
from test_download_optimization import packet_hashes
from test_workshop import until, completed


def pcm(path):
    return subprocess.check_output(
        [
            app.executable("ffmpeg"),
            "-v",
            "error",
            "-i",
            str(path),
            "-map",
            "0:a:0",
            "-ac",
            "1",
            "-ar",
            "48000",
            "-f",
            "s16le",
            "-",
        ]
    )


def first_frame(path, start=0):
    return subprocess.check_output(
        [
            app.executable("ffmpeg"),
            "-v",
            "error",
            "-ss",
            str(start),
            "-i",
            str(path),
            "-frames:v",
            "1",
            "-pix_fmt",
            "rgb24",
            "-f",
            "rawvideo",
            "-",
        ]
    )


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
                "20",
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

    def export(self, source, fmt="mp4", start=3.25, end=5.25, legacy=None):
        asset = self.import_source(source)
        ids = self.workshop.trim([asset["id"]], fmt, start, end, legacy)
        result = self.assert_success(ids)[0]
        return self.workshop.jobs[ids[0]], self.workshop.asset_path(result), asset

    def test_legacy_fast_setting_does_not_move_cut_to_previous_keyframe(self):
        job, output, asset = self.export(self.indexed, legacy="fast")
        self.assertEqual(job["trim_start"], 3.25)
        self.assertNotIn("mode", job["spec"])
        self.assertEqual(job["method"], "Accurate cut")
        self.assertAlmostEqual(app.probe(output)["duration"], 2, delta=0.11)
        self.assertTrue(self.workshop.asset_path(asset).exists())

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Optional smart renderer unavailable"
    )
    def test_long_cut_copies_middle_frames_and_preserves_requested_timing(self):
        before = hashlib.sha256(self.indexed.read_bytes()).hexdigest()
        job, output, asset = self.export(self.indexed, start=3.25, end=17.25)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertEqual(job["trim_start"], 3.25)
        self.assertGreaterEqual(job["smart_copied_seconds"], 10)
        self.assertAlmostEqual(app.probe(output)["duration"], 14, delta=0.11)
        self.assertEqual(app.probe(output)["video_frames"], 140)
        copied = set(packet_hashes(self.indexed, "v:0")) & set(
            packet_hashes(output, "v:0")
        )
        self.assertGreater(len(copied), 80)
        expected, actual = first_frame(self.indexed, 3.25), first_frame(output)
        self.assertEqual(len(expected), len(actual))
        self.assertLess(
            sum(abs(a - b) for a, b in zip(expected, actual)) / len(actual), 3
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
            before,
        )

    def test_mp3_cut_keeps_exact_audio_sample_count(self):
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
        self.assertEqual(job["trim_start"], 1.2)
        self.assertEqual(len(pcm(output)) // 2, 72000)

    def test_aac_to_mp3_cut_is_accurate(self):
        job, output, _ = self.export(self.indexed, fmt="mp3", start=1.2, end=2.7)
        self.assertEqual(app.probe(output)["audio_codec"], "mp3")
        self.assertEqual(len(pcm(output)) // 2, 72000)

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_copied_aac_preserves_fractional_start_samples_and_compressed_packets(self):
        import numpy as np

        for rate in (44100, 48000):
            with self.subTest(sample_rate=rate):
                source = Path(self.temp.name) / f"noise-{rate}.mp4"
                subprocess.run(
                    [
                        app.executable("ffmpeg"),
                        "-v",
                        "error",
                        "-i",
                        str(self.indexed),
                        "-f",
                        "lavfi",
                        "-i",
                        f"anoisesrc=sample_rate={rate}:seed=491",
                        "-t",
                        "20",
                        "-map",
                        "0:v:0",
                        "-map",
                        "1:a:0",
                        "-c:v",
                        "copy",
                        "-c:a",
                        "aac",
                        "-b:a",
                        "192k",
                        str(source),
                    ],
                    check=True,
                )
                start, end = 3.25123, 17.25389
                job, output, _ = self.export(source, start=start, end=end)
                self.assertEqual(
                    job["processing_backend"], "smart", job.get("fallback_reason")
                )
                self.assertEqual(job["audio_processing"], "copy")

                def samples(path, times=()):
                    data = subprocess.check_output(
                        [
                            app.executable("ffmpeg"),
                            "-v",
                            "error",
                            *times,
                            "-i",
                            str(path),
                            "-map",
                            "0:a:0",
                            "-f",
                            "f32le",
                            "-",
                        ]
                    )
                    return np.frombuffer(data, dtype=np.float32)

                expected = samples(source, ("-ss", str(start)))
                actual = samples(output)
                count = round((end - start) * rate)
                self.assertGreaterEqual(len(actual), count)
                self.assertLess(len(actual) - count, 1024)  # Last compressed AAC frame.
                np.testing.assert_allclose(
                    actual[:count], expected[:count], atol=0.00005, rtol=0
                )
                copied = set(packet_hashes(source, "a:0")) & set(
                    packet_hashes(output, "a:0")
                )
                self.assertGreater(len(copied), count // 1024 - 2)

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_late_silent_cut_uses_bounded_index_and_keeps_source_frames(self):
        from video_index import index_video_window

        source = Path(self.temp.name) / "long-silent.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "testsrc2=size=80x48:rate=10",
                "-t",
                "180",
                "-c:v",
                "libx264",
                "-g",
                "20",
                "-sc_threshold",
                "0",
                "-threads",
                "2",
                str(source),
            ],
            check=True,
        )
        index = index_video_window(source, 160.25, 175.25, 2)
        try:
            self.assertLessEqual(len(index.video_frame_times), 200)
            self.assertGreaterEqual(float(index.video_frame_times[0]), 156)
            self.assertEqual(index.audio_tracks, [])
        finally:
            index.close()
        job, output, _ = self.export(source, start=160.25, end=175.25)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertEqual(job["audio_processing"], "none")
        self.assertLessEqual(job["indexed_video_frames"], 200)
        self.assertFalse(app.probe(output)["audio"])
        self.assertEqual(app.probe(output)["video_frames"], 150)
        expected, actual = first_frame(source, 160.25), first_frame(output)
        self.assertLess(
            sum(abs(a - b) for a, b in zip(expected, actual)) / len(actual), 3
        )

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_six_second_cut_copies_interior_video_instead_of_full_encoding(self):
        job, output, _ = self.export(self.indexed, start=3.25, end=9.25)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertGreaterEqual(job["smart_copied_seconds"], 4)
        self.assertEqual(app.probe(output)["video_frames"], 60)

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_cut_through_source_end_preserves_the_last_video_frame(self):
        job, output, _ = self.export(self.indexed, start=3.25, end=20)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertEqual(app.probe(output)["video_frames"], 167)
        self.assertAlmostEqual(app.probe(output)["duration"], 16.75, delta=0.11)
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

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_non_aac_audio_converts_while_video_is_copied(self):
        source = Path(self.temp.name) / "opus.mkv"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.indexed),
                "-c:v",
                "copy",
                "-c:a",
                "libopus",
                str(source),
            ],
            check=True,
        )
        job, output, _ = self.export(source, start=3.25, end=17.25)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertEqual(job["audio_processing"], "encode")
        self.assertEqual(app.probe(output)["audio_codec"], "aac")
        self.assertAlmostEqual(app.probe(output)["duration"], 14, delta=0.11)

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_delayed_audio_retains_its_offset_from_the_video(self):
        import av

        source = Path(self.temp.name) / "delayed-audio.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.indexed),
                "-itsoffset",
                "0.75",
                "-i",
                str(self.indexed),
                "-map",
                "0:v:0",
                "-map",
                "1:a:0",
                "-c",
                "copy",
                str(source),
            ],
            check=True,
        )
        job, output, _ = self.export(source, start=0.25, end=13.5)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        with av.open(str(source)) as original, av.open(str(output)) as cut:
            track = original.streams.audio[0]
            actual = cut.streams.audio[0]
            self.assertAlmostEqual(
                float(actual.start_time * actual.time_base),
                float(track.start_time * track.time_base) - 0.25,
                delta=0.002,
            )

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Smart backend unavailable"
    )
    def test_cancellation_stops_parallel_audio_and_removes_partial_output(self):
        import sys

        original = app.trim_plan
        script = """
import sys
from pathlib import Path
sys.path.insert(0, sys.argv[1])
import media_worker
real = media_worker.ffmpeg
def paced(command, callback=lambda t: None, **kwargs):
    if command[-1].endswith("audio.m4a"):
        command = list(command)
        command.insert(command.index("-i"), "-re")
    return real(command, callback, **kwargs)
media_worker.ffmpeg = paced
media_worker.smart_trim(Path(sys.argv[2]), Path(sys.argv[3]), 3.25, 17.25, 2)
"""

        def paced_plan(*args):
            _, backend = original(*args)
            return [
                sys.executable,
                "-c",
                script,
                str(app.ROOT),
                str(args[0]),
                str(args[1]),
            ], backend

        asset = self.import_source(self.indexed)
        with patch.object(app, "trim_plan", side_effect=paced_plan):
            ids = self.workshop.trim([asset["id"]], "mp4", 3.25, 17.25)
            until(lambda: self.workshop.jobs[ids[0]].get("indexed_video_frames"))
            self.workshop.cancel(ids[0])
            job = completed(self.workshop, ids)[0]
        self.assertEqual(job["state"], "cancelled")
        self.assertFalse(self.workshop.processes)
        self.assertFalse((self.workshop.directory / "jobs" / ids[0]).exists())

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Optional smart renderer unavailable"
    )
    def test_nonzero_source_timestamps_do_not_shift_smart_cut(self):
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
        job, output, _ = self.export(source, start=3.25, end=17.25)
        self.assertEqual(job["trim_start"], 3.25)
        self.assertEqual(job["processing_backend"], "smart", job.get("fallback_reason"))
        self.assertAlmostEqual(app.probe(output)["duration"], 14, delta=0.11)
        expected, actual = first_frame(source, 3.25), first_frame(output)
        self.assertLess(
            sum(abs(a - b) for a, b in zip(expected, actual)) / len(actual), 3
        )

    def test_smart_failure_automatically_preserves_accuracy(self):
        original = app.trim_plan

        def broken(*args):
            command, backend = original(*args)
            return [
                __import__("sys").executable,
                "-c",
                "raise SystemExit('renderer failed')",
            ], "smart"

        with patch.object(app, "trim_plan", side_effect=broken):
            job, output, _ = self.export(self.indexed)
        self.assertEqual(job["processing_backend"], "encoder")
        self.assertEqual(job["trim_start"], 3.25)
        self.assertIn("renderer failed", job["fallback_reason"])
        self.assertAlmostEqual(app.probe(output)["duration"], 2, delta=0.11)

    def test_batch_uses_same_accurate_workflow_for_all_sources(self):
        first = self.import_source(self.indexed)
        second = self.workshop.register(
            self.workshop.asset_path(first), "Second source"
        )
        ids = self.workshop.trim([first["id"], second["id"]], "mp4", 3.25, 5.25)
        outputs = self.assert_success(ids)
        self.assertEqual(len({a["path"] for a in outputs}), 2)
        self.assertTrue(all(self.workshop.jobs[i]["trim_start"] == 3.25 for i in ids))
        self.assertTrue(self.workshop.asset_path(first).exists())

    def test_whole_source_copy_never_moves_the_original(self):
        asset = self.import_source(self.indexed)
        ids = self.workshop.trim([asset["id"]], "mp4", 0, "")
        self.assert_success(ids)
        self.assertTrue(self.workshop.asset_path(asset).exists())

    @unittest.skipUnless(
        importlib.util.find_spec("smartcut"), "Optional smart renderer unavailable"
    )
    def test_cutting_a_previous_smart_result_keeps_timing_and_decodes_cleanly(self):
        _, first, _ = self.export(self.indexed, start=3.25, end=17.25)
        source = self.workshop.register(first, "Previous cut")
        ids = self.workshop.trim([source["id"]], "mp4", 0.25, 13)
        result = self.assert_success(ids)[0]
        output = self.workshop.asset_path(result)
        self.assertAlmostEqual(result["duration"], 12.75, delta=0.11)
        expected, actual = first_frame(first, 0.25), first_frame(output)
        self.assertLess(
            sum(abs(a - b) for a, b in zip(expected, actual)) / len(actual), 3
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

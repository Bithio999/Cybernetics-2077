"""Bounded H.264 index adapter for the pinned smartcut 1.7 interface.

Read video packet timestamps around the selected range directly from the source.
Do not remux a temporary video, scan the prefix, decode frames, or retain audio.
"""

from fractions import Fraction


def index_video_window(source, start, end, threads):
    import av
    import numpy as np
    from smartcut.media_container import MediaContainer
    from smartcut.nal_tools import get_h264_nal_unit_type, is_safe_h264_keyframe_nal

    # Retain the backend's time-search methods, while providing a bounded index.
    index = MediaContainer.__new__(MediaContainer)
    index.path = str(source)
    index.av_container = container = av.open(str(source), metadata_errors="ignore")
    try:
        index.start_time = Fraction(container.start_time or 0, av.time_base)
        if container.duration is None:
            raise ValueError("The source has no indexed duration")
        index.duration = Fraction(container.duration, av.time_base)
        index.video_stream = stream = container.streams.video[0]
        if stream.codec_context.name != "h264":
            raise ValueError("Bounded indexing requires H.264 video")
        stream.thread_type = "FRAME"
        stream.codec_context.thread_count = threads
        index.audio_tracks, index.subtitle_tracks = [], []
        index.chat_url = index.chat_history = None
        index.chat_visualize = False
        index.video_keyframe_indices = []
        index.gop_start_times_dts, index.gop_end_times_dts = [], []
        index.gop_start_nal_types = []
        index.gop_leading_end_dts, index.gop_has_rasl = [], []
        frame_pts, last_dts = [], None
        end_absolute = Fraction(str(end)) + index.start_time
        # Prime the decoder from a preceding GOP, seeking through the source index.
        seek_time = max(Fraction(0), Fraction(str(start)) - 2) + index.start_time
        container.seek(int(seek_time / stream.time_base), stream=stream)
        for packet in container.demux(stream):
            if packet.pts is None or packet.dts is None:
                continue
            if packet.is_keyframe:
                nal_type = get_h264_nal_unit_type(bytes(packet))
                if is_safe_h264_keyframe_nal(nal_type):
                    # Finish the selected range's entire last GOP for B-frame refs.
                    if frame_pts and packet.pts * stream.time_base > end_absolute:
                        break
                    if last_dts is not None:
                        index.gop_end_times_dts.append(last_dts)
                    index.video_keyframe_indices.append(len(frame_pts))
                    index.gop_start_times_dts.append(packet.dts)
                    index.gop_start_nal_types.append(nal_type)
                    index.gop_leading_end_dts.append(None)
                    index.gop_has_rasl.append(False)
            if not index.gop_start_times_dts:
                continue  # Never begin with dependent/unsafe frames.
            frame_pts.append(packet.pts)
            last_dts = packet.dts
        if not frame_pts:
            raise ValueError("No independently decodable video in the selected range")
        index.gop_end_times_dts.append(last_dts)
        index.video_frame_times_pts = np.sort(np.array(frame_pts, dtype=np.int64))
        index.video_frame_times = index.video_frame_times_pts * stream.time_base
        index.gop_start_times_pts_s = list(
            index.video_frame_times[index.video_keyframe_indices]
        )
        if index.video_frame_times[0] > Fraction(str(start)) + index.start_time:
            raise ValueError("The video index could not reach the requested start")
        return index
    except Exception:
        container.close()
        raise

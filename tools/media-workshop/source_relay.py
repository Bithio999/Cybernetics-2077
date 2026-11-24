"""Feed a pipe promptly, retaining a seekable fallback only until decoding starts."""

import threading


class SourceRelay:
    def __init__(self, source, sink, cache_path):
        self.source, self.sink, self.cache_path = source, sink, cache_path
        self.release_cache = threading.Event()
        self.complete = False
        self.error = None
        self.pipe_error = False
        self.thread = threading.Thread(target=self.run, daemon=True)

    def run(self):
        cache = None
        feeding = True
        try:
            cache = self.cache_path.open("wb", buffering=256 * 1024)
            while chunk := self.source.read1(256 * 1024):
                if cache is not None and self.release_cache.is_set():
                    cache.close()
                    cache = None
                    self.cache_path.unlink(missing_ok=True)
                if cache is not None:
                    cache.write(chunk)
                if feeding:
                    try:
                        self.sink.write(chunk)
                        self.sink.flush()
                    except (OSError, ValueError):
                        # A container needing backward seeks may close the encoder
                        # pipe. Finish this one download into the seekable cache.
                        feeding = False
                        self.pipe_error = True
                        try:
                            self.sink.close()
                        except (OSError, ValueError):
                            pass
            self.complete = True
        except (OSError, ValueError) as error:
            self.error = error
        finally:
            if cache is not None:
                try:
                    cache.close()
                except OSError as error:
                    self.error = error
            try:
                self.sink.close()
            except (OSError, ValueError):
                self.pipe_error = True
            self.source.close()

    def start(self):
        self.thread.start()

    def join(self, timeout=None):
        self.thread.join(timeout)

    @property
    def reusable(self):
        return self.complete and self.error is None and self.cache_path.is_file()

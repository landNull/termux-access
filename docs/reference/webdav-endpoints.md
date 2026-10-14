# Reference: WebDAV endpoints

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| OPTIONS | `/*` | Implemented | Returns DAV: 1,2 and Allow header |
| GET | `/*` | Implemented | Streaming, Range/206 support |
| PUT | `/*` | Implemented | Streaming write |
| DELETE | `/*` | Implemented | |
| PROPFIND | `/*` | Basic | Depth 0/1, live properties |
| MKCOL | `/*` | Planned | |
| COPY | `/*` | Planned | |
| MOVE | `/*` | Planned | |

Path traversal outside `$HOME/Apps` is rejected on every method.

Server starts only on explicit user action and is off by default.

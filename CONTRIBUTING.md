# Contributing to Termux:Access

This project follows the **DLitFunc** approach: Diátaxis documentation + literal + functional synergistic programming and commenting style.

## Organization steps

1. **Documentation** — place every new doc in the correct Diátaxis folder under `docs/`:
   - `docs/tutorials/` — learning-oriented lessons
   - `docs/how-to/` — goal-oriented guides
   - `docs/reference/` — factual lookup
   - `docs/explanation/` — understanding-oriented discussion
2. **Code style** — prefer literal names and pure functions. Isolate side effects. Comment only the *why* or a non-obvious contract. See [docs/dlitfunc.md](docs/dlitfunc.md).
3. **Automation** — run the tools before you push:
   ```bash
   ./scripts/install-dlitfunc-tools.sh   # once
   vale docs                              # Diátaxis lint
   ktlint --format                        # Kotlin style
   ```
4. **Testing** — run `scripts/test-webdav.sh` and attach the generated report when changing WebDAV behavior.
5. **Push** — open a pull request against `main`. Describe the Diátaxis pages you updated and any remaining gaps.

## Quick start for contributors

```bash
git clone https://github.com/landNull/termux-access.git
cd termux-access
# Follow docs/tutorials/getting-started.md
# Then docs/how-to/run-webdav-tests.md
```

All new work should reinforce the DLitFunc style. The tools above automate the checks.

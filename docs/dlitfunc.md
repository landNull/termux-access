---
title: DLitFunc
tags: [dlitfunc, documentation, style]
aliases: [DLitFunc style guide]
---

# DLitFunc

**DLitFunc** = **Diátaxis** + **Lit**eral + **Func**tional synergistic programming and code commenting style.

> [!note] Portability
> This documentation uses standard CommonMark + GFM so it renders correctly in Obsidian, GitHub, VS Code, and other Markdown tools. Wiki-links and Obsidian-only callouts are avoided in the committed files.

## Principles

1. **Literal** — names, comments, and docs say exactly what the code does. No metaphors that hide behavior.
2. **Functional** — prefer pure functions, explicit data flow, and small composable units. Side effects are isolated and named.
3. **Synergistic** — comments and documentation reinforce the code rather than repeating it. A comment explains *why* or a non-obvious contract; the code itself is the *what*.
4. **Diátaxis** — all documentation is sorted into one of the four kinds (tutorial, how-to, reference, explanation).

## Code commenting style

- Function-level: one-line summary of the contract (inputs → outputs / effects).
- Complex blocks: short literal note on the invariant being maintained.
- Avoid restating the next line of code.
- Prefer names that make comments unnecessary.

Example:
```kotlin
// Rejects any path that would escape the Apps root.
fun isSafePath(root: File, candidate: File): Boolean =
    candidate.canonicalPath.startsWith(root.canonicalPath + File.separator)
```

## Automation tools

| Tool | Purpose | Command |
|------|---------|---------|
| **Vale** + Diátaxis package | Lint documentation into the four kinds | `vale docs` |
| **ktlint** | Format and enforce Kotlin style (literal naming) | `ktlint --format` |
| **detekt** | Structural quality, complexity, side-effect hints | `./gradlew detekt` |

Install them with:
```bash
./scripts/install-dlitfunc-tools.sh
```

GitHub Actions run Vale on every PR.

## Organization steps

1. Place new documentation in the correct Diátaxis folder.
2. Write code so the function name and signature are literal.
3. Add a comment only when the *why* or a hidden contract is not obvious.
4. Keep side effects at the edges (network, filesystem, Android APIs).
5. Update the relevant reference or explanation page when behavior changes.
6. Run the automation tools before pushing.

This style is the default for all new code and documentation in the termux-access project.

See also the [Diátaxis map](README.md).

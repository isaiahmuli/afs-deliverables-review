# MCP: Context7 (public library documentation)

## What is connected

| | |
|---|---|
| Server | **Context7**: up-to-date documentation and code examples for libraries and frameworks |
| Transport | Streamable HTTP, `https://mcp.context7.com/mcp` |
| Credentials | None. The public endpoint works without a key (rate-limited). **Never commit a key.** If you add one, put it in an environment variable, not in these files. |
| Tools | `resolve-library-id` (find a library's ID), `query-docs` (fetch docs for that ID) |

| Agent | Config file (committed) | One-time setup per developer |
|---|---|---|
| Claude Code | [`.mcp.json`](../.mcp.json) (project scope) | Start `claude` in the repo and approve the `context7` server when prompted. Check with `claude mcp get context7`; it should say `Scope: Project config`. |
| OpenAI Codex | [`.codex/config.toml`](../.codex/config.toml) | Codex reads project config only for **trusted** projects. Trust the repo when Codex asks, or add the same `[mcp_servers.context7]` block to `~/.codex/config.toml`. Check with `codex mcp list`; it should list `context7`. |

## When to consult it instead of memory

Agents' built-in knowledge of libraries is frozen at training time and often out of date. **Consult
Context7 before writing or changing code that depends on library API details**, specifically:

1. **Any OpenPDF code.** OpenPDF 3 moved its packages from `com.lowagie.text` to
   `org.openpdf.text`. Code written from memory will use the old imports and fail to compile.
   (Found through Context7 while writing this spec; see the evidence below.)
2. **Spring Boot 4 / Spring Framework 7 APIs.** Boot 4 split starters into modules
   (`spring-boot-starter-webmvc`, `*-test` starters) and moved to Jackson 3. Check before
   adding a dependency, test annotation, or `ProblemDetail` customisation.
3. **PDFBox 3 text extraction in tests.** The loading API changed between 2.x and 3.x
   (`Loader.loadPDF`).
4. **When a compile error or deprecation warning involves a library API.** Look it up before
   guessing a fix.
5. **Before naming or upgrading a dependency version** in `docs/SPEC.md` or `build.gradle.kts`.

**Do not consult it** for this project's own rules (read `docs/SPEC.md` and
`docs/CONVENTIONS.md`), for general Java syntax, or for business decisions. Open questions go to
a human, not to an MCP server.

**How to use it:** call `resolve-library-id` with the library name, then `query-docs` with that
ID and a specific question. In the task's review note, record the library ID and what you
checked. Docs from MCP are **input, not authority**: the acceptance checks in `docs/SPEC.md`
still decide whether code is correct.

## Evidence (captured 2026-11-16)

Connection to the public endpoint (MCP `initialize`):

```
serverInfo: {"name":"Context7","title":"Context7","version":"4.7.0", ...}
protocolVersion: 2025-06-18
```

`tools/list`:

```
- resolve-library-id | required: ['query', 'libraryName']
- query-docs         | required: ['libraryId', 'query']
```

`resolve-library-id` with `libraryName: "openpdf"`:

```
- Title: OpenPDF
- Context7-compatible library ID: /librepdf/openpdf
```

`query-docs` with `/librepdf/openpdf` ("OpenPDF 3.0 package names ..."), import lines returned:

```
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
```

Agent registration:

```
$ claude mcp get context7
context7:
  Scope: Project config (shared via .mcp.json)
  Status: ⏸ Pending approval (run `claude` to approve)
  Type: http
  URL: https://mcp.context7.com/mcp

$ codex mcp list -c 'mcp_servers.context7.url="https://mcp.context7.com/mcp"'
Name      Url
context7  https://mcp.context7.com/mcp
```

(The Codex check passes the same value as `.codex/config.toml` on the command line, because this
machine has not yet marked the repo as trusted.)

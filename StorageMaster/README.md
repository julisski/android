# Storage Master Lab

**[`storage-tutorial.html`](./storage-tutorial.html)** — a single-file, offline, interactive
master class on **Android local data storage**. It's a standalone, deep-dive companion to the
[`RoomAndPreferences`](../RoomAndPreferences) demo app (Room for the notes table + DataStore
Preferences for settings).

Open the HTML file in any browser — no build step, no network needed.

## What's inside

21 sections across six tracks — **Foundations**, **Key–value (DataStore)**, **Room**,
**Files & media**, **Security & lifecycle**, and **Architecture & wiring** — each with deep,
current (mid-2026) explanations, key points, common mistakes, and real Kotlin/SQL/XML.

Every major concept has a **hand-built interactive playground**:

| Playground | What you can do |
|---|---|
| **Live reactive database** | Insert / check / delete notes and watch a `Flow<List<Note>>` re-emit, the reactive pipeline pulse, and the UI rebuild — then force-stop & relaunch to prove the rows persisted. |
| **@Entity → CREATE TABLE** | Flip annotations and watch the generated Kotlin entity and the SQLite schema change together. |
| **DAO query builder** | Pick an operation and see the generated `@Dao` method, the SQL Room runs, and a sample result. |
| **Which storage?** decision tree | Answer a few questions and get a concrete recommendation with the reasoning. |
| **SharedPreferences vs DataStore** | Change a setting and compare a synchronous, callback store with a reactive `Flow` one (including the main-thread ANR). |
| **Device storage map** | Trigger clear-cache / clear-data / uninstall / restore and see exactly which buckets survive. |
| **Migration playground** | Add a column, bump the version, and watch a strategy preserve, crash, or wipe your data. |
| **Threading visualizer** | Run a query on the main thread vs `suspend` and watch the UI thread and frame rate. |
| **Type converters** | See how a `@TypeConverter` maps a `List`, `Instant`, enum or `Color` to a stored column. |
| **Backup rules generator** | Toggle buckets and read the generated `data_extraction_rules.xml`. |

## Editing

The HTML is generated from source by a small build step:

```bash
cd storage-tutorial
node build.mjs        # styles.css + engine.js + sections.json -> ../storage-tutorial.html
```

- `sections.json` — all section content (prose, key points, gotchas, canonical code, playground config)
- `engine.js` — the rendering engine + every interactive playground widget
- `styles.css` — the Material-3-flavored design system (shared tokens with the Compose Lab)

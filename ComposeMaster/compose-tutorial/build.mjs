#!/usr/bin/env node
// Assemble the self-contained Compose Layout Lab from styles.css + engine.js + sections.json.
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const css = readFileSync(join(here, "styles.css"), "utf8");
const engine = readFileSync(join(here, "engine.js"), "utf8");
const sections = JSON.parse(readFileSync(join(here, "sections.json"), "utf8"));
const out = join(here, "..", "compose-tutorial.html");

const html = `<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Jetpack Compose Layout Lab — every concept, interactive</title>
<meta name="description" content="The most detailed interactive Jetpack Compose tutorial: modifiers, padding, alignment, spacing, arrangement, weight, Box/Row/Column, and more — each with a live playground and generated Kotlin.">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
<style>${css}</style>
</head>
<body>
<button id="theme" class="theme-btn" aria-label="Toggle theme">🌙</button>
<div class="app">
  <aside class="sidebar">
    <div class="brand"><div class="logo">JC</div><div><b>Compose Layout Lab</b><span>Every concept, interactive</span></div></div>
    <div id="nav"></div>
  </aside>
  <main class="main">
    <header class="hero">
      <span class="kick">Jetpack Compose · Material 3 · Interactive</span>
      <h1>The Compose <em>Layout Lab</em></h1>
      <p>Every layout concept — modifiers and their order, padding, sizing, alignment, arrangement, spacing, weight, Box/Row/Column, scrolling, and text — explained in depth and made tweakable. Drag the controls and watch both the live preview and the generated Kotlin update in real time.</p>
      <p class="meta"><b>${sections.length} sections</b> · built for Android devs who want to truly understand Compose, not just copy-paste. The previews emulate Compose layout in the browser; the Kotlin is the real thing.</p>
    </header>
    <div id="sections"></div>
  </main>
</div>
<script>
const SECTIONS = ${JSON.stringify(sections)};
${engine}
</script>
</body>
</html>
`;

writeFileSync(out, html);
console.log("wrote", out, "(" + html.length + " bytes,", sections.length, "sections)");

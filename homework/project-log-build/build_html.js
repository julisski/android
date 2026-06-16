// build_html.js — renders spec.json into a styled, printable HTML (then Chrome -> PDF).
const fs = require('fs');
const spec = JSON.parse(fs.readFileSync(__dirname + '/spec.json', 'utf8'));
const esc = s => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function metaBar(fields) {
  return `<div class="meta">${fields.map(f => `
    <div class="metacell"><div class="mlabel">${esc(f.label)}</div><div class="mline">${esc(f.hint || '')}</div></div>`).join('')}</div>`;
}
function structTable(s) {
  const cols = s.columns && s.columns.length ? s.columns : ['Item', 'Notes'];
  const head = `<tr>${cols.map(c => `<th>${esc(c)}</th>`).join('')}</tr>`;
  const ex = (s.sampleRows || []).map(r => `<tr class="ex">${cols.map((_, i) => `<td>${i === 0 ? '<b>e.g.</b> ' : ''}${esc(r[i] || '')}</td>`).join('')}</tr>`).join('');
  const blanks = Array.from({ length: 3 }, () => `<tr>${cols.map(() => '<td>&nbsp;</td>').join('')}</tr>`).join('');
  return `<table class="grid">${head}${ex}${blanks}</table>`;
}
function structChecklist(items) {
  return `<div class="checklist">${(items || []).map(it => `<div class="chk"><span class="box">☐</span> ${esc(it)}</div>`).join('')}</div>`;
}
function structScale(s) {
  const rows = s.scaleRows && s.scaleRows.length ? s.scaleRows : ['Overall'];
  const head = `<tr><th class="scalelabel">Area &nbsp; <span class="legend">(1 = lost · 5 = confident)</span></th>${[1,2,3,4,5].map(n => `<th class="num">${n}</th>`).join('')}</tr>`;
  const body = rows.map(r => `<tr><td class="scalelabel">${esc(r)}</td>${[1,2,3,4,5].map(() => '<td class="num"><span class="box">☐</span></td>').join('')}</tr>`).join('');
  return `<table class="grid scale">${head}${body}</table>`;
}
const structSketch = () => `<div class="sketch">✎ &nbsp; Sketch your screen here — or paste a screenshot / @Preview render</div>`;
const structFreeform = () => `<div class="freeform">&nbsp;</div>`;

function section(s, i) {
  let body = '';
  const t = (s.structureType || 'freeform').toLowerCase();
  if (t === 'table') body = structTable(s);
  else if (t === 'checklist') body = structChecklist(s.checklistItems);
  else if (t === 'scale') body = structScale(s);
  else if (t === 'sketchbox') body = structSketch();
  else body = structFreeform();
  // optional secondary checklist (UI-state branches, components, triage flag)
  let extra = '';
  if (t !== 'checklist' && s.checklistItems && s.checklistItems.length) {
    extra = `${s.checklistLabel ? `<div class="chklabel">${esc(s.checklistLabel)}</div>` : ''}${structChecklist(s.checklistItems)}`;
  }
  const prompts = (s.prompts || []).map(p => `<li>${esc(p)}</li>`).join('');
  const example = s.example ? `<div class="example"><b>Example</b> ${esc(s.example)}</div>` : '';
  return `<section class="qsec">
    <h2><span class="emoji">${esc(s.emoji || '')}</span> ${esc(s.title)}</h2>
    <p class="purpose">${esc(s.purpose || '')}</p>
    ${prompts ? `<ul class="prompts">${prompts}</ul>` : ''}
    ${example}
    ${body}
    ${extra}
  </section>`;
}

const html = `<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
<title>${esc(spec.templateTitle)}</title>
<style>
:root{--ink:#1f2933;--muted:#5b6b7b;--line:#d9dee7;--green:#3ddc84;--deep:#0f9d58;--dark:#0f172a;
--light:#f2f5f8;--tip:#ecfdf3;--tipline:#c5ebd3;--blue:#2563eb;--bluesoft:#eff4ff;--accentink:#063a22;}
*{box-sizing:border-box;}
html{-webkit-print-color-adjust:exact;print-color-adjust:exact;}
body{margin:0;color:var(--ink);font:14px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;}
.wrap{max-width:820px;margin:0 auto;padding:26px 30px 56px;}
.banner{background:linear-gradient(135deg,#0b1220 0%,#15243a 60%,#0f9d58 175%);color:#fff;
  border-bottom:5px solid var(--green);border-radius:12px;padding:20px 24px;}
.banner .eyebrow{letter-spacing:.16em;text-transform:uppercase;font-size:11px;font-weight:800;color:var(--green);margin:0 0 5px;}
.banner h1{margin:0 0 6px;font-size:27px;font-weight:850;letter-spacing:-.01em;}
.banner .tag{margin:0;color:#cbd5e1;font-size:14px;font-style:italic;}
.meta{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:14px 0;}
.metacell{border:1px solid var(--line);background:var(--light);border-radius:8px;padding:8px 11px;}
.mlabel{font-size:10.5px;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:var(--deep);}
.mline{font-size:12px;color:var(--muted);font-style:italic;margin-top:3px;border-bottom:1px solid #cfd6e0;padding-bottom:7px;}
.howto{background:var(--tip);border:1px solid var(--tipline);border-radius:10px;padding:13px 17px;margin:0 0 18px;}
.howto h3{margin:0 0 7px;font-size:14.5px;color:var(--accentink);}
.howto ul{margin:0;padding-left:18px;}.howto li{margin:4px 0;font-size:13px;}
.qsec{margin:0 0 20px;break-inside:avoid;page-break-inside:avoid;}
.qsec h2{font-size:17px;margin:18px 0 4px;padding-bottom:5px;border-bottom:2px solid var(--green);font-weight:800;color:#1f2933;}
.qsec h2 .emoji{margin-right:4px;}
.purpose{margin:0 0 8px;color:var(--muted);font-style:italic;font-size:13px;}
ul.prompts{margin:0 0 8px;padding-left:20px;}ul.prompts li{margin:4px 0;font-size:13.5px;}
.example{background:var(--bluesoft);border:1px solid #d3e0ff;border-radius:8px;padding:8px 12px;margin:8px 0;font-size:12.5px;font-style:italic;color:#2c3a4e;}
.example b{color:var(--blue);font-style:normal;font-size:11px;letter-spacing:.04em;text-transform:uppercase;margin-right:5px;}
table.grid{width:100%;border-collapse:collapse;margin:6px 0 2px;font-size:12.5px;}
table.grid th{background:var(--deep);color:#fff;font-weight:700;text-align:left;padding:6px 9px;border:1px solid var(--line);font-size:12px;}
table.grid td{border:1px solid var(--line);padding:8px 9px;vertical-align:top;height:26px;}
table.grid tr.ex td{background:var(--bluesoft);color:var(--muted);font-style:italic;}
table.grid tr.ex td b{font-style:normal;color:var(--blue);}
table.scale th.num,table.scale td.num{text-align:center;width:46px;}
table.scale .scalelabel{width:auto;}
.legend{font-weight:500;opacity:.85;font-size:10.5px;}
.box{font-size:15px;color:var(--muted);}
.chklabel{font-weight:700;color:#1f2933;font-size:12.5px;margin:8px 0 4px;}
.checklist{columns:2;column-gap:24px;margin:4px 0;}
.chk{break-inside:avoid;margin:4px 0;font-size:13px;}.chk .box{color:var(--deep);margin-right:4px;}
.sketch{border:2px dashed var(--green);background:#fbfefc;border-radius:10px;min-height:150px;
  display:flex;align-items:center;justify-content:center;color:var(--muted);font-style:italic;font-size:13px;text-align:center;padding:14px;}
.freeform{border:1px solid var(--line);border-radius:8px;min-height:78px;}
.footer{background:var(--tip);border:1px solid var(--tipline);border-radius:10px;padding:12px 16px;margin-top:20px;
  text-align:center;font-weight:700;color:var(--accentink);font-size:13.5px;}
@page{margin:12mm 14mm;}
@media print{.wrap{padding:0;max-width:none;}}
</style></head><body><div class="wrap">
<div class="banner"><p class="eyebrow">Android · Kotlin · Jetpack Compose</p>
<h1>${esc(spec.templateTitle)}</h1><p class="tag">${esc(spec.tagline)}</p></div>
${metaBar(spec.metadataFields)}
<div class="howto"><h3>▸ How to use this each class</h3><ul>${spec.howToUse.map(t => `<li>${esc(t)}</li>`).join('')}</ul></div>
${spec.sections.map(section).join('\n')}
<div class="footer">${esc(spec.footer)}</div>
</div></body></html>`;

const out = __dirname + '/../project-design-log-TEMPLATE.html';
fs.writeFileSync(out, html);
console.log('wrote', out, html.length, 'bytes');

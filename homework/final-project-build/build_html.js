// build_html.js — render blocks.json into a styled HTML that mirrors the proposal, for PDF.
const fs = require('fs');
const data = JSON.parse(fs.readFileSync(__dirname + '/blocks.json', 'utf8'));
const esc = s => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function classCard(cls, goals) {
  return `<table class="card">
    <tr class="ch"><td class="lab">${esc(cls)}</td><td class="val"><b>Date:</b> <i class="ph">[Date]</i></td></tr>
    <tr><td class="lab">Goals:</td><td class="val"><ul class="tight">${goals.map(g => `<li>${esc(g)}</li>`).join('')}</ul></td></tr>
    <tr><td class="lab">Status:</td><td class="val"><i class="ph">[Not Started / In Progress / Complete]</i></td></tr>
    <tr><td class="lab">Notes:</td><td class="val"><i class="ph">[Add progress notes, blockers, or questions here]</i></td></tr>
  </table>`;
}
function render(b) {
  switch (b.t) {
    case 'h1': return `<h1>${esc(b.text)}</h1>`;
    case 'h2': return `<h2>${esc(b.text)}</h2>`;
    case 'h3b': return `<p class="h3b">${esc(b.text)}</p>`;
    case 'phase': return `<h3 class="phase ${esc(b.color)}">${esc(b.text)}</h3>`;
    case 'para': return `<p class="${b.ph ? 'ph' : ''}">${esc(b.text)}</p>`;
    case 'field': return `<p><b>${esc(b.label)}</b> <i class="ph">${esc(b.value)}</i></p>`;
    case 'kvTable': return `<table class="kv">${b.rows.map(([l, v, ph]) => `<tr><td class="lab">${esc(l)}</td><td class="${ph ? 'ph' : ''}">${esc(v)}</td></tr>`).join('')}</table>`;
    case 'numbered': return `<ol>${b.items.map(i => `<li>${esc(i) || '&nbsp;'}</li>`).join('')}</ol>`;
    case 'bullets': return `<ul>${b.items.map(i => `<li>${esc(i)}</li>`).join('')}</ul>`;
    case 'checklist': return `<div class="chk">${b.items.map(i => `<div><span class="box">☐</span> ${esc(i)}</div>`).join('')}</div>`;
    case 'classCard': return classCard(b.cls, b.goals);
    case 'milestone': return `<h3 class="phase ${esc(b.color)}">${esc(b.name)}</h3>`
      + (b.focus ? `<p><b>Focus:</b> ${esc(b.focus)}</p>` : '')
      + `<div class="chk">${(b.items || []).map(i => `<div><span class="box">☐</span> ${esc(i)}</div>`).join('')}</div>`
      + `<table class="kv"><tr><td class="lab">Status &amp; notes:</td><td class="ph">${esc(b.statusHint || '[done / in progress / not started — and anything blocking it]')}</td></tr></table>`;
    case 'table': {
      const hd = `<tr>${b.headers.map(h => `<th>${esc(h)}</th>`).join('')}</tr>`;
      const ex = (b.rows || []).map(r => `<tr>${b.headers.map((_, i) => `<td class="ph">${esc(r[i] || '')}</td>`).join('')}</tr>`).join('');
      const bl = Array.from({ length: b.blanks == null ? 3 : b.blanks }, () => `<tr>${b.headers.map(() => '<td>&nbsp;</td>').join('')}</tr>`).join('');
      return `<table class="fill">${hd}${ex}${bl}</table>${b.note ? `<p class="ph small">${esc(b.note)}</p>` : ''}`;
    }
    case 'fillBox': return `${b.label ? `<p class="h3b">${esc(b.label)}</p>` : ''}<div class="fillbox" style="min-height:${(b.lines || 2) * 22 + 18}px"></div>`;
    case 'note': return `<p class="note">${esc(b.text)}</p>`;
    case 'space': return `<div style="height:${b.h || 10}px"></div>`;
    default: return '';
  }
}

const html = `<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>${esc(data.title)}</title>
<style>
:root{--blue:#2E5496;--sub:#595959;--gray:#7f7f7f;--ink:#1a1a1a;--green:#538135;--orange:#C55A11;--purple:#7030A0;--shade:#f2f2f2;--border:#bfbfbf;}
*{box-sizing:border-box;}
html{-webkit-print-color-adjust:exact;print-color-adjust:exact;}
body{margin:0;color:var(--ink);font:11pt/1.45 Arial,Helvetica,sans-serif;}
.wrap{max-width:760px;margin:0 auto;padding:24px 28px 48px;}
.title{text-align:center;color:var(--blue);font-size:24pt;font-weight:bold;margin:6px 0 2px;}
.subtitle{text-align:center;color:var(--sub);font-size:13pt;margin:0 0 16px;}
table{border-collapse:collapse;width:100%;margin:4px 0 10px;}
table.meta td,table.kv td,table.card td{border:1px solid var(--border);padding:5px 9px;vertical-align:top;font-size:10.5pt;}
table.meta .lab,table.kv .lab,table.card .lab{font-weight:bold;width:26%;white-space:nowrap;}
table.kv .lab{width:30%;}
table.card .lab{width:22%;vertical-align:middle;}
table.card tr.ch td{background:var(--shade);font-weight:normal;vertical-align:middle;}
table.card tr.ch .lab{font-weight:bold;}
.card{page-break-inside:avoid;break-inside:avoid;margin-bottom:12px;}
table.fill{border-collapse:collapse;width:100%;margin:5px 0 10px;}
table.fill th{border:1px solid var(--border);background:var(--shade);padding:5px 9px;text-align:left;font-size:10.5pt;}
table.fill td{border:1px solid var(--border);padding:8px 9px;height:26px;font-size:10.5pt;vertical-align:top;}
.fillbox{border:1px solid var(--border);border-radius:2px;margin:2px 0 11px;}
.small{font-size:9.5pt;}
ul.tight{margin:0;padding-left:18px;}ul.tight li{margin:2px 0;}
h1{color:var(--blue);font-size:15pt;margin:22px 0 7px;}
h2{font-size:12pt;margin:15px 0 5px;}
.h3b{font-weight:bold;margin:10px 0 3px;}
.phase{font-size:12pt;margin:20px 0 5px;}
.phase.blue{color:var(--blue);}.phase.green{color:var(--green);}.phase.orange{color:var(--orange);}.phase.purple{color:var(--purple);}
.ph{color:var(--gray);font-style:italic;}
p{margin:7px 0;}
ol,ul{margin:6px 0 10px;padding-left:24px;}li{margin:3px 0;}
.chk div{margin:5px 0;}.chk .box{font-size:13pt;margin-right:6px;}
.note{text-align:center;color:var(--sub);font-style:italic;margin-top:18px;}
@page{margin:14mm 16mm;}
@media print{.wrap{padding:0;max-width:none;}}
</style></head><body><div class="wrap">
<div class="title">${esc(data.title)}</div>
<div class="subtitle">${esc(data.subtitle)}</div>
<table class="meta">${data.meta.map(([l, v]) => `<tr><td class="lab">${esc(l)}</td><td class="ph">${esc(v)}</td></tr>`).join('')}</table>
${data.blocks.map(render).join('\n')}
</div></body></html>`;

const out = __dirname + '/../../../Downloads/android_final_project.html';
fs.writeFileSync(out, html);
console.log('wrote', out, html.length, 'bytes');

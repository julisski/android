// build_docx.js — renders spec.json into a polished, fillable .docx template.
// Run: node build_docx.js  (reads ./spec.json, writes ../project-design-log-TEMPLATE.docx)
const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat, HeadingLevel, BorderStyle,
  WidthType, ShadingType, VerticalAlign, PageNumber,
} = require('docx');

const spec = JSON.parse(fs.readFileSync(__dirname + '/spec.json', 'utf8'));

// ---- palette (course identity: Android green + slate) ----
const C = {
  green: '3DDC84', deep: '0F9D58', dark: '0F172A', slate: '1F2933',
  muted: '5B6B7B', line: 'D9DEE7', light: 'F2F5F8', white: 'FFFFFF',
  tip: 'ECFDF3', tipLine: 'C5EBD3', blue: '2563EB', blueSoft: 'EFF4FF',
  accentInk: '063A22',
};
const USABLE = 10080; // Letter usable width with 0.75" margins (DXA): 12240 - 2*1080
const bd = (color = C.line, size = 4) => ({ style: BorderStyle.SINGLE, size, color });
const cellBorders = (color) => ({ top: bd(color), bottom: bd(color), left: bd(color), right: bd(color) });
const dashed = (color = C.green) => { const b = { style: BorderStyle.DASHED, size: 6, color }; return { top: b, bottom: b, left: b, right: b }; };

const run = (text, o = {}) => new TextRun({ text, font: 'Arial', ...o });
const P = (children, o = {}) => new Paragraph({ children: Array.isArray(children) ? children : [children], ...o });
const blank = (n = 1) => Array.from({ length: n }, () => P([run('')], { spacing: { after: 0 } }));

// ---- banner (full-width shaded single cell) ----
function banner() {
  return new Table({
    columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA },
    borders: { top: bd(C.dark), bottom: bd(C.green, 18), left: bd(C.dark), right: bd(C.dark),
               insideHorizontal: bd(C.dark), insideVertical: bd(C.dark) },
    rows: [new TableRow({ children: [new TableCell({
      width: { size: USABLE, type: WidthType.DXA },
      shading: { fill: C.dark, type: ShadingType.CLEAR },
      margins: { top: 200, bottom: 220, left: 240, right: 240 },
      children: [
        P([run('ANDROID · KOTLIN · JETPACK COMPOSE', { color: C.green, bold: true, size: 16, characterSpacing: 30 })], { spacing: { after: 40 } }),
        P([run(spec.templateTitle, { color: C.white, bold: true, size: 34 })], { spacing: { after: 30 } }),
        P([run(spec.tagline, { color: 'CBD5E1', italics: true, size: 21 })]),
      ],
    })] })],
  });
}

// ---- metadata bar (fields the student fills) ----
function metaBar(fields) {
  const perRow = 3;
  const colW = Math.floor(USABLE / perRow);
  const rows = [];
  for (let i = 0; i < fields.length; i += perRow) {
    const slice = fields.slice(i, i + perRow);
    while (slice.length < perRow) slice.push(null);
    rows.push(new TableRow({ children: slice.map(f => new TableCell({
      width: { size: colW, type: WidthType.DXA },
      borders: cellBorders(C.line),
      shading: { fill: C.light, type: ShadingType.CLEAR },
      margins: { top: 90, bottom: 110, left: 150, right: 120 },
      children: f ? [
        P([run(f.label.toUpperCase(), { color: C.deep, bold: true, size: 15, characterSpacing: 20 })], { spacing: { after: 30 } }),
        P([run(f.hint ? f.hint : ' ', { color: C.muted, italics: true, size: 16 })]),
      ] : [P([run(' ')])],
    })) }));
  }
  return new Table({ columnWidths: Array(perRow).fill(colW), width: { size: USABLE, type: WidthType.DXA }, rows });
}

// ---- a shaded callout box (tip / how-to / example) ----
function calloutBox(children, { fill = C.tip, border = C.tipLine } = {}) {
  return new Table({
    columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA },
    rows: [new TableRow({ children: [new TableCell({
      width: { size: USABLE, type: WidthType.DXA },
      borders: cellBorders(border),
      shading: { fill, type: ShadingType.CLEAR },
      margins: { top: 130, bottom: 130, left: 180, right: 180 },
      children,
    })] })],
  });
}

// ---- section structures ----
function structTable(s) {
  const cols = s.columns && s.columns.length ? s.columns : ['Item', 'Notes'];
  const n = cols.length;
  const colW = Math.floor(USABLE / n);
  const widths = Array(n).fill(colW);
  const headRow = new TableRow({ tableHeader: true, children: cols.map(c => new TableCell({
    width: { size: colW, type: WidthType.DXA }, borders: cellBorders(C.line),
    shading: { fill: C.deep, type: ShadingType.CLEAR }, verticalAlign: VerticalAlign.CENTER,
    margins: { top: 70, bottom: 70, left: 130, right: 130 },
    children: [P([run(c, { color: C.white, bold: true, size: 18 })])],
  })) });
  const exampleRows = (s.sampleRows || []).map(r => new TableRow({ children: cols.map((_, ci) => new TableCell({
    width: { size: colW, type: WidthType.DXA }, borders: cellBorders(C.line),
    shading: { fill: C.blueSoft, type: ShadingType.CLEAR },
    margins: { top: 70, bottom: 70, left: 130, right: 130 },
    children: [P([run((ci === 0 ? 'e.g.  ' : '') + (r[ci] || ''), { italics: true, color: C.muted, size: 18 })])],
  })) }));
  const blankRows = Array.from({ length: 3 }, () => new TableRow({ children: cols.map(() => new TableCell({
    width: { size: colW, type: WidthType.DXA }, borders: cellBorders(C.line),
    margins: { top: 90, bottom: 90, left: 130, right: 130 }, children: [P([run('')])],
  })) }));
  return new Table({ columnWidths: widths, width: { size: USABLE, type: WidthType.DXA }, rows: [headRow, ...exampleRows, ...blankRows] });
}

function structChecklist(items) {
  return (items && items.length ? items : ['…']).map(it =>
    P([run('☐  ', { size: 22, color: C.deep }), run(it, { size: 20 })], { spacing: { after: 40 }, indent: { left: 200 } }));
}

function structScale(s) {
  const rows = s.scaleRows && s.scaleRows.length ? s.scaleRows : ['Overall confidence'];
  const labelW = 4680, numW = Math.floor((USABLE - labelW) / 5);
  const widths = [labelW, numW, numW, numW, numW, numW];
  const head = new TableRow({ tableHeader: true, children: [
    new TableCell({ width: { size: labelW, type: WidthType.DXA }, borders: cellBorders(C.line),
      shading: { fill: C.deep, type: ShadingType.CLEAR }, margins: { top: 60, bottom: 60, left: 130, right: 130 },
      children: [P([run('Area   (1 = lost · 5 = confident)', { color: C.white, bold: true, size: 16 })])] }),
    ...['1', '2', '3', '4', '5'].map(nm => new TableCell({ width: { size: numW, type: WidthType.DXA },
      borders: cellBorders(C.line), shading: { fill: C.deep, type: ShadingType.CLEAR }, verticalAlign: VerticalAlign.CENTER,
      margins: { top: 60, bottom: 60, left: 80, right: 80 },
      children: [P([run(nm, { color: C.white, bold: true, size: 18 })], { alignment: AlignmentType.CENTER })] })),
  ] });
  const body = rows.map(r => new TableRow({ children: [
    new TableCell({ width: { size: labelW, type: WidthType.DXA }, borders: cellBorders(C.line), verticalAlign: VerticalAlign.CENTER,
      margins: { top: 70, bottom: 70, left: 130, right: 130 }, children: [P([run(r, { size: 19 })])] }),
    ...[0, 1, 2, 3, 4].map(() => new TableCell({ width: { size: numW, type: WidthType.DXA }, borders: cellBorders(C.line),
      verticalAlign: VerticalAlign.CENTER, margins: { top: 70, bottom: 70, left: 80, right: 80 },
      children: [P([run('☐', { size: 22, color: C.muted })], { alignment: AlignmentType.CENTER })] })),
  ] }));
  return new Table({ columnWidths: widths, width: { size: USABLE, type: WidthType.DXA }, rows: [head, ...body] });
}

function structSketch(label) {
  return new Table({ columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA },
    rows: [new TableRow({ height: { value: 2300, rule: 'atLeast' }, children: [new TableCell({
      width: { size: USABLE, type: WidthType.DXA }, borders: dashed(C.green), verticalAlign: VerticalAlign.CENTER,
      shading: { fill: 'FBFEFC', type: ShadingType.CLEAR }, margins: { top: 200, bottom: 200, left: 180, right: 180 },
      children: [P([run(label || '✎  Sketch your screen here — or paste a screenshot / @Preview render', { color: C.muted, italics: true, size: 19 })], { alignment: AlignmentType.CENTER })],
    })] })] });
}

function structFreeform() {
  return new Table({ columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA },
    rows: [new TableRow({ height: { value: 1100, rule: 'atLeast' }, children: [new TableCell({
      width: { size: USABLE, type: WidthType.DXA }, borders: cellBorders(C.line),
      margins: { top: 110, bottom: 110, left: 160, right: 160 }, children: blank(3),
    })] })] });
}

// ---- assemble body ----
const body = [];
body.push(banner());
body.push(P([run('')], { spacing: { after: 60 } }));
body.push(metaBar(spec.metadataFields));
body.push(P([run('')], { spacing: { after: 80 } }));

// How to use
body.push(calloutBox([
  P([run('▸ How to use this each class', { bold: true, color: C.accentInk, size: 20 })], { spacing: { after: 60 } }),
  ...spec.howToUse.map(t => P([run('•  ', { bold: true, color: C.deep }), run(t, { size: 19 })], { spacing: { after: 30 }, indent: { left: 120 } })),
]));
body.push(P([run('')], { spacing: { after: 120 } }));

// Sections
spec.sections.forEach((s, idx) => {
  const title = `${s.emoji ? s.emoji + '  ' : ''}${s.title}`;
  body.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [run(title, { bold: true, color: C.slate, size: 26 })],
    spacing: { before: idx === 0 ? 60 : 220, after: 40 }, border: { bottom: bd(C.green, 10) } }));
  if (s.purpose) body.push(P([run(s.purpose, { italics: true, color: C.muted, size: 19 })], { spacing: { after: 80 } }));
  (s.prompts || []).forEach(pr => body.push(P([run(pr, { size: 20 })], { numbering: { reference: 'bullets', level: 0 }, spacing: { after: 30 } })));
  if (s.example) {
    body.push(P([run('')], { spacing: { after: 30 } }));
    body.push(calloutBox([P([run('Example   ', { bold: true, color: C.blue, size: 16 }), run(s.example, { italics: true, color: C.slate, size: 18 })])], { fill: C.blueSoft, border: 'D3E0FF' }));
  }
  body.push(P([run('')], { spacing: { after: 50 } }));
  const t = (s.structureType || 'freeform').toLowerCase();
  if (t === 'table') body.push(structTable(s));
  else if (t === 'checklist') body.push(...structChecklist(s.checklistItems));
  else if (t === 'scale') body.push(structScale(s));
  else if (t === 'sketchbox') body.push(structSketch());
  else body.push(structFreeform());
  // optional SECONDARY checklist (e.g. UI-state branches, components used, triage flag)
  if (t !== 'checklist' && s.checklistItems && s.checklistItems.length) {
    body.push(P([run('')], { spacing: { after: 40 } }));
    if (s.checklistLabel) body.push(P([run(s.checklistLabel, { bold: true, color: C.slate, size: 18 })], { spacing: { after: 40 } }));
    body.push(...structChecklist(s.checklistItems));
  }
});

// Footer line
if (spec.footer) {
  body.push(P([run('')], { spacing: { after: 160 } }));
  body.push(calloutBox([P([run(spec.footer, { bold: true, color: C.accentInk, size: 19 })], { alignment: AlignmentType.CENTER })]));
}

// ---- document ----
const doc = new Document({
  creator: 'Compose course', title: spec.templateTitle,
  styles: { default: { document: { run: { font: 'Arial', size: 20 } } } },
  numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•',
    alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 460, hanging: 240 } } } }] }] },
  sections: [{
    properties: { page: { margin: { top: 1080, right: 1080, bottom: 1080, left: 1080 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, border: { top: bd(C.line) },
      children: [run('Weekly Project Design Log  ·  page ', { color: C.muted, size: 16 }),
                new TextRun({ children: [PageNumber.CURRENT], font: 'Arial', color: C.muted, size: 16 }),
                run(' of ', { color: C.muted, size: 16 }),
                new TextRun({ children: [PageNumber.TOTAL_PAGES], font: 'Arial', color: C.muted, size: 16 })] })] }) },
    children: body,
  }],
});

const out = __dirname + '/../project-design-log-TEMPLATE.docx';
Packer.toBuffer(doc).then(buf => { fs.writeFileSync(out, buf); console.log('wrote', out, buf.length, 'bytes,', spec.sections.length, 'sections'); });

// build_docx.js — render blocks.json into a Word doc matching the original proposal layout.
const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, LevelFormat, BorderStyle, WidthType, ShadingType, VerticalAlign,
} = require('docx');

const data = JSON.parse(fs.readFileSync(__dirname + '/blocks.json', 'utf8'));
const C = {
  blue: '2E5496', subtitle: '595959', gray: '7F7F7F', ink: '1A1A1A',
  green: '538135', orange: 'C55A11', purple: '7030A0',
  shade: 'F2F2F2', border: 'BFBFBF', white: 'FFFFFF',
};
const PHASE = { blue: C.blue, green: C.green, orange: C.orange, purple: C.purple };
const USABLE = 9360;
const bd = (color = C.border, size = 4) => ({ style: BorderStyle.SINGLE, size, color });
const cb = (color = C.border) => ({ top: bd(color), bottom: bd(color), left: bd(color), right: bd(color) });
const R = (text, o = {}) => new TextRun({ text, font: 'Arial', size: 22, ...o });
const P = (children, o = {}) => new Paragraph({ children: Array.isArray(children) ? children : [children], ...o });

// pre-assign a numbering reference per numbered block (so each restarts at 1)
const numConfigs = [{ reference: 'bul', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 420, hanging: 240 } } } }] }];
let numIdx = 0;
data.blocks.forEach(b => { if (b.t === 'numbered') { b._ref = 'num' + (numIdx++); numConfigs.push({ reference: b._ref, levels: [{ level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 420, hanging: 260 } } } }] }); } });

const out = [];
// title block
out.push(P([R(data.title, { bold: true, size: 48, color: C.blue })], { alignment: AlignmentType.CENTER, spacing: { before: 200, after: 40 } }));
out.push(P([R(data.subtitle, { size: 26, color: C.subtitle })], { alignment: AlignmentType.CENTER, spacing: { after: 240 } }));
// meta table
out.push(new Table({ columnWidths: [2600, 6760], width: { size: USABLE, type: WidthType.DXA },
  rows: data.meta.map(([label, val]) => new TableRow({ children: [
    new TableCell({ width: { size: 2600, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(label, { bold: true })])] }),
    new TableCell({ width: { size: 6760, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(val, { italics: true, color: C.gray })])] }),
  ] })) }));
out.push(P([R('')], { spacing: { after: 120 } }));

const bullets = (items, ref = 'bul') => items.map(it => P([R(it)], { numbering: { reference: ref, level: 0 }, spacing: { after: 20 } }));

function classCard(cls, goals) {
  const lab = 2200, val = 7160;
  const headCell = (txt, opts = {}) => new TableCell({ width: { size: opts.w, type: WidthType.DXA }, borders: cb(), shading: { fill: C.shade, type: ShadingType.CLEAR }, margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(opts.txt0 || txt, opts.run || {})], opts.par || {})] });
  return new Table({ columnWidths: [lab, val], width: { size: USABLE, type: WidthType.DXA }, rows: [
    new TableRow({ children: [
      new TableCell({ width: { size: lab, type: WidthType.DXA }, borders: cb(), shading: { fill: C.shade, type: ShadingType.CLEAR }, margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(cls, { bold: true })])] }),
      new TableCell({ width: { size: val, type: WidthType.DXA }, borders: cb(), shading: { fill: C.shade, type: ShadingType.CLEAR }, margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R('Date: ', { bold: true }), R('[Date]', { italics: true, color: C.gray })])] }),
    ] }),
    new TableRow({ children: [
      new TableCell({ width: { size: lab, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R('Goals:', { bold: true })])] }),
      new TableCell({ width: { size: val, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: goals.map(g => P([R('•  '), R(g)], { spacing: { after: 20 } })) }),
    ] }),
    new TableRow({ children: [
      new TableCell({ width: { size: lab, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R('Status:', { bold: true })])] }),
      new TableCell({ width: { size: val, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R('[Not Started / In Progress / Complete]', { italics: true, color: C.gray })])] }),
    ] }),
    new TableRow({ children: [
      new TableCell({ width: { size: lab, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R('Notes:', { bold: true })])] }),
      new TableCell({ width: { size: val, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R('[Add progress notes, blockers, or questions here]', { italics: true, color: C.gray })])] }),
    ] }),
  ] });
}

// self-paced milestone: colored heading + focus + checklist + a status/notes fill line
function milestone(b) {
  const o = [];
  o.push(P([R(b.name, { bold: true, size: 24, color: PHASE[b.color] || C.blue })], { spacing: { before: 240, after: 50 } }));
  if (b.focus) o.push(P([R('Focus: ', { bold: true }), R(b.focus)], { spacing: { after: 70 } }));
  (b.items || []).forEach(it => o.push(P([R('☐  ', { size: 24 }), R(it)], { spacing: { after: 24 }, indent: { left: 200 } })));
  o.push(P([R('')], { spacing: { after: 20 } }));
  o.push(new Table({ columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA }, rows: [new TableRow({ children: [new TableCell({
    width: { size: USABLE, type: WidthType.DXA }, borders: cb(), margins: { top: 70, bottom: 70, left: 120, right: 120 },
    children: [P([R('Status & notes:  ', { bold: true }), R(b.statusHint || '[done / in progress / not started — and anything blocking it]', { italics: true, color: C.gray })])] })] })] }));
  return o;
}
// fillable table: shaded header row + optional example rows (gray italic) + blank rows to fill
function fillTable(headers, rows, blanks) {
  const n = headers.length, colW = Math.floor(USABLE / n);
  const head = new TableRow({ tableHeader: true, children: headers.map(h => new TableCell({ width: { size: colW, type: WidthType.DXA }, borders: cb(), shading: { fill: C.shade, type: ShadingType.CLEAR }, margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R(h, { bold: true })])] })) });
  const ex = (rows || []).map(r => new TableRow({ children: headers.map((_, i) => new TableCell({ width: { size: colW, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, children: [P([R(r[i] || '', { italics: true, color: C.gray })])] })) }));
  const bl = Array.from({ length: blanks == null ? 3 : blanks }, () => new TableRow({ children: headers.map(() => new TableCell({ width: { size: colW, type: WidthType.DXA }, borders: cb(), margins: { top: 90, bottom: 90, left: 120, right: 120 }, children: [P([R('')])] })) }));
  return new Table({ columnWidths: Array(n).fill(colW), width: { size: USABLE, type: WidthType.DXA }, rows: [head, ...ex, ...bl] });
}
// a labeled blank box for free writing
function fillBox(label, lines) {
  const o = [];
  if (label) o.push(P([R(label, { bold: true })], { spacing: { before: 80, after: 30 } }));
  o.push(new Table({ columnWidths: [USABLE], width: { size: USABLE, type: WidthType.DXA }, rows: [new TableRow({ height: { value: (lines || 2) * 280 + 160, rule: 'atLeast' }, children: [new TableCell({ width: { size: USABLE, type: WidthType.DXA }, borders: cb(), margins: { top: 80, bottom: 80, left: 140, right: 140 }, children: [P([R('')])] })] })] }));
  return o;
}

for (const b of data.blocks) {
  switch (b.t) {
    case 'h1': out.push(P([R(b.text, { bold: true, size: 30, color: C.blue })], { spacing: { before: 320, after: 100 } })); break;
    case 'h2': out.push(P([R(b.text, { bold: true, size: 24 })], { spacing: { before: 200, after: 60 } })); break;
    case 'h3b': out.push(P([R(b.text, { bold: true })], { spacing: { before: 120, after: 40 } })); break;
    case 'phase': out.push(P([R(b.text, { bold: true, size: 24, color: PHASE[b.color] || C.blue })], { spacing: { before: 260, after: 60 } })); break;
    case 'para': out.push(P([R(b.text, b.ph ? { italics: true, color: C.gray } : {})], { spacing: { after: 80 } })); break;
    case 'field': out.push(P([R(b.label + ' ', { bold: true }), R(b.value, { italics: true, color: C.gray })], { spacing: { after: 60 } })); break;
    case 'kvTable': out.push(new Table({ columnWidths: [3000, 6360], width: { size: USABLE, type: WidthType.DXA }, rows: b.rows.map(([label, val, ph]) => new TableRow({ children: [
      new TableCell({ width: { size: 3000, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(label, { bold: true })])] }),
      new TableCell({ width: { size: 6360, type: WidthType.DXA }, borders: cb(), margins: { top: 60, bottom: 60, left: 120, right: 120 }, verticalAlign: VerticalAlign.CENTER, children: [P([R(val, ph ? { italics: true, color: C.gray } : {})])] }),
    ] })) })); break;
    case 'numbered': out.push(...b.items.map(it => P([R(it)], { numbering: { reference: b._ref, level: 0 }, spacing: { after: 20 } }))); break;
    case 'bullets': out.push(...bullets(b.items)); break;
    case 'checklist': out.push(...b.items.map(it => P([R('☐  ', { size: 24 }), R(it)], { spacing: { after: 30 }, indent: { left: 200 } }))); break;
    case 'classCard': out.push(classCard(b.cls, b.goals)); out.push(P([R('')], { spacing: { after: 100 } })); break;
    case 'milestone': out.push(...milestone(b)); out.push(P([R('')], { spacing: { after: 100 } })); break;
    case 'table': out.push(fillTable(b.headers, b.rows, b.blanks)); if (b.note) out.push(P([R(b.note, { italics: true, color: C.gray, size: 20 })], { spacing: { before: 30, after: 60 } })); break;
    case 'fillBox': out.push(...fillBox(b.label, b.lines)); break;
    case 'note': out.push(P([R('')], { spacing: { after: 160 } })); out.push(P([R(b.text, { italics: true, color: C.subtitle })], { alignment: AlignmentType.CENTER })); break;
    case 'space': out.push(P([R('')], { spacing: { after: b.h || 120 } })); break;
  }
}

const doc = new Document({
  creator: 'Android course', title: data.title,
  styles: { default: { document: { run: { font: 'Arial', size: 22 } } } },
  numbering: { config: numConfigs },
  sections: [{ properties: { page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } }, children: out }],
});
const file = __dirname + '/../../../Downloads/android_final_project.docx';
Packer.toBuffer(doc).then(buf => { fs.writeFileSync(file, buf); console.log('wrote', file, buf.length, 'bytes'); });

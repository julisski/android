# Compose Logbook — template build files

Source for the weekly project-design-log template (output lives one level up):
- `../project-design-log-TEMPLATE.docx`  — editable Word template (fill each class)
- `../project-design-log-TEMPLATE.pdf`   — printable companion
- `../project-design-log-TEMPLATE.html`  — web/print source for the PDF

## Regenerate after editing spec.json
    npm init -y && npm install docx
    node build_docx.js     # -> ../project-design-log-TEMPLATE.docx
    node build_html.js     # -> ../project-design-log-TEMPLATE.html
    # PDF: print the HTML to PDF (Chrome --headless --print-to-pdf)

All content lives in `spec.json` — both builders read it, so edits stay in sync.

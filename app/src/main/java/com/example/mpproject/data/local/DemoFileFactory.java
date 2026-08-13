package com.example.mpproject.data.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Writes the stand-in documents that back the demo account's file-based records
// (module notes, personal note attachments, research papers).
//
// The demo data ships with the app, so there are no real lecture slides or journal PDFs to bundle.
// Instead each record points at a small generated single-page PDF that says what it stands in for,
// which keeps the file viewer, the "open note" flow, and the file size labels genuinely working
// rather than pointing at paths that do not exist.
final class DemoFileFactory {

    private DemoFileFactory() {}

    // Writes a one-page PDF at absolutePath containing the given heading and body lines.
    // Returns the size in bytes, or 0 if the file could not be written.
    static long writePlaceholderPdf(File target, String heading, String... bodyLines) {
        try {
            File parent = target.getParentFile();
            if (parent != null) parent.mkdirs();

            byte[] pdf = buildPdf(heading, bodyLines);
            try (FileOutputStream out = new FileOutputStream(target)) {
                out.write(pdf);
            }
            return pdf.length;
        } catch (IOException e) {
            return 0L;
        }
    }

    // Builds a minimal but valid PDF 1.4 document: catalog, page tree, one page, one Helvetica
    // font, and a content stream of text lines. Offsets in the xref table are counted as we go,
    // which is why the document is assembled as ASCII bytes rather than composed as one string.
    private static byte[] buildPdf(String heading, String[] bodyLines) {
        StringBuilder text = new StringBuilder();
        text.append("BT\n/F1 16 Tf\n60 780 Td\n(").append(escape(heading)).append(") Tj\n");
        text.append("/F1 11 Tf\n");
        for (String line : bodyLines) {
            text.append("0 -24 Td\n(").append(escape(line)).append(") Tj\n");
        }
        text.append("ET");
        String contents = text.toString();

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Length " + contents.length() + " >>\nstream\n" + contents + "\nendstream");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = pdf.length();
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }

        int xrefOffset = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");

        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    // Backslash, and both parentheses, terminate a PDF string literal and have to be escaped.
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}

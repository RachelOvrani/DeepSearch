package project.Common;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class TextExtractor {

    public static boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".txt") || fileName.endsWith(".log") || fileName.endsWith(".md") ||
                fileName.endsWith(".java") || fileName.endsWith(".html") || fileName.endsWith(".xml") ||
                fileName.endsWith(".json") || fileName.endsWith(".csv") || fileName.endsWith(".pdf") ||
                fileName.endsWith(".doc") || fileName.endsWith(".docx");
    }

    public static String extractText(File file) throws IOException {
        String name = file.getName().toLowerCase();

        try {
            if (name.matches(".*\\.(txt|log|md|java|html|xml|json|csv)$")) {
                return extractTextFromTxt(file);
            } else if (name.endsWith(".pdf")) {
                return extractTextFromPDF(file);
            } else if (name.endsWith(".doc")) {
                return extractTextFromDoc(file);
            } else if (name.endsWith(".docx")) {
                return extractTextFromDocx(file);
            }
        } catch (Exception e) {
            ProjectLogger.warning("Error extracting text from file: " + file.getPath() + " - " + e.getMessage());
        }

        return "";
    }

    /**
     * חילוץ טקסט מקובץ טקסט פשוט עם זיהוי קידוד אוטומטי
     */
    public static String extractTextFromTxt(File file) throws IOException {
        Charset detectedCharset = detectCharset(file);
        return Files.readString(file.toPath(), detectedCharset);
    }

    private static Charset detectCharset(File file) throws IOException {
        byte[] buf = new byte[4096];
        UniversalDetector detector = new UniversalDetector(null);

        try (FileInputStream fis = new FileInputStream(file)) {
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
        }

        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        if (encoding == null) {
            encoding = "UTF-8"; // ברירת מחדל
        }
        return Charset.forName(encoding);
    }

    public static String extractTextFromPDF(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    public static String extractTextFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            return doc.getParagraphs().stream()
                    .map(p -> p.getText())
                    .collect(Collectors.joining("\n"));
        }
    }

    public static String extractTextFromDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }
}

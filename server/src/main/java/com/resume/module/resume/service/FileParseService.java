package com.resume.module.resume.service;

import com.resume.common.TextSanitizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileParseService {

    public String parse(Path path, String fileType) throws IOException {
        return switch (fileType.toUpperCase()) {
            case "PDF" -> parsePdf(path);
            case "DOCX" -> parseDocx(path);
            case "DOC" -> parseDoc(path);
            case "JPG", "JPEG", "PNG" -> "[图片文件，请调用OCR接口识别文本]";
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }

    public String detectType(String filename) {
        if (filename == null) {
            return "UNKNOWN";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx")) return "DOCX";
        if (lower.endsWith(".doc")) return "DOC";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "JPG";
        if (lower.endsWith(".png")) return "PNG";
        return "UNKNOWN";
    }

    private String parsePdf(Path path) throws IOException {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            return TextSanitizer.forParsedText(new PDFTextStripper().getText(doc));
        }
    }

    private String parseDocx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return TextSanitizer.forParsedText(extractor.getText());
        }
    }

    private String parseDoc(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            return TextSanitizer.forParsedText(extractor.getText());
        }
    }
}

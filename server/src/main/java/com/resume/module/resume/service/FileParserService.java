package com.resume.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class FileParserService {

    /**
     * 解析 PDF 文件，提取文本内容
     */
    public String parsePdf(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF 解析完成，共 {} 页，提取文本 {} 字符", document.getNumberOfPages(), text.length());
            return text;
        }
    }

    /**
     * 解析 Word (.docx) 文件，提取文本内容
     */
    public String parseDocx(Path filePath) throws IOException {
        try (var fis = new java.io.FileInputStream(filePath.toFile());
             var docx = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();

            // 提取段落文本
            for (var paragraph : docx.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }

            // 提取表格文本
            for (var table : docx.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }

            String result = sb.toString().trim();
            log.info("DOCX 解析完成，提取文本 {} 字符", result.length());
            return result;
        }
    }
}

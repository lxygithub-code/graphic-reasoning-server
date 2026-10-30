package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.date.DateUtil;
import com.lee.graphic_reasoning_server.po.Question;
import com.lee.graphic_reasoning_server.vo.QuestionDetailVO;
import com.lee.graphic_reasoning_server.vo.WrongPdfItemVO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final OssService ossService;
    private static BaseFont baseFontChinese;

    private synchronized BaseFont getChineseFont() throws Exception {
        if (baseFontChinese == null) {
            ClassPathResource resource = new ClassPathResource("fonts/msyh.ttc");
            try (InputStream is = resource.getInputStream()) {
                byte[] fontBytes = is.readAllBytes();
                baseFontChinese = BaseFont.createFont("msyh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
            }
        }
        return baseFontChinese;
    }

    public String generateWrongPdf(List<WrongPdfItemVO> items) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new WatermarkPageEvent("图推达人 - 仅供个人学习使用", getChineseFont()));
            document.addTitle("图推达人 - 我的错题本");
            document.open();

            BaseFont bf = getChineseFont();
            Font titleFont = new Font(bf, 16, Font.BOLD, Color.BLACK);
            Font textFont = new Font(bf, 11, Font.NORMAL, Color.DARK_GRAY);
            Font redFont = new Font(bf, 11, Font.BOLD, Color.RED);
            Font greenFont = new Font(bf, 11, Font.BOLD, new Color(0, 128, 0));

            document.add(new Paragraph("图推达人 - 我的错题本", titleFont));
            document.add(new Paragraph(" "));

            int index = 1;
            for (WrongPdfItemVO item : items) {
                Question q = item.getQuestion();

                // 1. 标题与耗时
                String timeStr = item.getDuration() != null ? " (本题耗时: " + item.getDuration() + "秒)" : "";
                Paragraph pTitle = new Paragraph("第 " + index + " 题" + timeStr, new Font(bf, 12, Font.BOLD));
                document.add(pTitle);

                // 2. 题干
                document.add(new Paragraph(q.getContent(), textFont));
                if (q.getImageUrl() != null && !q.getImageUrl().isEmpty()) {
                    addImageToPdf(document, q.getImageUrl());
                }

                // 3. 选项渲染逻辑（横向文字 / 纵向图文）
                if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                    boolean allText = q.getOptions().stream().allMatch(o -> "text".equals(o.getType()));

                    if (allText) {
                        // 纯文字选项，横向排成一行
                        PdfPTable table = new PdfPTable(q.getOptions().size());
                        table.setWidthPercentage(100);
                        table.setSpacingBefore(5f);
                        table.setSpacingAfter(10f);
                        float[] widths = new float[q.getOptions().size()];
                        Arrays.fill(widths, 1f);
                        table.setWidths(widths);

                        for (Question.Option opt : q.getOptions()) {
                            String content = opt.getKey() + ". " + opt.getValue();
                            Paragraph p;
                            if (opt.getKey().equals(q.getCorrectOption())) {
                                p = new Paragraph(content, greenFont);
                            } else if (opt.getKey().equals(item.getUserOption())) {
                                p = new Paragraph(content, redFont);
                            } else {
                                p = new Paragraph(content, textFont);
                            }
                            PdfPCell cell = new PdfPCell(p);
                            cell.setBorder(Rectangle.NO_BORDER);
                            cell.setPadding(0);
                            table.addCell(cell);
                        }
                        document.add(table);
                    } else {
                        // 图文混合选项，纵向排列
                        for (Question.Option opt : q.getOptions()) {
                            String prefix = opt.getKey() + ". ";
                            String content = prefix + ("text".equals(opt.getType()) ? opt.getValue() : "[图片选项]");

                            Paragraph optP;
                            if (opt.getKey().equals(q.getCorrectOption())) {
                                optP = new Paragraph(content + " (正确答案)", greenFont);
                            } else if (opt.getKey().equals(item.getUserOption())) {
                                optP = new Paragraph(content + " (您的错选)", redFont);
                            } else {
                                optP = new Paragraph(content, textFont);
                            }
                            document.add(optP);

                            if ("image".equals(opt.getType())) {
                                addImageToPdf(document, opt.getValue());
                            }
                        }
                    }
                }

                // 4. 解析渲染逻辑（修改点：有解析才显示，且不展示来源）
                boolean hasDefaultAnalysis = q.getAnalysis() != null && !q.getAnalysis().trim().isEmpty();
                boolean hasPlatformAnalysis = item.getAnalyses() != null && !item.getAnalyses().isEmpty();

                if (hasDefaultAnalysis || hasPlatformAnalysis) {
                    document.add(new Paragraph("【解析】", new Font(bf, 11, Font.BOLD)));

                    if (hasDefaultAnalysis) {
                        document.add(new Paragraph(q.getAnalysis(), textFont));
                    }

                    if (hasPlatformAnalysis) {
                        for (QuestionDetailVO.AnalysisVO analysis : item.getAnalyses()) {
                            // 去掉了“来源: xxx”的展示
                            if ("image".equals(analysis.getType())) {
                                addImageToPdf(document, analysis.getContent());
                            } else {
                                document.add(new Paragraph(analysis.getContent(), textFont));
                            }
                        }
                    }
                }

                // 分隔线
                document.add(new Paragraph("------------------------------------------------------------------------"));
                document.add(new Paragraph(" "));
                index++;

                // 每 5 题强制分页，防止图片过多导致内存溢出
                if (index % 5 == 0) {
                    document.newPage();
                }
            }

            document.close();

            byte[] pdfBytes = out.toByteArray();
            // ★ 修改点 1：生成包含日期和中文的文件名
            String dateStr = DateUtil.format(new Date(), "yyyyMMdd_HHmmss");
            String fileName = "图推达人_错题本_" + dateStr + ".pdf";
            String objectKey = "pdf/" + fileName;
            return ossService.uploadBytes(pdfBytes, objectKey, "application/pdf");

        } catch (Exception e) {
            log.error("生成 PDF 失败", e);
            throw new RuntimeException("生成 PDF 失败");
        }
    }

    private void addImageToPdf(Document document, String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            Image image = Image.getInstance(url);
            if (image.getWidth() > 300 || image.getHeight() > 300) {
                image.scaleToFit(300, 300);
            }
            image.setCompressionLevel(9);
            image.setAlignment(Image.ALIGN_CENTER);
            document.add(image);
            document.add(new Paragraph(" "));
        } catch (Exception e) {
            log.warn("图片加载失败: " + imageUrl, e);
        }
    }

    static class WatermarkPageEvent extends PdfPageEventHelper {
        private final String watermarkText;
        private final BaseFont bf;

        public WatermarkPageEvent(String text, BaseFont bf) {
            this.watermarkText = text;
            this.bf = bf;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            try {
                canvas.beginText();
                canvas.setFontAndSize(bf, 24);
                canvas.setColorFill(new Color(200, 200, 200, 30));
                canvas.showTextAligned(Element.ALIGN_CENTER, watermarkText,
                        (document.right() + document.left()) / 2,
                        (document.top() + document.bottom()) / 2,
                        45);
                canvas.endText();
            } catch (Exception e) {
                log.error("水印添加失败", e);
            }
        }
    }
}
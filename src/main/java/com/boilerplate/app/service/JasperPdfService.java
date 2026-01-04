package com.boilerplate.app.service;

import com.boilerplate.app.model.KdpPresets;
import com.boilerplate.app.model.KdpTemplate;
import com.boilerplate.app.model.Story;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF generation service using JasperReports.
 * Supports KDP template configurations for different book formats.
 */
public class JasperPdfService {

    private static final Logger logger = LogManager.getLogger(JasperPdfService.class);

    // Current template (can be changed via setter)
    private KdpTemplate template = KdpPresets.getDefault();

    /**
     * Set the KDP template to use for PDF generation.
     */
    public void setTemplate(KdpTemplate template) {
        this.template = template;
        logger.info("Template set: {}", template.getName());
    }

    /**
     * Get the current template.
     */
    public KdpTemplate getTemplate() {
        return template;
    }

    /**
     * Generates JasperPrint object from story data using current template.
     * Dynamically adjusts page size based on template settings.
     */
    private JasperPrint generateReport(Story story) throws JRException {
        logger.info("Generating report for: {} with template: {}", story.getTitle(), template.getName());
        logger.info("Page size: {}x{} inches ({}x{} pts)",
                template.getPageWidth(), template.getPageHeight(),
                template.getPageWidthPoints(), template.getPageHeightPoints());

        // Set temp dir for compilation
        System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));

        // Load report template as JasperDesign to modify page size
        InputStream reportStream = getClass().getResourceAsStream("/reports/story_book.jrxml");
        if (reportStream == null) {
            logger.error("Report template not found: /reports/story_book.jrxml");
            throw new JRException("Report template not found");
        }

        // Load as design to modify dimensions
        JasperDesign design = JRXmlLoader.load(reportStream);

        // Apply template page dimensions
        int pageWidth = template.getPageWidthPoints();
        int pageHeight = template.getPageHeightPoints();
        int marginTop = template.getMarginTopPoints();
        int marginBottom = template.getMarginBottomPoints();
        int marginLeft = template.getMarginInsidePoints();
        int marginRight = template.getMarginOutsidePoints();

        design.setPageWidth(pageWidth);
        design.setPageHeight(pageHeight);
        design.setTopMargin(marginTop);
        design.setBottomMargin(marginBottom);
        design.setLeftMargin(marginLeft);
        design.setRightMargin(marginRight);

        // Column width must be page width - margins
        int columnWidth = pageWidth - marginLeft - marginRight;
        design.setColumnWidth(columnWidth);

        logger.info("Applied page: {}x{}, margins: T={} B={} L={} R={}",
                pageWidth, pageHeight, marginTop, marginBottom, marginLeft, marginRight);

        // --- DYNAMICALLY RESIZE BANDS ---

        // Adjust Page Footer
        int footerHeight = 0;
        if (design.getPageFooter() != null) {
            JRDesignBand footerBand = (JRDesignBand) design.getPageFooter();
            footerHeight = footerBand.getHeight();
            // Resize footer elements width
            for (JRChild child : footerBand.getChildren()) {
                if (child instanceof JRDesignElement) {
                    ((JRDesignElement) child).setWidth(columnWidth);
                }
            }
        }

        // Adjust Title Band (if exists)
        int titleHeight = 0;
        if (design.getTitle() != null) {
            JRDesignBand titleBand = (JRDesignBand) design.getTitle();
            titleHeight = titleBand.getHeight();
            for (JRChild child : titleBand.getChildren()) {
                if (child instanceof JRDesignElement) {
                    ((JRDesignElement) child).setWidth(columnWidth);
                }
            }
        }

        // Adjust Detail Band (The most important one)
        if (design.getDetailSection() != null && design.getDetailSection().getBands().length > 0) {
            JRDesignBand detailBand = (JRDesignBand) design.getDetailSection().getBands()[0];

            // Available height for detail
            // Note: If using breaks, page headers/footers apply.
            int availableHeight = pageHeight - marginTop - marginBottom - footerHeight;

            logger.info("Resizing Detail Band. Orig Height: {}, Available: {}", detailBand.getHeight(),
                    availableHeight);

            // Set band height
            detailBand.setHeight(availableHeight);

            // Resize elements inside detail band
            JRDesignElement imageElement = null;
            JRDesignElement textElement = null;

            for (JRChild child : detailBand.getChildren()) {
                if (child instanceof JRDesignElement) {
                    JRDesignElement el = (JRDesignElement) child;
                    el.setWidth(columnWidth); // Fix width

                    if (el instanceof JRDesignImage) {
                        imageElement = el;
                    } else if (el instanceof JRDesignTextField) {
                        textElement = el;
                    }
                }
            }

            // Layout Logic based on KdpTemplate Layout
            String layoutName = template.getLayout().name();

            if (imageElement != null && textElement != null) {
                if ("FULL_PAGE_IMAGE".equals(layoutName)) {
                    // Maximum image size
                    int imgH = (int) (availableHeight * 0.90);
                    imageElement.setY(0);
                    imageElement.setHeight(imgH);

                    textElement.setY(imgH + 5);
                    textElement.setHeight(availableHeight - imgH - 5);

                } else if ("TEXT_ONLY".equals(layoutName)) {
                    // Hide image
                    imageElement.setHeight(0);
                    imageElement.setY(0);

                    textElement.setY(0);
                    textElement.setHeight(availableHeight);

                } else if ("SIDEBAR_IMAGE".equals(layoutName)) {
                    // Side-by-side
                    int imgW = (int) (columnWidth * 0.45);
                    int textW = columnWidth - imgW - 15;

                    imageElement.setX(0);
                    imageElement.setY(0);
                    imageElement.setWidth(imgW);
                    imageElement.setHeight(availableHeight);

                    textElement.setX(imgW + 15);
                    textElement.setY(0);
                    textElement.setWidth(textW);
                    textElement.setHeight(availableHeight);

                } else {
                    // TOP_IMAGE_TEXT_BELOW (Default) - Smart Logic
                    // We need to support both Landscape and Portrait layouts optimally.
                    // Instead of resizing the single elements, we will clone them to create two
                    // sets.

                    // 1. Remove original elements from band to avoid duplication/confusion
                    detailBand.removeElement(imageElement);
                    detailBand.removeElement(textElement);

                    // Expression: Is Landscape? (Default to true if null)
                    String isLandscapeExpr = "($F{imageWidth} == null || $F{imageHeight} == null) ? true : ($F{imageWidth} >= $F{imageHeight})";
                    String isPortraitExpr = "($F{imageWidth} != null && $F{imageHeight} != null) && ($F{imageHeight} > $F{imageWidth})";

                    // --- SET 1: LANDSCAPE (Standard 55/45 split) ---
                    int landImgH = (int) (availableHeight * 0.55);

                    JRDesignImage imgLand = (JRDesignImage) imageElement.clone();
                    imgLand.setUUID(java.util.UUID.randomUUID());
                    imgLand.setY(0);
                    imgLand.setHeight(landImgH);
                    imgLand.setPrintWhenExpression(new JRDesignExpression(isLandscapeExpr));

                    JRDesignTextField txtLand = (JRDesignTextField) textElement.clone();
                    txtLand.setUUID(java.util.UUID.randomUUID());
                    txtLand.setY(landImgH + 10);
                    txtLand.setHeight(availableHeight - landImgH - 10);
                    txtLand.setPrintWhenExpression(new JRDesignExpression(isLandscapeExpr));

                    // --- SET 2: PORTRAIT (Taller 85/15 split) ---
                    // Portrait images need more vertical space to avoid looking tiny
                    int portImgH = (int) (availableHeight * 0.85);

                    JRDesignImage imgPort = (JRDesignImage) imageElement.clone();
                    imgPort.setUUID(java.util.UUID.randomUUID());
                    imgPort.setY(0);
                    imgPort.setHeight(portImgH);
                    imgPort.setPrintWhenExpression(new JRDesignExpression(isPortraitExpr));

                    JRDesignTextField txtPort = (JRDesignTextField) textElement.clone();
                    txtPort.setUUID(java.util.UUID.randomUUID());
                    txtPort.setY(portImgH + 10);
                    txtPort.setHeight(availableHeight - portImgH - 10);
                    txtPort.setPrintWhenExpression(new JRDesignExpression(isPortraitExpr));

                    // Add all to band
                    detailBand.addElement(imgLand);
                    detailBand.addElement(txtLand);
                    detailBand.addElement(imgPort);
                    detailBand.addElement(txtPort);

                    logger.info("Applied Smart Layout: Landscape ({}%) / Portrait ({}%)", 55, 85);
                }
            }
        }

        // Compile modified report
        JasperReport jasperReport = JasperCompileManager.compileReport(design);

        // Prepare parameters with template settings
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("BookTitle", story.getTitle() != null ? story.getTitle() : "Untitled Story");
        parameters.put("AuthorName", story.getAuthor() != null ? story.getAuthor() : "");
        parameters.put("FontSize", template.getFontSize());
        parameters.put("FontFamily", template.getFontFamily());
        parameters.put("Layout", template.getLayout().name());

        // Prepare data source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(story.getScenes());
        logger.info("Data source: {} scenes", story.getScenes().size());

        // Fill report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        logger.info("Report generated: {} pages", jasperPrint.getPages().size());

        return jasperPrint;
    }

    /**
     * Export story to PDF file using current template.
     */
    public void exportToPdf(Story story, String destPath) throws JRException {
        logger.info("Exporting PDF to: {}", destPath);

        JasperPrint jasperPrint = generateReport(story);
        JasperExportManager.exportReportToPdfFile(jasperPrint, destPath);

        logger.info("PDF exported: {}", destPath);
    }

    /**
     * Export story to PDF with specific template.
     */
    public void exportToPdf(Story story, String destPath, KdpTemplate template) throws JRException {
        setTemplate(template);
        exportToPdf(story, destPath);
    }

    /**
     * View PDF preview using current template.
     */
    public void viewPdf(Story story) throws JRException {
        logger.info("Launching PDF preview for: {}", story.getTitle());

        JasperPrint jasperPrint = generateReport(story);

        net.sf.jasperreports.view.JasperViewer viewer = new net.sf.jasperreports.view.JasperViewer(jasperPrint, false);
        viewer.setTitle("📖 " + story.getTitle() + " - " + template.getName());
        viewer.setVisible(true);

        logger.info("PDF preview launched");
    }

    /**
     * View PDF preview with specific template.
     */
    public void viewPdf(Story story, KdpTemplate template) throws JRException {
        setTemplate(template);
        viewPdf(story);
    }
}

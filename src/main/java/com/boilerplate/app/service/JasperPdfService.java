package com.boilerplate.app.service;

import com.boilerplate.app.model.Story;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class JasperPdfService {

    /**
     * Generates JasperPrint object from story data.
     * Common logic extracted to avoid duplication.
     */
    private JasperPrint generateReport(Story story) throws JRException {
        System.out.println("JasperPdfService: Generating report for story: " + story.getTitle());

        // Set temp dir for compilation (from reference project)
        System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));

        // Load report template
        InputStream reportStream = getClass().getResourceAsStream("/reports/story_book.jrxml");
        if (reportStream == null) {
            System.err.println("JasperPdfService: Report template not found");
            throw new JRException("Report template not found: /reports/story_book.jrxml");
        }

        // Compile report
        System.out.println("JasperPdfService: Compiling report template");
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // Prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("BookTitle", story.getTitle());
        parameters.put("AuthorName", story.getAuthor());
        System.out.println(
                "JasperPdfService: Report params - title: " + story.getTitle() + ", author: " + story.getAuthor());

        // Prepare data source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(story.getScenes());
        System.out.println("JasperPdfService: Data source created with " + story.getScenes().size() + " scenes");

        // Fill report
        System.out.println("JasperPdfService: Filling report with data");
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        System.out.println("JasperPdfService: Report generated. Total pages: " + jasperPrint.getPages().size());
        return jasperPrint;
    }

    public void exportToPdf(Story story, String destPath) throws JRException {
        System.out.println("JasperPdfService: Exporting PDF to: " + destPath);

        JasperPrint jasperPrint = generateReport(story);

        // Export to PDF
        JasperExportManager.exportReportToPdfFile(jasperPrint, destPath);
        System.out.println("JasperPdfService: PDF exported successfully");
    }

    public void viewPdf(Story story) throws JRException {
        System.out.println("JasperPdfService: Launching PDF preview for: " + story.getTitle());

        JasperPrint jasperPrint = generateReport(story);

        // View Report
        net.sf.jasperreports.view.JasperViewer viewer = new net.sf.jasperreports.view.JasperViewer(jasperPrint, false);
        viewer.setTitle("Story Preview - " + story.getTitle());
        viewer.setVisible(true);

        System.out.println("JasperPdfService: PDF preview launched");
    }
}

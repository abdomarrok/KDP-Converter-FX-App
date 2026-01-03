package com.boilerplate.app.service;

import com.boilerplate.app.model.KdpTemplate;
import com.boilerplate.app.model.Story;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * JavaFX Service for generating PDFs.
 * Provides cancellation support and progress updates.
 */
public class PdfGenerationService extends Service<Void> {
    
    private static final Logger logger = LogManager.getLogger(PdfGenerationService.class);
    
    private Story story;
    private File outputFile;
    private KdpTemplate template;
    private final JasperPdfService pdfService = new JasperPdfService();
    
    public void setStory(Story story) {
        this.story = story;
    }
    
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }
    
    public void setTemplate(KdpTemplate template) {
        this.template = template;
    }
    
    @Override
    protected Task<Void> createTask() {
        if (story == null || outputFile == null || template == null) {
            throw new IllegalStateException("Story, output file, and template must be set");
        }
        
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparing PDF generation...");
                updateProgress(0, 100);
                
                updateMessage("Generating PDF pages...");
                updateProgress(25, 100);
                
                pdfService.exportToPdf(story, outputFile.getAbsolutePath(), template);
                
                updateMessage("PDF generation complete");
                updateProgress(100, 100);
                
                return null;
            }
        };
    }
}


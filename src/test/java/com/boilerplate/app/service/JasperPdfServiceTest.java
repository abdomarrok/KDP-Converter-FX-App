package com.boilerplate.app.service;

import com.boilerplate.app.model.KdpPresets;
import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JasperPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testPdfGeneration() throws Exception {
        // Arrange
        JasperPdfService service = new JasperPdfService();
        service.setTemplate(KdpPresets.getDefault()); // 6x9 No Bleed

        Story story = new Story();
        story.setTitle("Test Story");
        story.setAuthor("Test Author");

        List<Scene> scenes = new ArrayList<>();
        Scene scene = new Scene();
        scene.setText("This is a test scene.");
        // We leave image null or provide a dummy path if needed.
        // Assuming the report handles null images gracefully (via PrintWhenExpression)
        scenes.add(scene);
        story.setScenes(scenes);

        File outputFile = tempDir.resolve("output.pdf").toFile();

        // Act
        service.exportToPdf(story, outputFile.getAbsolutePath());

        // Assert
        assertThat(outputFile).exists();
        assertThat(outputFile.length()).isGreaterThan(0);
    }
}

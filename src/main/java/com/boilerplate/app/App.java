package com.boilerplate.app;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        scene = new Scene(loadFXML("view/main_view"), 800, 600);
        scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Boilerplate App");
        stage.show();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    private static com.boilerplate.app.repository.EmbeddedDatabase db;

    @Override
    public void init() throws Exception {
        db = new com.boilerplate.app.repository.EmbeddedDatabase();
        db.startDatabase();
    }

    @Override
    public void stop() throws Exception {
        if (db != null) {
            db.stopDatabase();
        }
        com.boilerplate.app.repository.DatabaseConnection.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}

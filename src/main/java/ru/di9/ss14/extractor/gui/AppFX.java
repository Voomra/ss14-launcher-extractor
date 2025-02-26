package ru.di9.ss14.extractor.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class AppFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        var loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/view/main.fxml")));
        Parent root = loader.load();
        var controller = (MainController)loader.getController();
        controller.setStage(stage);

        stage.setTitle("SS14: Content Extractor");
        stage.setScene(new Scene(root, 640.0, 480.0));
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

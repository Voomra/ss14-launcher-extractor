package ru.di9.ss14.extractor.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.Setter;
import ru.di9.ss14.extractor.ContentDbManager;
import ru.di9.ss14.extractor.ContentRec;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class ExportInfoController implements Initializable {
    private static final Path EMPTY_PATH = Paths.get("");

    @FXML
    public Label labelCurrentExport;

    @FXML
    public Button btnCancel;

    @Setter
    private Stage stage;

    @Setter
    private ContentDbManager manager;

    private volatile boolean running = false;
    private Task<Void> task;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnCancel.setDisable(false);
        btnCancel.setText("Отменить");

        btnCancel.setOnAction(e -> {
            btnCancel.setDisable(true);
            btnCancel.setText("Отменяется...");
            stopExport();
        });
    }

    public void startExport(ContentRec contentRec, Path currentDir) {
        running = true;

        task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exportRecursiveFolder(contentRec, currentDir, new AtomicReference<>(EMPTY_PATH));
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            stage.close();
            task = null;
        });
        task.setOnCancelled(e -> {
            running = false;
            stage.close();
            task = null;
        });

        var thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void stopExport() {
        task.cancel();
    }

    private void exportRecursiveFolder(ContentRec contentRec, Path currentDir, AtomicReference<Path> refExportedPath) throws IOException {
        if (!running) {
            return;
        }

        var subDir = currentDir.resolve(contentRec.getName());
        if (Files.notExists(subDir)) {
            Files.createDirectory(subDir);
        }

        for (ContentRec rec : contentRec.getChildren()) {
            if (!running) {
                break;
            }

            refExportedPath.updateAndGet(p -> p.resolve(rec.getName()));

            if (rec.isFolder()) {
                exportRecursiveFolder(rec, subDir, refExportedPath);
            } else {
                var path = subDir.resolve(rec.getName());
                try (var out = new FileOutputStream(path.toFile())) {
                    var exportedPath = refExportedPath.get();
                    System.out.printf("export %s...\n", exportedPath);
                    final var ref = new AtomicReference<>(exportedPath);
                    Platform.runLater(() -> labelCurrentExport.setText(ref.get().toString()));
                    manager.readContent(rec.getId(), out);
                }
                refExportedPath.updateAndGet(p -> p.getParent() == null ? EMPTY_PATH : p.getParent());
            }
        }
        refExportedPath.updateAndGet(p -> p.getParent() == null ? EMPTY_PATH : p.getParent());
    }
}

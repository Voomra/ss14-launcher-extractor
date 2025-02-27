package ru.di9.ss14.extractor.gui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.*;
import lombok.Setter;
import ru.di9.ss14.extractor.ContentDbManager;
import ru.di9.ss14.extractor.ContentRec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class MainController implements Initializable {
    private static final int STATE_NOT_LOADED = 0;
    private static final int STATE_LOAD_IN_PROGRESS = 1;
    private static final int STATE_LOADED = 2;

    @FXML
    public TreeView<String> treeView;

    private final Map<TreeItem<String>, Integer> mapForkLoaded = new HashMap<>();

    @Setter
    private Stage stage;
    private TreeItem<String> rootItem;
    private ContentDbManager manager;
    private File lastSaveDir;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        rootItem = new TreeItem<>("(root)");
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(ignore -> new TreeCellExt<>());
    }

    public void onClickOpenMenu() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Открыть content.db");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("content.db", "content.db"));
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        manager = new ContentDbManager(file.getAbsolutePath());

        rootItem.getChildren().clear();
        mapForkLoaded.clear();

        manager.getForkVersions().forEach(forkVersion -> {
            var forkVersionItem = new TreeItemExt<>("\uD83D\uDCE6 " + forkVersion);
            forkVersionItem.getChildren().add(new TreeItem<>("(загрузка...)"));
            forkVersionItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue || mapForkLoaded.get(forkVersionItem) != STATE_NOT_LOADED) {
                    return;
                }

                mapForkLoaded.put(forkVersionItem, STATE_LOAD_IN_PROGRESS);

                var task = new Task<ContentRec>() {
                    @Override
                    protected ContentRec call() {
                        return manager.getPaths(forkVersion);
                    }
                };

                task.setOnSucceeded(event -> {
                    forkVersionItem.setExpanded(false);
                    forkVersionItem.getChildren().clear();

                    var rootRec = (ContentRec) event.getSource().getValue();
                    SortedSet<ContentRec> paths = rootRec.getChildren();
                    createTreeItems(forkVersionItem, paths);

                    mapForkLoaded.put(forkVersionItem, STATE_LOADED);
                    forkVersionItem.setExpanded(true);
                    forkVersionItem.setContextMenuBuilder(() -> createFolderContextMenu(rootRec, "\uD83D\uDCBE Сохранить в..."));
                });

                var thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            });

            mapForkLoaded.put(forkVersionItem, STATE_NOT_LOADED);
            rootItem.getChildren().add(forkVersionItem);
        });
    }

    private void createTreeItems(TreeItem<String> parentItem, SortedSet<ContentRec> sortedSet) {
        sortedSet.forEach(contentRec -> {
            var fileItem = new TreeItemExt<>(
                (contentRec.isFolder()
                    ? "\uD83D\uDCC2 "
                    : "\uD83D\uDCC4 ")
                + contentRec.getName());
            fileItem.setContentRec(contentRec);
            parentItem.getChildren().add(fileItem);

            if (contentRec.isFolder()) {
                createTreeItems(fileItem, contentRec.getChildren());
                fileItem.setContextMenuBuilder(() -> createFolderContextMenu(contentRec));
            } else {
                fileItem.setContextMenuBuilder(() -> createFileContextMenu(contentRec));
            }
        });
    }

    private ContextMenu createFileContextMenu(ContentRec contentRec) {
        var menuItem = new MenuItem("\uD83D\uDCBE Сохранить файл '%s' в...".formatted(contentRec.getName()));
        menuItem.setOnAction(event -> {
            var fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить " + contentRec.getName());
            fileChooser.setInitialFileName(contentRec.getName());
            if (lastSaveDir != null) {
                fileChooser.setInitialDirectory(lastSaveDir);
            }
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*"));
            File file = fileChooser.showSaveDialog(stage);

            if (file == null) {
                return;
            }

            try (var out = new FileOutputStream(file)) {
                manager.readContent(contentRec.getId(), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            lastSaveDir = file.getParentFile();
        });

        var contextMenu = new ContextMenu();
        contextMenu.getItems().add(menuItem);
        return contextMenu;
    }

    private ContextMenu createFolderContextMenu(ContentRec contentRec) {
        return createFolderContextMenu(contentRec, "\uD83D\uDCBE Сохранить папку '%s' в...".formatted(contentRec.getName()));
    }

    private ContextMenu createFolderContextMenu(ContentRec contentRec, String title) {
        var menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> {
            var dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Сохранить " + contentRec.getName());
            if (lastSaveDir != null) {
                dirChooser.setInitialDirectory(lastSaveDir);
            }
            File dir = dirChooser.showDialog(stage);

            if (dir == null) {
                return;
            }

            try {
                showExportWindow(contentRec, dir.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            lastSaveDir = dir;
        });

        var contextMenu = new ContextMenu();
        contextMenu.getItems().add(menuItem);
        return contextMenu;
    }

    private void showExportWindow(ContentRec contentRec, Path currentDir) throws IOException {
        var loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/view/export-dialog.fxml")));
        Parent root = loader.load();

        var exportStage = new Stage();
        exportStage.setScene(new Scene(root));
        exportStage.setTitle("Статус сохранения");
        exportStage.initModality(Modality.APPLICATION_MODAL);
        exportStage.initOwner(stage);

        var controller = (ExportInfoController)loader.getController();
        controller.setStage(exportStage);
        controller.setManager(manager);

        exportStage.setOnShowing(event -> {
            stage.getScene().getRoot().setDisable(true);
            controller.startExport(contentRec, currentDir);
        });
        exportStage.setOnHiding(event -> {
            controller.stopExport();
            stage.getScene().getRoot().setDisable(false);
        });
        exportStage.showAndWait();
    }
}

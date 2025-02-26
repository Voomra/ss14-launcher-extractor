package ru.di9.ss14.extractor.gui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import ru.di9.ss14.extractor.ContentDbManager;
import ru.di9.ss14.extractor.ContentRec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedSet;

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

        manager = new ContentDbManager(file.getAbsolutePath());

        rootItem.getChildren().clear();
        mapForkLoaded.clear();

        manager.getForkVersions().forEach(forkVersion -> {
            var forkVersionItem = new TreeItem<>("\uD83D\uDCE6 " + forkVersion);
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

                    SortedSet<ContentRec> paths = ((ContentRec) event.getSource().getValue()).getChildren();
                    createTreeItems(forkVersionItem, paths);

                    mapForkLoaded.put(forkVersionItem, STATE_LOADED);
                    forkVersionItem.setExpanded(true);
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
            } else {
                fileItem.setContextMenuBuilder(() -> createContextMenu(contentRec));
            }
        });
    }

    private ContextMenu createContextMenu(ContentRec contentRec) {
        var menuItem = new MenuItem("\uD83D\uDCBE Сохранить %s в...".formatted(contentRec.getName()));
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
}

package ru.di9.ss14.extractor.gui;

import javafx.scene.control.TreeCell;

public class TreeCellExt<T> extends TreeCell<T> {

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setText(getItem() == null ? "" : getItem().toString());

            var treeItem = getTreeItem();
            setGraphic(treeItem.getGraphic());
            if (treeItem instanceof TreeItemExt<T> treeItemExt) {
                setContextMenu(treeItemExt.getContextMenu());
            }
        }
    }
}

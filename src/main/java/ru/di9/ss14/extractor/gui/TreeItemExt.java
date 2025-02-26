package ru.di9.ss14.extractor.gui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;
import ru.di9.ss14.extractor.ContentRec;

import java.util.function.Supplier;

public class TreeItemExt<T> extends TreeItem<T> {
    @Getter
    @Setter
    private ContentRec contentRec;

    private ContextMenu contextMenu;

    @Setter
    private Supplier<ContextMenu> contextMenuBuilder;

    public TreeItemExt(T t) {
        super(t);
    }

    public ContextMenu getContextMenu() {
        if (contextMenu == null && contextMenuBuilder != null) {
            contextMenu = contextMenuBuilder.get();
        }

        return contextMenu;
    }
}

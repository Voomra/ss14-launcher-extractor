package ru.di9.ss14.extractor;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.SortedSet;
import java.util.TreeSet;

@Getter
public final class ContentRec implements Comparable<ContentRec> {
    private final String name;
    private final Integer id;
    private final Boolean compressed;
    private SortedSet<ContentRec> children;

    public ContentRec(String folderName) {
        this(folderName, null, null);
    }

    public ContentRec(String fileName, Integer id, Boolean compressed) {
        this.name = fileName;
        this.id = id;
        this.compressed = compressed;

        if (isFolder()) {
            this.children = new TreeSet<>();
        }
    }

    public boolean isFolder() {
        return id == null;
    }

    @Override
    public int compareTo(@NotNull ContentRec rec) {
        if (this.isFolder() == rec.isFolder()) {
            return this.getName().compareTo(rec.getName());
        } else {
            return this.isFolder() ? -1 : 1;
        }
    }
}

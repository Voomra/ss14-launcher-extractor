package ru.di9.ss14.extractor;

import com.github.luben.zstd.ZstdInputStream;
import org.sqlite.SQLiteDataSource;
import ru.di9.jdbc.JdbcTemplate;
import ru.di9.jdbc.JdbcTemplateImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ContentDbManager {
    private final JdbcTemplate jdbcTemplate;

    public ContentDbManager(String pathToDb) {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:%s".formatted(pathToDb));

        this.jdbcTemplate = new JdbcTemplateImpl(dataSource);
    }

    public List<String> getForkVersions() {
        return jdbcTemplate.queryList(
            "SELECT ForkVersion FROM ContentVersion",
            (rs, rowNum) -> rs.getString(1).toUpperCase());
    }

    public ContentRec getPaths(String forkVersion) {
        ContentRec root = new ContentRec(forkVersion);

        jdbcTemplate.query("""
                    select cm.ContentId, cm.Path, c.Compression
                    from ContentManifest cm
                    inner join ContentVersion cv on cv.Id = cm.VersionId
                    inner join Content c on c.Id = cm.ContentId
                    where cv.ForkVersion like ?
                    order by cm.Path;
                """,
            ps -> ps.setString(1, forkVersion),
            rs -> {
                while (rs.next()) {
                    int contentId = rs.getInt(1);
                    String path = rs.getString(2);
                    boolean compressed = rs.getInt(3) > 0;

                    parseLine(contentId, path, compressed, root);
                }
                return null;
            });

        return root;
    }

    public void readContent(int contentId, OutputStream outputStream) {
        jdbcTemplate.query("""
                select Data, Compression
                from Content
                where Id = ?;
                """,
            ps -> ps.setInt(1, contentId),
            rs -> {
                rs.next();

                boolean compressed = rs.getInt(2) > 0;

                try(InputStream binaryStream = rs.getBinaryStream(1)) {
                    if (compressed) {
                        var zstdInputStream = new ZstdInputStream(binaryStream);
                        zstdInputStream.transferTo(outputStream);
                    } else {
                        binaryStream.transferTo(outputStream);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return null;
            });
    }

    private void parseLine(int contentId, String path, boolean compressed, ContentRec contentRec) {
        var idx = path.indexOf('/');
        if (idx == -1) {
            contentRec.getChildren().add(new ContentRec(path, contentId, compressed));
        } else {
            String folder = path.substring(0, idx);
            var rec = contentRec.getChildren()
                .stream()
                .filter(r -> r.getName().equals(folder))
                .findFirst()
                .orElseGet(() -> {
                    var r = new ContentRec(folder);
                    contentRec.getChildren().add(r);
                    return r;
                });
            parseLine(contentId, path.substring(idx + 1), compressed, rec);
        }
    }
}

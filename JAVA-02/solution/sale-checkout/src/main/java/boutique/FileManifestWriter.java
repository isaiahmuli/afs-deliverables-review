package boutique;

import java.io.IOException;
import java.nio.file.Path;

/** The warehouse manifest: one {@code id,productCode,quantity,unitPriceCents} line per order. */
public class FileManifestWriter extends AsyncFileWriter<Order> implements ManifestWriter {
    public FileManifestWriter(Path path) throws IOException {
        super(path, CsvRecord::toCsvLine);
    }
}

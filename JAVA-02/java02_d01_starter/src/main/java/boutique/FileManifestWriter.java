package boutique;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileManifestWriter implements ManifestWriter {
    private final BufferedWriter writer;

    public FileManifestWriter(Path path) throws IOException {
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void append(Order order) throws IOException {
        writer.write(order.id());
        writer.write(",");
        writer.write(order.productCode());
        writer.write(",");
        writer.write(Integer.toString(order.quantity()));
        writer.write(",");
        writer.write(Long.toString(order.unitPriceCents()));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

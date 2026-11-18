package boutique;

import java.io.IOException;

public interface ManifestWriter extends AutoCloseable {
    void append(Order order) throws IOException;

    @Override
    default void close() throws IOException { }
}

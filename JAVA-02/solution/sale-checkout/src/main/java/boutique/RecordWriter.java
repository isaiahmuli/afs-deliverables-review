package boutique;

import java.io.IOException;

/** Accepts records of one type. Closing it must leave no accepted record unwritten. */
public interface RecordWriter<T> extends AutoCloseable {
    void append(T record) throws IOException;

    @Override
    default void close() throws IOException { }
}

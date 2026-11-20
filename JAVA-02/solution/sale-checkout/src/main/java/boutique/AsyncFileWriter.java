package boutique;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Appends one line per record to a file, in the background.
 *
 * <p>{@link #append} only queues the record and returns, so callers never wait for the
 * disk. One writer thread does every write, so lines never run together. {@link #close}
 * stops taking records, waits until every queued line is written, then closes the file.
 *
 * <p>The formatter is a {@code Function<? super T, String>}: one formatter written for a
 * supertype (for example {@code CsvRecord::toCsvLine}) serves writers of any subtype. The
 * manifest ({@code Order}) and the audit log ({@code AuditEvent}) both rely on that.
 */
public class AsyncFileWriter<T> implements RecordWriter<T> {
    private static final long CLOSE_TIMEOUT_SECONDS = 30;

    private final BufferedWriter writer;
    private final Function<? super T, String> formatter;
    private final ExecutorService executor;
    private final AtomicReference<IOException> failure = new AtomicReference<>();

    public AsyncFileWriter(Path path, Function<? super T, String> formatter) throws IOException {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "writer-" + path.getFileName()));
    }

    /**
     * Queues the record and returns without waiting for the write.
     *
     * @throws IOException if an earlier write failed
     * @throws IllegalStateException if the writer is closed
     */
    @Override
    public void append(T record) throws IOException {
        Objects.requireNonNull(record, "record");
        throwIfFailed();
        try {
            executor.execute(() -> write(record));
        } catch (RejectedExecutionException closed) {
            throw new IllegalStateException("Writer is closed", closed);
        }
    }

    private void write(T record) {
        try {
            String line = formatter.apply(record);
            if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
                throw new IOException("A record must format as one line: " + line);
            }
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException | RuntimeException error) {
            failure.compareAndSet(null, error instanceof IOException io ? io : new IOException(error));
        }
    }

    /** Waits for every queued line to be written, then closes the file. */
    @Override
    public void close() throws IOException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                throw new IOException("Timed out writing pending lines");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while writing pending lines");
        } finally {
            writer.close();
        }
        throwIfFailed();
    }

    private void throwIfFailed() throws IOException {
        IOException error = failure.get();
        if (error != null) {
            throw error;
        }
    }
}

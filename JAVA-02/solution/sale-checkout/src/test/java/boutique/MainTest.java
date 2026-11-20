package boutique;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir Path directory;

    @Test
    void theSaleSimulationSellsExactlySixtyAndWritesBothFiles() throws Exception {
        var output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[] {directory.toString()});
        } finally {
            System.setOut(original);
        }
        assertThat(output.toString(StandardCharsets.UTF_8)).contains(
                "Reservations accepted: 60", "Jackets left: 0", "Checkout errors: 0",
                "Manifest lines after close: 60", "Audit lines after close: 61");
        try (Stream<Path> files = Files.walk(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .contains("manifest.csv", "audit.csv");
        }
    }
}

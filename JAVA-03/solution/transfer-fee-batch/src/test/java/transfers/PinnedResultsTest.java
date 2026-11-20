package transfers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the results that already work. Runs the batch through Main on the supplied input and
 * checks that every row in the supplied fees.csv (the original output) is produced unchanged.
 * Written before any change, and kept passing through every fix and refactor.
 */
class PinnedResultsTest {
    @TempDir Path directory;

    @Test
    void everyOriginallyPricedRowIsUnchanged() throws Exception {
        Path output = directory.resolve("fees.csv");
        Main.main(new String[] {"transfers.csv", "bank-rates.csv", output.toString()});

        Map<String, String> original = rowsById(Path.of("fees.csv"));
        Map<String, String> now = rowsById(output);

        assertEquals(176, original.size(), "rows in the supplied fees.csv");
        assertEquals("id,type,amount_usd,fee_usd,recipient_amount,recipient_currency",
                Files.readAllLines(output).getFirst());
        for (var row : original.entrySet()) {
            assertTrue(now.containsKey(row.getKey()), "missing row " + row.getKey());
            assertEquals(row.getValue(), now.get(row.getKey()), "changed row " + row.getKey());
        }
    }

    private static Map<String, String> rowsById(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            rows.put(line.substring(0, line.indexOf(',')), line);
        }
        return rows;
    }
}

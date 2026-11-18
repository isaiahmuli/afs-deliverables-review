package transfers;

import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 3) {
            throw new IllegalArgumentException("Supply no arguments, or: transfers.csv bank-rates.csv output.csv");
        }
        Path input = Path.of(args.length == 0 ? "transfers.csv" : args[0]);
        Path rates = Path.of(args.length == 0 ? "bank-rates.csv" : args[1]);
        Path output = Path.of(args.length == 0 ? "build/output/fees.csv" : args[2]);
        new TransferFeeBatch(rates).run(input, output);
    }
}

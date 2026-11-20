# Transfer fee batch

The project targets Java 25 and the wrapper runs Gradle 9.2.0. Gradle needs
Java 17 or newer to start; if JDK 25 is not installed, the first build
downloads it. JUnit 5 and ArchUnit 1 are on the
test classpath; SLF4J 2 and Logback 1.5 are configured for logging. The first
build needs internet access.

```bash
java -version
./gradlew clean build
./gradlew run
```

Use `gradlew.bat` on Windows. `BUILD SUCCESSFUL` means the starter compiled;
it does not mean its results are correct.

## What you are running

Main passes three file paths to TransferFeeBatch. The batch reads 200 sample
transfers, splits them into four groups, and runs those groups on four threads.
Each successful transfer waits for a local SMS simulation, which pauses for
5 milliseconds and returns a message ID. It never contacts a real service.

The real workload is about 40,000 transfers. This sample keeps investigation
quick while preserving the parallel execution. One fresh application process
represents one night's batch; exit and run again for another night.

The `price` method's switch handles `friend`, `bank`, and `international`.
Any other type, such as `intl`, is refused and logged with its row ID.

## Files and output

| File | Meaning |
|---|---|
| transfers.csv | 200 input rows, including transfers to Mexico and unsupported types. |
| bank-rates.csv | The bank's full table for this sample night. Each rate is recipient currency units per US dollar. |
| fees.csv | Observed output from the original starter, sorted by transfer ID. This is a record of current behavior, not the expected result after fixes. |
| build/output/fees.csv | Output from your latest run. This path keeps the supplied fees.csv unchanged. |
| src/main/java/transfers/Main.java | Entry point and optional file arguments. |
| src/main/java/transfers/TransferFeeBatch.java | Existing batch, pricing, rates, receipts, and review flags. |
| src/main/resources/logback.xml | Console logging; INFO by default. |

The input columns are `id,type,sender,amount_usd,recipient_currency`.
The files use plain comma-separated fields without quoting or embedded commas.
All amounts sent are US dollars. Friend and bank transfers pay out in US dollars.
International transfers pay out in the stated recipient currency.

The output columns are `id,type,amount_usd,fee_usd,recipient_amount,recipient_currency`.
The sender pays the fee separately: conversion uses the full transfer amount.
At the supplied MXN rate, USD 200 delivers MXN 3450.00, with a USD 4.00 fee.
Money is rounded half up to two decimal places.

## Reading a run

The log includes refused row IDs and types, exceptions, bank fetches, and final
counts. Review flags currently appear as `REVIEW <id> USD <amount>` on stdout.
Rows that fail do not appear in the generated fee file; later rows still run.
The final counts include both unsupported rows and failed rows as refused.

Mexico transfers currently fail with IllegalStateException; the full stack
trace is in the log. The final counts line shows how many times the bank's
rates were fetched in that run.

To inspect simulated receipt contents, use DEBUG logging:

```bash
LOG_LEVEL=DEBUG ./gradlew run
```

On Windows PowerShell, set `$env:LOG_LEVEL='DEBUG'` before running the wrapper.
The starter has no receipt-off setting. Turning logging off does not stop the
SMS simulation.

You can supply another input, rates file, and output path together:

```bash
./gradlew run --args="transfers.csv bank-rates.csv build/check/fees.csv"
```

## Tests

There is no `src/test/java` folder yet. `./gradlew test` runs whatever you add
there.

The task, rules, and what to submit are in the deliverable.

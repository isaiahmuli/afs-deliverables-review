# Root cause: transfers to Mexico fail with IllegalStateException

**Status:** fixed. **Reproducing test:** `src/test/java/transfers/MexicoTransferTest.java`.

## Symptom

Every international transfer to Mexico (20 of the 200 sample rows) was missing from
`fees.csv` and logged as an error. Other currencies priced normally.

## Stack trace (original code, from the reproducing test)

```text
java.lang.IllegalStateException: Could not price transfer T0005
    at transfers.TransferFeeBatch.price(TransferFeeBatch.java:92)
    at transfers.MexicoTransferTest.twoHundredDollarsToMexico(MexicoTransferTest.java:17)
Caused by: java.lang.NullPointerException: Cannot read field "scale" because "multiplicand" is null
    at java.base/java.math.BigDecimal.multiply(BigDecimal.java:1602)
    at transfers.TransferFeeBatch.convert(TransferFeeBatch.java:97)
    at transfers.TransferFeeBatch.price(TransferFeeBatch.java:88)
```

The `IllegalStateException` is only a wrapper: `price` catches every `RuntimeException` (line 91)
and rethrows it. The real failure is the `Caused by` exception.

- **First project stack frame of the cause:** `transfers.TransferFeeBatch.convert(TransferFeeBatch.java:97)`,
  `return amount.multiply(rate);`. The `rate` it was given is `null`.
- `rate` came from `RateTable.rate("MXN")`, which is `rates.get(currency)`. A `HashMap` returns
  `null` for a missing key.

## Cause

The rates file has an `MXN,17.25` row, but the loader never stored it.

**Defect line:** `TransferFeeBatch.java:130`, in the `RateTable` constructor:

```java
if (Set.of("EUR", "GBP", "CAD", "INR").contains(columns[0])) {
    rates.put(columns[0], new BigDecimal(columns[1]));
}
```

The loader only keeps four hard-coded currencies. When the bank added Mexican pesos to its
file, the code silently skipped them. Nothing failed until a transfer asked for that rate, and
then the `null` surfaced far away, as a `NullPointerException` inside `BigDecimal`.

## Fix

- **Load every currency in the bank's file.** The fixed loader is in
  `rates/BankRates.java`, method `fetch`.
- **Fail clearly on a missing rate.** `BankRates.unitsPerUsd` now throws
  `UnknownCurrencyException("No bank rate for currency: JPY")` instead of returning `null`, so
  the next missing rate names itself.
- **One-line fix first, alone.** The fix was made and verified before any restructuring:
  `evidence/01_before_fix_tests.txt` shows the reproducing test failing, and
  `evidence/02_after_fix_tests.txt` shows it passing. The pinning test passed both times.

## Result

A $200 transfer to Mexico now costs $4.00, and the recipient receives MXN 3450.00
(200 × 17.25). All 20 Mexico rows appear in `evidence/sample-output/fees.csv`. Every row of the
original `fees.csv` is unchanged (`PinnedResultsTest`).

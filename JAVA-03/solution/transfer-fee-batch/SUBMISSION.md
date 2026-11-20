# Transfer fee batch: submission notes

This is the starter project, repaired and separated. `README.md` is the starter's own README,
left unchanged.

- **Root-cause note:** `docs/root-cause-mexico.md`
- **ADR:** `docs/adr/0001-fee-rules-as-strategies.md`

## Run it

```bash
./gradlew clean build                 # 63 tests
./gradlew run                         # one night: build/output/fees.csv
RECEIPTS=off ./gradlew run            # receipts off: no texts; pricing and review flags unchanged
```

`RECEIPTS` can be `on` (the default when it isn't set) or `off`; anything else stops the run
with an error. You can also pass it as `-Dreceipts=off`. Bill payments use the input type `bill`.

## Order of work

| Step | Evidence |
|---|---|
| 1. Pinned the working results on the **original** code (`PinnedResultsTest`: every row of the supplied `fees.csv` is reproduced unchanged). Wrote the Mexico reproducing test. | `evidence/01_before_fix_tests.txt`: pinning PASSED, Mexico FAILED with the production stack trace |
| 2. One-line root-cause fix, nothing else | `evidence/02_after_fix_tests.txt`: both PASSED |
| 3. Restructured into packages. The pinning test was not changed. | `evidence/03_after_refactor_tests.txt`: both PASSED |
| 4. Added the bill payment rule, thread-safe rates Singleton, observers, receipt-off factory, review flags through SLF4J, and the ArchUnit rules | `evidence/08_clean_build_all_tests.txt`: 63 PASSED from a fresh copy |
| Negative check: the ArchUnit and Singleton tests fail when the rule is broken | `evidence/04_negative_checks.txt` |

## Results from the repaired batch

| Run | Read | Priced | Refused | Rate fetches | SMS receipts | Review flags |
|---|---|---|---|---|---|---|
| Original starter | 200 | 176 | 24 (4 `intl` + 20 Mexico errors) | **4** | 176 | 40 (stdout) |
| Repaired, receipts on (`evidence/05_run_receipts_on.log`) | 200 | 196 | 4 (`intl` only) | **1** | 196 | 40 (log) |
| Repaired, `RECEIPTS=off` (`evidence/06_run_receipts_off.log`) | 200 | 196 | 4 | 1 | **0** | 40 |

- **Output files:**
  - `evidence/sample-output/fees.csv` is the original 176 rows unchanged, plus the 20 Mexico
    rows, for example `T0005,international,200.00,4.00,3450.00,MXN`.
  - The receipts-off run writes an identical file.
- **Bill payments:** the supplied input has no bill payments, so `samples/transfers-with-bills.csv`
  adds some. Its output is `evidence/sample-output/fees-with-bills.csv`:

  ```text
  B0001,bill,120.00,0.25,120.00,USD
  B0002,bill,5200.00,0.25,5200.00,USD      (also flagged: REVIEW B0002 USD 5200.00)
  B0004,international,200.00,4.00,3450.00,MXN
  ```

## Packages

```text
transfers            Main: wiring only (composition root)
transfers.batch      file reading/writing, the 4 parallel groups          -> core, events
transfers.core       Transfer, Quote, FeeRule, FeeCalculator, RateSource   -> nothing else in the project
transfers.core.rules FriendTransferFee, BankTransferFee, BillPaymentFee, InternationalTransferFee
transfers.rates      BankRates: thread-safe Singleton, fetched once        -> core
transfers.events     PricingEvents (subject), PricedTransferListener       -> core
transfers.receipts   SmsReceiptListener, SmsService, ReceiptListenerFactory -> core, events
transfers.review     ReviewFlagListener (> $5,000)                         -> core, events
```

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 SOLID | Each class has one job. `TransferFeeBatch`, `FeeCalculator`, and `SmsReceiptListener` take their dependencies (`FeeCalculator`, `PricingEvents`, `RateSource`, `SmsService`) through constructors. `PinnedResultsTest` passed before the fix, after the fix, and after the restructure (`evidence/01`–`03`). |
| D1-2 Creational | **Factory:** `ReceiptListenerFactory.create` returns the SMS listener for `on`, none for `off`, and rejects anything else (`ReceiptListenerFactoryTest`). **Singleton:** `BankRates`, using double-checked locking on a volatile field. `BankRatesSingletonTest` runs 32 threads at once, 20 times, and asserts one instance and one fetch. |
| D1-3 Behavioural | **Strategy:** `FeeCalculator` holds `FeeRule`s by composition. `FeeCalculatorTest.theContextGainsANewStrategyWithNoChange` adds a `wallet` rule with no code change. **Observer:** `PricingEventsTest` shows subscribed listeners are notified and unsubscribed ones are not. |
| D1-4 Modules | `ArchitectureTest`: package slices are free of cycles, and `transfers.core..` depends only on itself and `java..`. Also: no class uses `System.out` or `System.err`. |
| D1-5 ADR | `docs/adr/0001-fee-rules-as-strategies.md`: status, context, three options, decision, and consequences, including when strategies would be wrong. |
| D1-6 Root cause | `docs/root-cause-mexico.md`: the first project frame of the cause (`convert`, line 97), the cause (MXN never loaded), and the defect line (130). `MexicoTransferTest` failed before the fix and passes after. |

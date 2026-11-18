# ADR 0001: Fee rules as strategies in a separate core package

**Status:** Accepted, 2026-11-18

## Context

- **One class did everything.** `TransferFeeBatch` read the file, chose fees in a `switch`,
  refused unknown types, sent SMS receipts, flagged large transfers, and wrote results. Adding
  bill payments meant editing that `switch`.
- **The website will reuse the fee rules.** It must not pull in batch, file, or receipt code.
- **More transfer types are coming.** The rule is: add a new type without editing the existing
  fee rules.
- **Receipts and review flags are side effects of pricing.** Support needs to switch receipts
  off without affecting pricing or review flags.

## Options considered

1. **Keep the `switch` and add a `bill` case.**
   - Smallest change.
   - But every new type edits the same method, and the fee rules stay tied to the batch class,
     which the website would have to take with them.
2. **An enum of transfer types, each with its fee.**
   - Compact, and the compiler sees every type.
   - But adding a type still edits the enum, which is the existing rules' file. A type defined
     by another team or module can't be added.
3. **One `FeeRule` class per type (Strategy), held by a `FeeCalculator` (context) in a `core`
   package that depends on nothing else in the project.** Receipts and review flags subscribe to
   a `PricingEvents` subject (Observer).
   - More classes.
   - New types are new classes, and side effects are pluggable.

## Decision

Option 3.

- **Strategies and context:** `transfers.core` holds `FeeRule`, the four rules, and
  `FeeCalculator`. The calculator maps type to rule, by composition, from the rules it is given,
  and never names a type itself.
- **Wiring:** `Main` lists the rules. Adding bill payments was one new class
  (`BillPaymentFee`) plus one entry in that list.
- **Observers:** receipts (`transfers.receipts`) and review flags (`transfers.review`)
  implement `PricedTransferListener`. The receipt-off control is a factory that simply returns
  no receipt listener.
- **Rates:** they come in through the core's own `RateSource` interface. The bank-backed,
  fetch-once Singleton (`transfers.rates.BankRates`) is plugged in at startup.
- **Enforcement:** an ArchUnit test checks that packages have no cycles and that `core` depends
  only on itself and `java..`.

## Consequences

- **Good:**
  - A new transfer type doesn't touch `FeeCalculator` or any existing rule (tested with a
    `wallet` rule).
  - The website can depend on `transfers.core` alone.
  - Receipts can be removed without touching pricing.
- **Cost:**
  - More small classes, and the list of active rules lives in `Main`. Forgetting to add a new
    rule there means its type is refused.
  - That shows up as "Refused transfer … unsupported type" in the log, not a compile error.
- **Pricing changes are a new class, not a config change.** Changing a fee such as the bank fee
  still edits that one rule's class, which is intended.
- **When this pattern would be wrong:**
  - **Data, not behaviour.** If fees became pure data that operations staff change often (a
    price table per country), strategies would be the wrong tool. A data-driven table loaded
    from configuration would let staff change prices without a release.
  - **A closed, small set of types.** If the types were fixed and few, an enum or a `switch`
    over a sealed type would be simpler, and would give a compile-time check that every type is
    priced.
  - **Fees that depend on each other.** If a fee depended on other transfers (for example,
    volume discounts across a sender's night), a per-transfer strategy would be the wrong shape.

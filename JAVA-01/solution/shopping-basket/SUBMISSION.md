# Shopping basket: submission notes

This is the starter project after restructuring, with the new discount and the pickup option
added. `README.md` is the starter's own README, left unchanged.

## Run it

```bash
./gradlew clean build   # 35 tests: the 9 supplied regression tests + 26 new ones
./gradlew run           # sample receipt
```

## Evidence

| What | File |
|---|---|
| Compiler rejects pricing that skips a delivery option | `evidence/01_missing_pickup_case_compile_error.txt` |
| Supplied tests: only the two `setDelivery` calls changed; every expected amount is the same | `evidence/02_supplied_tests_diff.txt` |
| Clean build from a fresh copy (no `build/` or `.gradle/`) | `evidence/03_clean_build.txt` |
| Sample receipt | `evidence/04_sample_receipt.txt` |

Sample receipt after the changes. The shirt is now one line of $50.00, and the total is still
$66.20:

```text
BOUTIQUE RECEIPT
AB-12 | Shirt | quantity 2 | $50.00
cap-7 | Cap | quantity 1 | $18.00
Items: $68.00
After discount: $61.20
Delivery: $5.00
Total: $66.20
```

Compiler output after `Pickup` was added to the sealed `Delivery` type, before the pricing
switch was updated:

```text
> Task :compileJava FAILED
src/main/java/boutique/ShoppingBasket.java:71: error: the switch expression does not cover all possible input values
        return switch (delivery) {
               ^
1 error
BUILD FAILED in 593ms
```

## New discount: spend threshold ("spend X, save Y")

`SpendThresholdDiscount(thresholdCents, amountOffCents)`: when the items subtotal reaches the
threshold, a fixed amount comes off the items. Below the threshold, nothing comes off. Like the
other discounts, it applies before delivery.

Examples with `new SpendThresholdDiscount(5000, 1000)`, which is $10.00 off when the items
reach $50.00:

| Items | After discount | Why |
|---|---|---|
| $68.00 | $58.00 | Above the threshold |
| $50.00 | $40.00 | Exactly the threshold counts |
| $49.99 | $49.99 | Below the threshold: no discount |
| $0.00 | $0.00 | Empty basket |

The constructor rejects a threshold of zero or less, an amount off of zero or less, and an amount
off larger than the threshold, so the items can never go below zero. It was added as one new
class; `ShoppingBasket` and the existing discounts were not changed.

## Pickup

`Delivery.PICKUP` costs **$0.00**: the shopper collects the order, so the checkout total equals
the discounted items. For example, a $25.00 shirt with pickup totals $25.00.

## How each exit criterion is met

| ID | Where |
|---|---|
| D1-1 Encapsulation | Every field is private (`basketStateIsPrivate`). Each rejected input has an `assertThrows` test (`RejectedInput`). |
| D1-2 Polymorphism | The `Discount` interface has three implementations, each overriding `apply`. The basket calls `discount.apply(...)` with no `instanceof` check. |
| D1-3 Modern Java | Records: `Product`, `BasketLine`, the discounts, and the delivery options. `Delivery` is a sealed interface. `deliveryCents()` uses a pattern switch with no `default`. |
| D1-4 Collections | Catalog: `HashMap` for keyed lookup. Lines: `LinkedHashMap` for keyed merge plus first-added order. `lines()`: an unmodifiable `List` snapshot. |
| D1-5 equals/hashCode | `ProductCode` is a plain class, with `equals` and `hashCode` on the normalized code. A test shows `ab-12` and `AB-12 ` act as one set element and one map key. |
| D1-6 Exceptions | Checked: `UnknownProductException` (an unknown code is recoverable). Unchecked: `IllegalArgumentException` for a bad quantity, price, code, or discount, which are caller errors. Both are tested, and the clean build passes. |

## Design notes

- `Discount` is deliberately **not** sealed. A new discount must be addable without touching
  existing code, while a new delivery option *must* force every pricing switch to be updated.
- A merged line shows the catalog's code (`AB-12`), not whichever spelling the shopper typed
  first.
- `ProductCode.equals` uses `instanceof`. That's the standard way to write `equals`, not a chain
  that picks behaviour.

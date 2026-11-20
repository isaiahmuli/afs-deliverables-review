# Shopping basket starter

## Run it

Install JDK 25. The Gradle wrapper is included, so you do not need Gradle
installed. From this folder:

```bash
java -version          # should report 25
./gradlew test         # runs the 9 supplied tests; all pass
./gradlew run          # prints the sample receipt below
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

`./gradlew run` prints:

```text
BOUTIQUE RECEIPT
AB-12 | Shirt | quantity 1 | $25.00
ab-12 | Shirt | quantity 1 | $25.00
cap-7 | Cap | quantity 1 | $18.00
Items: $68.00
After discount: $61.20
Delivery: $5.00
Total: $66.20
```

Main.java adds the shirt once as `AB-12` and once as `ab-12`, which is why it
appears twice. Run this command again after each change to see the effect.

## Find your way around the code

All code is in `src/main/java/boutique/`.

- **ShoppingBasket.java** holds everything the basket does.
  - `add` looks the code up in the catalog, ignoring case and spaces, then
    always appends a new line with the code exactly as typed.
  - `lines` returns the basket's own list, so a caller can change it.
  - `discountedSubtotalCents` picks the discount's behavior with `instanceof`
    checks, one per discount kind.
  - `deliveryCents` compares the delivery `String` with `"STANDARD"` and
    `"EXPRESS"` and returns 0 for anything else.
- **Discount.java** declares the two discount kinds as nested classes with
  public fields and no behavior.
- **Product.java** and **BasketLine.java** hold data in public fields, with an
  accessor method for each.
- **Main.java** builds the sample basket and prints the receipt.

The checkout rules the tests rely on: standard delivery costs 500 cents and
express 1,200. Discounts apply to the items before delivery is added. A
percentage discount rounds to the nearest cent, half up, so 10% off 5 cents
removes 1 cent. A fixed discount never takes the items below zero.

## The supplied tests

`src/test/java/boutique/ShoppingBasketTest.java` uses only the basket's public
methods and the accessors (`code()`, `quantity()`, and so on), never fields.
Every test already declares `throws Exception`.

The expected amounts in these tests are the store's current prices; keep them.
If a type you introduce changes how a test chooses delivery or builds a
discount, update that call in the test.

## What to do

The task, rules, and what to submit are in the deliverable.

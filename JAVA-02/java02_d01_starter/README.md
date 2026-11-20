# Sale checkout starter

## Run it

Install JDK 25. The Gradle wrapper is included, so you do not need Gradle
installed. JUnit 5, Mockito 5, and AssertJ 3 are already set up. From this
folder:

```bash
java -version          # should report 25
./gradlew test         # runs the 6 single-threaded tests; all pass
./gradlew run          # simulates the sale
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

`./gradlew run` starts 200 shoppers at the same moment against 60 jackets and
prints something like this. The numbers change from run to run:

```text
Jackets available: 60
Shoppers: 200
Reservations accepted: 95
Jackets left: 0
Checkout errors: 0
Manifest lines after close: 200
Manifest: /…/build/sale-output/sale-123…/manifest.csv
```

Each run writes a new folder under `build/sale-output`. Open that run's
`manifest.csv`. A correct line looks like `order-1,JACKET,1,4999`: order ID,
product code, quantity, and unit price in cents. `Main` also accepts an output
folder as its first argument.

## Find your way around the code

All code is in `src/main/java/boutique/`.

- **Checkout.java** holds everything checkout does.
  - `checkout` looks up the product, reads its stock, builds an `Order`,
    reserves when the quantity fits, saves the new stock, then appends the
    order to the writer whether or not it was reserved. It returns only after
    the write.
  - `findProduct` loops through the products and returns `null` when no code
    matches.
  - `changePrice` sets the new price. Nothing records the change.
  - `lowStock` is a plain loop that keeps products with fewer than 5 units, in
    catalog order.
- **ManifestWriter.java** is the interface checkout writes through. The tests
  replace it with a Mockito mock.
- **FileManifestWriter.java** turns an `Order` into one line, writing each
  field with a separate call, then flushes.
- **Order.java** and **Product.java** hold the data. A product's stock and
  price can change.
- **Main.java** starts the shopper threads, releases them together from a
  latch, and prints the results.

## The supplied tests

`CheckoutTest.java` has the 6 single-threaded tests. They use Mockito and
AssertJ, and the file test writes into a JUnit temporary folder.

`ConcurrentSaleTest.java` is disabled. It releases 200 shoppers together and
repeats the sale 25 times, because a race does not show on every run. It
checks for 60 reservations, zero stock left, and that exactly the accepted
orders reach the writer. Remove `@Disabled`, then run:

```bash
./gradlew test --tests boutique.ConcurrentSaleTest
```

It fails against this starter.

## Coverage

JaCoCo is not set up. Versions 0.8.14 and newer support Java 25.

## What to do

The task, rules, and what to submit are in the deliverable.

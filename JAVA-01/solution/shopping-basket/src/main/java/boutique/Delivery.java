package boutique;

/** The delivery options a shopper can choose. Sealed: the compiler knows every option. */
public sealed interface Delivery {
    Delivery STANDARD = new Standard();
    Delivery EXPRESS = new Express();
    Delivery PICKUP = new Pickup();

    record Standard() implements Delivery {}

    record Express() implements Delivery {}

    /** The shopper collects the order from the store. */
    record Pickup() implements Delivery {}
}

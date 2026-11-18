package boutique;

public abstract class Discount {
    public static class Percentage extends Discount {
        public int percent;

        public Percentage(int percent) {
            this.percent = percent;
        }
    }

    public static class FixedAmount extends Discount {
        public long cents;

        public FixedAmount(long cents) {
            this.cents = cents;
        }
    }
}

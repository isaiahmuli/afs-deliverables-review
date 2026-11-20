package boutique;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Checkout implements AutoCloseable {
    private final List<Product> products;
    private final ManifestWriter manifest;

    public Checkout(List<Product> products, ManifestWriter manifest) {
        this.products = new ArrayList<>(products);
        this.manifest = manifest;
    }

    public Product findProduct(String code) {
        for (Product product : products) {
            if (product.code().equals(code)) return product;
        }
        return null;
    }

    public boolean checkout(String orderId, String code, int quantity) throws IOException {
        Product product = findProduct(code);
        int available = product.stock();
        Order order = new Order(orderId, product.code(), quantity, product.priceCents());
        boolean reserved = quantity <= available;
        if (reserved) product.setStock(available - quantity);
        manifest.append(order);
        return reserved;
    }

    public void changePrice(String code, long priceCents) {
        findProduct(code).setPriceCents(priceCents);
    }

    public List<Product> lowStock() {
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            if (product.stock() < 5) result.add(product);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        manifest.close();
    }
}

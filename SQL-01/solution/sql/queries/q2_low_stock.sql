-- Q2: Product options low on stock (3 or fewer units), lowest first. Expected: expected/q2_low_stock.csv
SELECT p.name AS product,
       s.code AS size,
       c.name AS colour,
       pv.stock_quantity
FROM product_variant pv
JOIN product p ON p.product_id = pv.product_id
JOIN size    s ON s.size_id    = pv.size_id
JOIN colour  c ON c.colour_id  = pv.colour_id
WHERE pv.stock_quantity <= 3
ORDER BY pv.stock_quantity, p.name, s.sort_order, c.name;

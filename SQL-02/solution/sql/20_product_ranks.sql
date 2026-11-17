-- Planning query 1: rank each category's products by units sold.
-- Contract (starter README): category_id, product_id, units_sold, sales_rank.
--   * units count only in PAID or SHIPPED orders;
--   * every product is kept, including products that never sold (units_sold = 0);
--   * rank within category, largest first; ties share a rank; no gap after a tie
--     -> dense_rank(), not rank();
--   * sorted by category_id, product_id.
-- Uses: a CTE, an OUTER join (products LEFT JOIN sales), and a window function.
WITH sales AS (
    SELECT i.product_id, sum(i.quantity) AS units_sold
    FROM order_items i
    JOIN orders o ON o.order_id = i.order_id
    WHERE o.status IN ('PAID', 'SHIPPED')
    GROUP BY i.product_id
)
SELECT p.category_id,
       p.product_id,
       coalesce(s.units_sold, 0) AS units_sold,
       dense_rank() OVER (PARTITION BY p.category_id
                          ORDER BY coalesce(s.units_sold, 0) DESC) AS sales_rank
FROM products p
LEFT JOIN sales s ON s.product_id = p.product_id
ORDER BY p.category_id, p.product_id

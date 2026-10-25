-- Format: "-- ID | category | note", then the statement, ending with ";" on its last line.
-- Do not put ";" anywhere else inside a statement.

-- Q01 | scan | primary-key style lookup, should be an index scan
SELECT * FROM lineitem WHERE l_orderkey = 1;

-- Q02 | scan | selective date filter on the largest table
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1998-09-01';

-- Q03 | scan | filter keeps almost every row, seq scan is the correct plan
SELECT count(*) FROM lineitem WHERE l_shipdate <= DATE '1998-09-02';

-- Q04 | scan | two range predicates, moderately selective
SELECT l_orderkey, l_extendedprice FROM lineitem
WHERE l_discount BETWEEN 0.05 AND 0.07 AND l_quantity < 24;

-- Q05 | scan | equality on a date column
SELECT * FROM orders WHERE o_orderdate = DATE '1995-03-15';

-- Q06 | scan | low-cardinality filter, about 20 percent of rows
SELECT * FROM customer WHERE c_mktsegment = 'BUILDING';

-- Q07 | pattern | leading wildcard LIKE, cannot use a normal btree index
SELECT * FROM part WHERE p_name LIKE '%green%';

-- Q08 | pattern | function on column, index on l_shipdate cannot be used
SELECT * FROM lineitem WHERE extract(year FROM l_shipdate) = 1995;

-- Q09 | pattern | upper() on column, rewrite or expression index candidate
SELECT * FROM customer WHERE upper(c_name) = 'CUSTOMER#000000001';

-- Q10 | pattern | cast on column
SELECT * FROM orders WHERE o_orderkey::text = '1';

-- Q11 | pattern | prefix LIKE
SELECT * FROM part WHERE p_name LIKE 'forest%';

-- Q12 | pattern | ILIKE with wildcards on a text column
SELECT * FROM lineitem WHERE l_comment ILIKE '%special%requests%';

-- Q13 | pattern | OR across different columns
SELECT * FROM orders WHERE o_custkey = 100 OR o_totalprice > 500000;

-- Q14 | aggregate | pricing summary, reads almost the whole table
SELECT l_returnflag, l_linestatus, sum(l_quantity) AS sum_qty,
       sum(l_extendedprice) AS sum_price, avg(l_discount) AS avg_disc, count(*) AS cnt
FROM lineitem
WHERE l_shipdate <= DATE '1998-09-02'
GROUP BY l_returnflag, l_linestatus
ORDER BY l_returnflag, l_linestatus;

-- Q15 | aggregate | group by on a three month window
SELECT o_orderpriority, count(*) AS order_count
FROM orders
WHERE o_orderdate >= DATE '1995-01-01' AND o_orderdate < DATE '1995-04-01'
GROUP BY o_orderpriority
ORDER BY o_orderpriority;

-- Q16 | aggregate | top-N after aggregation
SELECT l_suppkey, sum(l_extendedprice * (1 - l_discount)) AS revenue
FROM lineitem
WHERE l_shipdate >= DATE '1996-01-01' AND l_shipdate < DATE '1996-04-01'
GROUP BY l_suppkey
ORDER BY revenue DESC
LIMIT 10;

-- Q17 | aggregate | count distinct on a high-cardinality column
SELECT count(DISTINCT l_orderkey) FROM lineitem;

-- Q18 | aggregate | group by with HAVING on a large group count
SELECT l_partkey, avg(l_quantity) AS avg_qty
FROM lineitem
GROUP BY l_partkey
HAVING avg(l_quantity) > 30;

-- Q19 | aggregate | small table aggregate
SELECT c_nationkey, avg(c_acctbal) AS avg_balance FROM customer GROUP BY c_nationkey;

-- Q20 | join | two tables with filters on both sides
SELECT c.c_name, o.o_orderkey, o.o_totalprice
FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey
WHERE c.c_mktsegment = 'BUILDING' AND o.o_orderdate < DATE '1995-03-15';

-- Q21 | join | three-table join, shipping priority style
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority
FROM customer, orders, lineitem
WHERE c_mktsegment = 'BUILDING' AND c_custkey = o_custkey AND l_orderkey = o_orderkey
  AND o_orderdate < DATE '1995-03-15' AND l_shipdate > DATE '1995-03-15'
GROUP BY l_orderkey, o_orderdate, o_shippriority
ORDER BY revenue DESC, o_orderdate
LIMIT 10;

-- Q22 | join | six-table join, join order matters
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue
FROM customer, orders, lineitem, supplier, nation, region
WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey
  AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey
  AND r_name = 'ASIA' AND o_orderdate >= DATE '1994-01-01' AND o_orderdate < DATE '1995-01-01'
GROUP BY n_name
ORDER BY revenue DESC;

-- Q23 | join | part, partsupp, supplier
SELECT s_name, p_name, ps_supplycost
FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey
WHERE p_size = 15 AND p_type LIKE '%BRASS';

-- Q24 | join | join with ORDER BY and LIMIT, top-N over a big join
SELECT o_orderkey, l_linenumber
FROM orders JOIN lineitem ON o_orderkey = l_orderkey
ORDER BY o_totalprice DESC
LIMIT 20;

-- Q25 | join | full join of the two largest tables, no filter
SELECT count(*) FROM lineitem l JOIN orders o ON l.l_orderkey = o.o_orderkey;

-- Q26 | join | small tables, nested loop is fine
SELECT s_name, n_name FROM supplier JOIN nation ON s_nationkey = n_nationkey WHERE n_name = 'GERMANY';

-- Q27 | join | self join on lineitem, large intermediate result
SELECT a.l_orderkey
FROM lineitem a JOIN lineitem b ON a.l_orderkey = b.l_orderkey AND a.l_suppkey <> b.l_suppkey
WHERE a.l_shipdate > DATE '1998-08-01';

-- Q28 | subquery | IN subquery
SELECT o_orderkey FROM orders
WHERE o_custkey IN (SELECT c_custkey FROM customer WHERE c_acctbal > 9000);

-- Q29 | subquery | NOT IN, rewrite candidate (NOT EXISTS)
SELECT c_custkey FROM customer WHERE c_custkey NOT IN (SELECT o_custkey FROM orders);

-- Q30 | subquery | NOT EXISTS, same meaning as Q29
SELECT c_custkey FROM customer c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.o_custkey = c.c_custkey);

-- Q31 | subquery | EXISTS with filter, order priority checking style
SELECT o_orderpriority, count(*) AS order_count
FROM orders
WHERE o_orderdate >= DATE '1993-07-01' AND o_orderdate < DATE '1993-10-01'
  AND EXISTS (SELECT 1 FROM lineitem WHERE l_orderkey = o_orderkey AND l_commitdate < l_receiptdate)
GROUP BY o_orderpriority
ORDER BY o_orderpriority;

-- Q32 | subquery | correlated scalar subquery, very slow without an index on l_partkey
SELECT sum(l_extendedprice) / 7.0 AS avg_yearly
FROM lineitem, part
WHERE p_partkey = l_partkey AND p_brand = 'Brand#23' AND p_container = 'MED BOX'
  AND l_quantity < (SELECT 0.2 * avg(l_quantity) FROM lineitem WHERE l_partkey = p_partkey);

-- Q33 | subquery | correlated subquery on the same table
SELECT ps_partkey, ps_suppkey FROM partsupp
WHERE ps_supplycost > (SELECT avg(p2.ps_supplycost) FROM partsupp p2 WHERE p2.ps_partkey = partsupp.ps_partkey);

-- Q34 | subquery | CTE joined back to a table
WITH big_orders AS (
  SELECT o_custkey, sum(o_totalprice) AS total FROM orders GROUP BY o_custkey
)
SELECT c_name, total FROM customer JOIN big_orders ON c_custkey = o_custkey WHERE total > 400000;

-- Q35 | sort | top-N on the largest table
SELECT * FROM lineitem ORDER BY l_extendedprice DESC LIMIT 10;

-- Q36 | sort | full sort of a big table, may spill to disk
SELECT * FROM orders ORDER BY o_totalprice DESC;

-- Q37 | sort | DISTINCT on low-cardinality columns
SELECT DISTINCT l_shipmode, l_shipinstruct FROM lineitem;

-- Q38 | estimation | correlated column-to-column predicates
SELECT count(*) FROM lineitem WHERE l_shipdate < l_commitdate AND l_commitdate < l_receiptdate;

-- Q39 | estimation | correlated status and date
SELECT count(*) FROM orders WHERE o_orderstatus = 'F' AND o_orderdate < DATE '1995-01-01';

-- Q40 | estimation | correlated flags, planner assumes independence
SELECT count(*) FROM lineitem WHERE l_returnflag = 'R' AND l_linestatus = 'F';

-- Q41 | window | window function over a big table
SELECT o_custkey, o_orderkey,
       rank() OVER (PARTITION BY o_custkey ORDER BY o_totalprice DESC) AS rnk
FROM orders;

-- Q42 | aggregate | top customers by order count
SELECT o_custkey, count(*) AS c FROM orders GROUP BY o_custkey ORDER BY c DESC LIMIT 10;
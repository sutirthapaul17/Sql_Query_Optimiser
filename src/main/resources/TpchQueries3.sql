-- queries3: control and gap-filling queries, IDs start at Q301.
-- Categories: improvable (a better formulation, index or setting should win), optimal (baseline probably best),
-- equivalent (PostgreSQL normalizes both forms, expect the same plan). These are predictions; the measured results decide.

-- Q301 | improvable | pagination group H1: deep OFFSET (walks and discards 1M rows)
SELECT o_orderkey, o_totalprice FROM orders ORDER BY o_orderkey OFFSET 1000000 LIMIT 20;

-- Q302 | improvable | pagination group H1: keyset form (continues after the last key seen; same page only if keys are dense)
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderkey > 1000000 ORDER BY o_orderkey LIMIT 20;

-- Q303 | improvable | scalar subquery in SELECT list group H2: correlated form
SELECT c.c_custkey, (SELECT count(*) FROM orders o WHERE o.o_custkey = c.c_custkey) AS n FROM customer c WHERE c.c_acctbal > 9000;

-- Q304 | improvable | scalar subquery in SELECT list group H2: pre-aggregated LEFT JOIN form (which wins depends on indexes)
SELECT c.c_custkey, coalesce(o.n, 0) AS n FROM customer c LEFT JOIN (SELECT o_custkey, count(*) AS n FROM orders GROUP BY o_custkey) o ON o.o_custkey = c.c_custkey WHERE c.c_acctbal > 9000;

-- Q305 | improvable | aggregate placement group H3: join everything, aggregate last
SELECT c.c_nationkey, sum(l.l_extendedprice) AS rev FROM customer c JOIN orders o ON o.o_custkey = c.c_custkey JOIN lineitem l ON l.l_orderkey = o.o_orderkey GROUP BY c.c_nationkey;

-- Q306 | improvable | aggregate placement group H3: aggregate orders and lineitem first, then join
SELECT c.c_nationkey, sum(x.rev) AS rev FROM customer c JOIN (SELECT o.o_custkey, sum(l.l_extendedprice) AS rev FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey GROUP BY o.o_custkey) x ON x.o_custkey = c.c_custkey GROUP BY c.c_nationkey;

-- Q307 | improvable | existence test group H4: count(*) > 0 counts every match
SELECT c_custkey FROM customer c WHERE (SELECT count(*) FROM orders o WHERE o.o_custkey = c.c_custkey) > 0;

-- Q308 | improvable | existence test group H4: EXISTS stops at the first match
SELECT c_custkey FROM customer c WHERE EXISTS (SELECT 1 FROM orders o WHERE o.o_custkey = c.c_custkey);

-- Q309 | improvable | duplicate removal group H5: UNION sorts or hashes to remove duplicates that cannot exist (disjoint date ranges)
SELECT o_orderkey FROM orders WHERE o_orderdate < DATE '1993-01-01' UNION SELECT o_orderkey FROM orders WHERE o_orderdate >= DATE '1998-01-01';

-- Q310 | improvable | duplicate removal group H5: UNION ALL, same result because the ranges are disjoint
SELECT o_orderkey FROM orders WHERE o_orderdate < DATE '1993-01-01' UNION ALL SELECT o_orderkey FROM orders WHERE o_orderdate >= DATE '1998-01-01';

-- Q311 | improvable | redundant DISTINCT group H6: DISTINCT over a column set that includes the primary key
SELECT DISTINCT o_orderkey, o_custkey FROM orders WHERE o_orderdate >= DATE '1997-01-01';

-- Q312 | improvable | redundant DISTINCT group H6: same query without DISTINCT
SELECT o_orderkey, o_custkey FROM orders WHERE o_orderdate >= DATE '1997-01-01';

-- Q313 | improvable | top row per group H7: row_number() = 1 over a full sort
SELECT o_custkey, o_orderkey, o_totalprice FROM (SELECT o_custkey, o_orderkey, o_totalprice, row_number() OVER (PARTITION BY o_custkey ORDER BY o_totalprice DESC) AS rn FROM orders) t WHERE rn = 1;

-- Q314 | improvable | top row per group H7: DISTINCT ON (ties may pick a different row)
SELECT DISTINCT ON (o_custkey) o_custkey, o_orderkey, o_totalprice FROM orders ORDER BY o_custkey, o_totalprice DESC;

-- Q315 | improvable | top 3 orders per customer for a customer subset, LATERAL with LIMIT
SELECT c.c_custkey, o.o_orderkey FROM customer c CROSS JOIN LATERAL (SELECT o_orderkey FROM orders o WHERE o.o_custkey = c.c_custkey ORDER BY o.o_totalprice DESC LIMIT 3) o WHERE c.c_acctbal > 9500;

-- Q316 | improvable | orders with more than one supplier group H8: self-join with DISTINCT
SELECT DISTINCT a.l_orderkey FROM lineitem a JOIN lineitem b ON a.l_orderkey = b.l_orderkey AND a.l_suppkey <> b.l_suppkey;

-- Q317 | improvable | orders with more than one supplier group H8: single pass with GROUP BY and HAVING
SELECT l_orderkey FROM lineitem GROUP BY l_orderkey HAVING count(DISTINCT l_suppkey) > 1;

-- Q318 | improvable | nine relations group H9: written biggest table first; beyond join_collapse_limit the planner follows the written order
SELECT count(*) FROM lineitem l JOIN orders o ON l.l_orderkey = o.o_orderkey JOIN customer c ON c.c_custkey = o.o_custkey JOIN nation n1 ON n1.n_nationkey = c.c_nationkey JOIN region r ON r.r_regionkey = n1.n_regionkey JOIN supplier s ON s.s_suppkey = l.l_suppkey JOIN nation n2 ON n2.n_nationkey = s.s_nationkey JOIN part p ON p.p_partkey = l.l_partkey JOIN partsupp ps ON ps.ps_partkey = l.l_partkey AND ps.ps_suppkey = l.l_suppkey WHERE r.r_name = 'EUROPE' AND p.p_type LIKE '%BRASS';

-- Q319 | improvable | nine relations group H9: same joins written selective tables first (region, nation, part)
SELECT count(*) FROM region r JOIN nation n1 ON n1.n_regionkey = r.r_regionkey JOIN customer c ON c.c_nationkey = n1.n_nationkey JOIN orders o ON o.o_custkey = c.c_custkey JOIN lineitem l ON l.l_orderkey = o.o_orderkey JOIN part p ON p.p_partkey = l.l_partkey JOIN partsupp ps ON ps.ps_partkey = l.l_partkey AND ps.ps_suppkey = l.l_suppkey JOIN supplier s ON s.s_suppkey = l.l_suppkey JOIN nation n2 ON n2.n_nationkey = s.s_nationkey WHERE r.r_name = 'EUROPE' AND p.p_type LIKE '%BRASS';

-- Q320 | improvable | composite index candidate (l_suppkey, l_shipdate): equality plus range on two columns
SELECT count(*) FROM lineitem WHERE l_suppkey = 100 AND l_shipdate >= DATE '1997-01-01' AND l_shipdate < DATE '1997-07-01';

-- Q321 | improvable | composite index candidate (l_partkey, l_shipdate): filter plus ORDER BY LIMIT
SELECT l_orderkey, l_extendedprice FROM lineitem WHERE l_partkey = 5000 ORDER BY l_shipdate DESC LIMIT 5;

-- Q322 | improvable | covering index candidate: index-only scan possible when only indexed columns are read
SELECT l_partkey, l_suppkey FROM lineitem WHERE l_partkey BETWEEN 1000 AND 1100;

-- Q323 | improvable | big sort on two columns, may spill to disk (work_mem or index candidate)
SELECT * FROM lineitem ORDER BY l_shipdate, l_orderkey;

-- Q324 | improvable | join key wrapped in arithmetic group H10: expression on the join column blocks index use
SELECT count(*) FROM orders o JOIN lineitem l ON o.o_orderkey + 0 = l.l_orderkey WHERE o.o_orderdate >= DATE '1998-07-01';

-- Q325 | improvable | join key wrapped in arithmetic group H10: plain join condition
SELECT count(*) FROM orders o JOIN lineitem l ON o.o_orderkey = l.l_orderkey WHERE o.o_orderdate >= DATE '1998-07-01';

-- Q326 | improvable | pointless ORDER BY group H11: sorting a subquery that is only counted
SELECT count(*) FROM (SELECT * FROM orders ORDER BY o_totalprice) t;

-- Q327 | improvable | pointless ORDER BY group H11: no sort
SELECT count(*) FROM orders;

-- Q328 | improvable | maximum row group H12: scalar subquery reads lineitem twice
SELECT l_orderkey FROM lineitem WHERE l_extendedprice = (SELECT max(l_extendedprice) FROM lineitem);

-- Q329 | improvable | maximum row group H12: ORDER BY LIMIT 1 reads once (ties return one row)
SELECT l_orderkey FROM lineitem ORDER BY l_extendedprice DESC LIMIT 1;

-- Q330 | optimal | primary key lookup on orders; baseline is probably already best
SELECT * FROM orders WHERE o_orderkey = 12345;

-- Q331 | optimal | primary key lookup on customer; baseline is probably already best
SELECT * FROM customer WHERE c_custkey = 42;

-- Q332 | optimal | primary key lookup on part; baseline is probably already best
SELECT * FROM part WHERE p_partkey = 777;

-- Q333 | optimal | primary key lookup on supplier; baseline is probably already best
SELECT s_name FROM supplier WHERE s_suppkey = 77;

-- Q334 | optimal | composite primary key lookup on lineitem; baseline is probably already best
SELECT * FROM lineitem WHERE l_orderkey = 1 AND l_linenumber = 2;

-- Q335 | optimal | short IN list on the primary key; baseline is probably already best
SELECT * FROM orders WHERE o_orderkey IN (1, 2, 3, 4, 5);

-- Q336 | optimal | narrow primary key range; baseline is probably already best
SELECT * FROM orders WHERE o_orderkey BETWEEN 1 AND 1000;

-- Q337 | optimal | ordered by primary key with LIMIT; baseline is probably already best
SELECT * FROM orders ORDER BY o_orderkey LIMIT 10;

-- Q338 | optimal | LIMIT with no ORDER BY, stops after 10 rows; baseline is probably already best
SELECT * FROM lineitem LIMIT 10;

-- Q339 | optimal | count on a 5-row table; baseline is probably already best
SELECT count(*) FROM region;

-- Q340 | optimal | join of the two tiny dimension tables; baseline is probably already best
SELECT n_name, r_name FROM nation JOIN region ON n_regionkey = r_regionkey;

-- Q341 | optimal | group by on a 25-row table; baseline is probably already best
SELECT n_regionkey, count(*) FROM nation GROUP BY n_regionkey;

-- Q342 | optimal | whole-table aggregate, a full scan is unavoidable; baseline is probably already best
SELECT sum(l_quantity) FROM lineitem;

-- Q343 | optimal | whole-table average, a full scan is unavoidable; baseline is probably already best
SELECT avg(o_totalprice) FROM orders;

-- Q344 | optimal | max of an indexed primary key, answered from the index; baseline is probably already best
SELECT max(o_orderkey) FROM orders;

-- Q345 | optimal | filter keeps essentially every row, a seq scan is right; baseline is probably already best
SELECT count(*) FROM lineitem WHERE l_quantity > 0;

-- Q346 | optimal | filter keeps essentially every row on orders; baseline is probably already best
SELECT count(*) FROM orders WHERE o_totalprice > 0;

-- Q347 | optimal | primary key join with a primary key filter; baseline is probably already best
SELECT o.o_orderkey, c.c_name FROM orders o JOIN customer c ON c.c_custkey = o.o_custkey WHERE o.o_orderkey = 1;

-- Q348 | optimal | order lines of a single order; baseline is probably already best
SELECT l.l_linenumber, l.l_extendedprice FROM lineitem l JOIN orders o ON o.o_orderkey = l.l_orderkey WHERE o.o_orderkey = 100;

-- Q349 | optimal | count of the 800k-row partsupp table; baseline is probably already best
SELECT count(*) FROM partsupp;

-- Q350 | equivalent | same-plan group S1: IN list vs OR chain, form 1
SELECT count(*) FROM customer WHERE c_nationkey IN (1, 2, 3);

-- Q351 | equivalent | same-plan group S1: IN list vs OR chain, form 2
SELECT count(*) FROM customer WHERE c_nationkey = 1 OR c_nationkey = 2 OR c_nationkey = 3;

-- Q352 | equivalent | same-plan group S2: BETWEEN vs two comparisons, form 1
SELECT count(*) FROM orders WHERE o_totalprice BETWEEN 100000 AND 200000;

-- Q353 | equivalent | same-plan group S2: BETWEEN vs two comparisons, form 2
SELECT count(*) FROM orders WHERE o_totalprice >= 100000 AND o_totalprice <= 200000;

-- Q354 | equivalent | same-plan group S3: comma join vs explicit JOIN, form 1
SELECT c.c_name, o.o_orderkey FROM customer c, orders o WHERE c.c_custkey = o.o_custkey AND o.o_orderdate >= DATE '1998-06-01';

-- Q355 | equivalent | same-plan group S3: comma join vs explicit JOIN, form 2
SELECT c.c_name, o.o_orderkey FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE o.o_orderdate >= DATE '1998-06-01';

-- Q356 | equivalent | same-plan group S4: simple derived table vs inlined filters (subquery is pulled up), form 1
SELECT count(*) FROM (SELECT * FROM orders WHERE o_orderdate >= DATE '1997-01-01') t WHERE t.o_totalprice > 300000;

-- Q357 | equivalent | same-plan group S4: simple derived table vs inlined filters (subquery is pulled up), form 2
SELECT count(*) FROM orders WHERE o_orderdate >= DATE '1997-01-01' AND o_totalprice > 300000;

-- Q358 | equivalent | same-plan group S5: HAVING on a grouping column vs WHERE (filter is pushed down), form 1
SELECT l_shipmode, count(*) FROM lineitem GROUP BY l_shipmode HAVING l_shipmode = 'AIR';

-- Q359 | equivalent | same-plan group S5: HAVING on a grouping column vs WHERE (filter is pushed down), form 2
SELECT l_shipmode, count(*) FROM lineitem WHERE l_shipmode = 'AIR' GROUP BY l_shipmode;

-- Q360 | equivalent | same-plan group S6: count(1) vs count(*), form 1
SELECT count(1) FROM orders WHERE o_orderstatus = 'F';

-- Q361 | equivalent | same-plan group S6: count(1) vs count(*), form 2
SELECT count(*) FROM orders WHERE o_orderstatus = 'F';

-- Q362 | equivalent | same-plan group S7: NOT (a < b) vs a >= b, form 1
SELECT count(*) FROM lineitem WHERE NOT (l_quantity < 25);

-- Q363 | equivalent | same-plan group S7: NOT (a < b) vs a >= b, form 2
SELECT count(*) FROM lineitem WHERE l_quantity >= 25;

-- Q364 | equivalent | same-plan group S8: LEFT JOIN with a null-rejecting WHERE vs INNER JOIN (converted automatically), form 1
SELECT count(*) FROM orders o LEFT JOIN customer c ON c.c_custkey = o.o_custkey WHERE c.c_acctbal > 9000;

-- Q365 | equivalent | same-plan group S8: LEFT JOIN with a null-rejecting WHERE vs INNER JOIN (converted automatically), form 2
SELECT count(*) FROM orders o JOIN customer c ON c.c_custkey = o.o_custkey WHERE c.c_acctbal > 9000;

-- Q366 | equivalent | same-plan group S9: IN subquery vs EXISTS (both become a semi-join), form 1
SELECT count(*) FROM orders WHERE o_custkey IN (SELECT c_custkey FROM customer WHERE c_nationkey = 3);

-- Q367 | equivalent | same-plan group S9: IN subquery vs EXISTS (both become a semi-join), form 2
SELECT count(*) FROM orders o WHERE EXISTS (SELECT 1 FROM customer c WHERE c.c_custkey = o.o_custkey AND c.c_nationkey = 3);

-- Q368 | equivalent | same-plan group S10: = ANY(ARRAY) vs IN list, form 1
SELECT count(*) FROM part WHERE p_size = ANY (ARRAY[1, 5, 10]);

-- Q369 | equivalent | same-plan group S10: = ANY(ARRAY) vs IN list, form 2
SELECT count(*) FROM part WHERE p_size IN (1, 5, 10);

-- Q370 | equivalent | same-plan group S11: predicate order swapped, form 1
SELECT count(*) FROM lineitem WHERE l_shipmode = 'AIR' AND l_quantity < 10;

-- Q371 | equivalent | same-plan group S11: predicate order swapped, form 2
SELECT count(*) FROM lineitem WHERE l_quantity < 10 AND l_shipmode = 'AIR';

-- Q372 | equivalent | same-plan group S12: non-materialized CTE vs derived table (CTE is inlined), form 1
WITH t AS (SELECT * FROM orders WHERE o_orderdate >= DATE '1997-01-01') SELECT count(*) FROM t WHERE o_totalprice > 300000;

-- Q373 | equivalent | same-plan group S12: non-materialized CTE vs derived table (CTE is inlined), form 2
SELECT count(*) FROM (SELECT * FROM orders WHERE o_orderdate >= DATE '1997-01-01') t WHERE o_totalprice > 300000;

-- Q374 | equivalent | same-plan group S13: explicit ASC vs default order, form 1
SELECT o_orderkey FROM orders ORDER BY o_totalprice ASC LIMIT 10;

-- Q375 | equivalent | same-plan group S13: explicit ASC vs default order, form 2
SELECT o_orderkey FROM orders ORDER BY o_totalprice LIMIT 10;

-- Q376 | equivalent | same-plan group S14: constant folded on the right vs precomputed constant, form 1
SELECT count(*) FROM lineitem WHERE l_shipdate < DATE '1995-01-01' + 30;

-- Q377 | equivalent | same-plan group S14: constant folded on the right vs precomputed constant, form 2
SELECT count(*) FROM lineitem WHERE l_shipdate < DATE '1995-01-31';
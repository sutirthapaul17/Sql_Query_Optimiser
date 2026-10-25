-- Extra TPC-H queries, same format as the main file. IDs start at Q101.
-- Queries sharing a 'group' label in the note are equivalent formulations; compare them with each other.

-- Q101 | scan | selectivity sweep: l_shipdate window of 1 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1995-01-02';

-- Q102 | scan | selectivity sweep: l_shipdate window of 7 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1995-01-08';

-- Q103 | scan | selectivity sweep: l_shipdate window of 30 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1995-01-31';

-- Q104 | scan | selectivity sweep: l_shipdate window of 90 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1995-04-01';

-- Q105 | scan | selectivity sweep: l_shipdate window of 365 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01';

-- Q106 | scan | selectivity sweep: l_shipdate window of 730 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-12-31';

-- Q107 | scan | selectivity sweep: l_shipdate window of 1500 days
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1999-02-09';

-- Q108 | scan | selectivity sweep: l_quantity < 2
SELECT count(*) FROM lineitem WHERE l_quantity < 2;

-- Q109 | scan | selectivity sweep: l_quantity < 5
SELECT count(*) FROM lineitem WHERE l_quantity < 5;

-- Q110 | scan | selectivity sweep: l_quantity < 10
SELECT count(*) FROM lineitem WHERE l_quantity < 10;

-- Q111 | scan | selectivity sweep: l_quantity < 25
SELECT count(*) FROM lineitem WHERE l_quantity < 25;

-- Q112 | scan | selectivity sweep: l_quantity < 40
SELECT count(*) FROM lineitem WHERE l_quantity < 40;

-- Q113 | scan | selectivity sweep: l_quantity < 50
SELECT count(*) FROM lineitem WHERE l_quantity < 50;

-- Q114 | scan | selectivity sweep: o_orderdate window of 1 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-01-02';

-- Q115 | scan | selectivity sweep: o_orderdate window of 7 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-01-08';

-- Q116 | scan | selectivity sweep: o_orderdate window of 30 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-01-31';

-- Q117 | scan | selectivity sweep: o_orderdate window of 90 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-03-31';

-- Q118 | scan | selectivity sweep: o_orderdate window of 365 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-12-31';

-- Q119 | scan | selectivity sweep: o_orderdate window of 1000 days
SELECT o_orderkey, o_totalprice FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1998-09-27';

-- Q120 | scan | low-cardinality equality c_nationkey = 0
SELECT c_custkey, c_name FROM customer WHERE c_nationkey = 0;

-- Q121 | scan | low-cardinality equality c_nationkey = 7
SELECT c_custkey, c_name FROM customer WHERE c_nationkey = 7;

-- Q122 | scan | low-cardinality equality c_nationkey = 13
SELECT c_custkey, c_name FROM customer WHERE c_nationkey = 13;

-- Q123 | scan | low-cardinality equality c_nationkey = 24
SELECT c_custkey, c_name FROM customer WHERE c_nationkey = 24;

-- Q124 | scan | selectivity sweep: c_acctbal > -500
SELECT count(*) FROM customer WHERE c_acctbal > -500;

-- Q125 | scan | selectivity sweep: c_acctbal > 0
SELECT count(*) FROM customer WHERE c_acctbal > 0;

-- Q126 | scan | selectivity sweep: c_acctbal > 5000
SELECT count(*) FROM customer WHERE c_acctbal > 5000;

-- Q127 | scan | selectivity sweep: c_acctbal > 9000
SELECT count(*) FROM customer WHERE c_acctbal > 9000;

-- Q128 | scan | selectivity sweep: c_acctbal > 9900
SELECT count(*) FROM customer WHERE c_acctbal > 9900;

-- Q129 | join | customer-orders join, segment AUTOMOBILE, orders before 1993-12-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'AUTOMOBILE' AND o.o_orderdate < DATE '1993-12-15';

-- Q130 | join | customer-orders join, segment FURNITURE, orders before 1994-04-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'FURNITURE' AND o.o_orderdate < DATE '1994-04-15';

-- Q131 | join | customer-orders join, segment BUILDING, orders before 1993-11-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'BUILDING' AND o.o_orderdate < DATE '1993-11-15';

-- Q132 | join | customer-orders join, segment MACHINERY, orders before 1993-10-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'MACHINERY' AND o.o_orderdate < DATE '1993-10-15';

-- Q133 | join | customer-orders join, segment HOUSEHOLD, orders before 1993-01-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'HOUSEHOLD' AND o.o_orderdate < DATE '1993-01-15';

-- Q134 | join | customer-orders join, segment AUTOMOBILE, orders before 1994-04-15
SELECT c.c_name, o.o_orderkey, o.o_totalprice FROM customer c JOIN orders o ON c.c_custkey = o.o_custkey WHERE c.c_mktsegment = 'AUTOMOBILE' AND o.o_orderdate < DATE '1994-04-15';

-- Q135 | join | three-table shipping priority style, segment MACHINERY, date 1997-01-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'MACHINERY' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1997-01-15' AND l_shipdate > DATE '1997-01-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q136 | join | three-table shipping priority style, segment MACHINERY, date 1994-12-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'MACHINERY' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1994-12-15' AND l_shipdate > DATE '1994-12-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q137 | join | three-table shipping priority style, segment MACHINERY, date 1996-04-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'MACHINERY' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1996-04-15' AND l_shipdate > DATE '1996-04-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q138 | join | three-table shipping priority style, segment HOUSEHOLD, date 1997-05-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'HOUSEHOLD' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1997-05-15' AND l_shipdate > DATE '1997-05-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q139 | join | three-table shipping priority style, segment AUTOMOBILE, date 1994-12-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'AUTOMOBILE' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1994-12-15' AND l_shipdate > DATE '1994-12-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q140 | join | three-table shipping priority style, segment HOUSEHOLD, date 1995-05-15
SELECT l_orderkey, sum(l_extendedprice * (1 - l_discount)) AS revenue, o_orderdate, o_shippriority FROM customer, orders, lineitem WHERE c_mktsegment = 'HOUSEHOLD' AND c_custkey = o_custkey AND l_orderkey = o_orderkey AND o_orderdate < DATE '1995-05-15' AND l_shipdate > DATE '1995-05-15' GROUP BY l_orderkey, o_orderdate, o_shippriority ORDER BY revenue DESC, o_orderdate LIMIT 10;

-- Q141 | join | six-table join, region AFRICA, year 1994
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue FROM customer, orders, lineitem, supplier, nation, region WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey AND r_name = 'AFRICA' AND o_orderdate >= DATE '1994-01-01' AND o_orderdate < DATE '1995-01-01' GROUP BY n_name ORDER BY revenue DESC;

-- Q142 | join | six-table join, region AMERICA, year 1994
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue FROM customer, orders, lineitem, supplier, nation, region WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey AND r_name = 'AMERICA' AND o_orderdate >= DATE '1994-01-01' AND o_orderdate < DATE '1995-01-01' GROUP BY n_name ORDER BY revenue DESC;

-- Q143 | join | six-table join, region ASIA, year 1995
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue FROM customer, orders, lineitem, supplier, nation, region WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey AND r_name = 'ASIA' AND o_orderdate >= DATE '1995-01-01' AND o_orderdate < DATE '1996-01-01' GROUP BY n_name ORDER BY revenue DESC;

-- Q144 | join | six-table join, region EUROPE, year 1993
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue FROM customer, orders, lineitem, supplier, nation, region WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey AND r_name = 'EUROPE' AND o_orderdate >= DATE '1993-01-01' AND o_orderdate < DATE '1994-01-01' GROUP BY n_name ORDER BY revenue DESC;

-- Q145 | join | six-table join, region MIDDLE EAST, year 1993
SELECT n_name, sum(l_extendedprice * (1 - l_discount)) AS revenue FROM customer, orders, lineitem, supplier, nation, region WHERE c_custkey = o_custkey AND l_orderkey = o_orderkey AND l_suppkey = s_suppkey AND c_nationkey = s_nationkey AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey AND r_name = 'MIDDLE EAST' AND o_orderdate >= DATE '1993-01-01' AND o_orderdate < DATE '1994-01-01' GROUP BY n_name ORDER BY revenue DESC;

-- Q146 | subquery | EXISTS with date filter, quarter starting 1996-01-01
SELECT o_orderpriority, count(*) AS order_count FROM orders WHERE o_orderdate >= DATE '1996-01-01' AND o_orderdate < DATE '1996-04-02' AND EXISTS (SELECT 1 FROM lineitem WHERE l_orderkey = o_orderkey AND l_commitdate < l_receiptdate) GROUP BY o_orderpriority ORDER BY o_orderpriority;

-- Q147 | subquery | EXISTS with date filter, quarter starting 1995-07-01
SELECT o_orderpriority, count(*) AS order_count FROM orders WHERE o_orderdate >= DATE '1995-07-01' AND o_orderdate < DATE '1995-10-01' AND EXISTS (SELECT 1 FROM lineitem WHERE l_orderkey = o_orderkey AND l_commitdate < l_receiptdate) GROUP BY o_orderpriority ORDER BY o_orderpriority;

-- Q148 | subquery | EXISTS with date filter, quarter starting 1997-07-01
SELECT o_orderpriority, count(*) AS order_count FROM orders WHERE o_orderdate >= DATE '1997-07-01' AND o_orderdate < DATE '1997-10-01' AND EXISTS (SELECT 1 FROM lineitem WHERE l_orderkey = o_orderkey AND l_commitdate < l_receiptdate) GROUP BY o_orderpriority ORDER BY o_orderpriority;

-- Q149 | subquery | EXISTS with date filter, quarter starting 1993-10-01
SELECT o_orderpriority, count(*) AS order_count FROM orders WHERE o_orderdate >= DATE '1993-10-01' AND o_orderdate < DATE '1994-01-01' AND EXISTS (SELECT 1 FROM lineitem WHERE l_orderkey = o_orderkey AND l_commitdate < l_receiptdate) GROUP BY o_orderpriority ORDER BY o_orderpriority;

-- Q150 | subquery | anti-join group A1 (acctbal > 5000, no orders since 1998-01-01); same result in TPC-H because o_custkey is never null, NOT IN form
SELECT c_custkey FROM customer WHERE c_acctbal > 5000 AND c_custkey NOT IN (SELECT o_custkey FROM orders WHERE o_orderdate >= DATE '1998-01-01');

-- Q151 | subquery | anti-join group A1 (acctbal > 5000, no orders since 1998-01-01); same result in TPC-H because o_custkey is never null, NOT EXISTS form
SELECT c_custkey FROM customer c WHERE c.c_acctbal > 5000 AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.o_custkey = c.c_custkey AND o.o_orderdate >= DATE '1998-01-01');

-- Q152 | subquery | anti-join group A1 (acctbal > 5000, no orders since 1998-01-01); same result in TPC-H because o_custkey is never null, LEFT JOIN IS NULL form
SELECT c.c_custkey FROM customer c LEFT JOIN orders o ON o.o_custkey = c.c_custkey AND o.o_orderdate >= DATE '1998-01-01' WHERE c.c_acctbal > 5000 AND o.o_orderkey IS NULL;

-- Q153 | subquery | anti-join group A2 (acctbal > 9000, no orders since 1997-01-01); same result in TPC-H because o_custkey is never null, NOT IN form
SELECT c_custkey FROM customer WHERE c_acctbal > 9000 AND c_custkey NOT IN (SELECT o_custkey FROM orders WHERE o_orderdate >= DATE '1997-01-01');

-- Q154 | subquery | anti-join group A2 (acctbal > 9000, no orders since 1997-01-01); same result in TPC-H because o_custkey is never null, NOT EXISTS form
SELECT c_custkey FROM customer c WHERE c.c_acctbal > 9000 AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.o_custkey = c.c_custkey AND o.o_orderdate >= DATE '1997-01-01');

-- Q155 | subquery | anti-join group A2 (acctbal > 9000, no orders since 1997-01-01); same result in TPC-H because o_custkey is never null, LEFT JOIN IS NULL form
SELECT c.c_custkey FROM customer c LEFT JOIN orders o ON o.o_custkey = c.c_custkey AND o.o_orderdate >= DATE '1997-01-01' WHERE c.c_acctbal > 9000 AND o.o_orderkey IS NULL;

-- Q156 | subquery | semi-join group B1 (brand Brand#11); three equivalent formulations, IN form
SELECT count(*) FROM lineitem WHERE l_partkey IN (SELECT p_partkey FROM part WHERE p_brand = 'Brand#11');

-- Q157 | subquery | semi-join group B1 (brand Brand#11); three equivalent formulations, JOIN form
SELECT count(*) FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey WHERE p.p_brand = 'Brand#11';

-- Q158 | subquery | semi-join group B1 (brand Brand#11); three equivalent formulations, EXISTS form
SELECT count(*) FROM lineitem l WHERE EXISTS (SELECT 1 FROM part p WHERE p.p_partkey = l.l_partkey AND p.p_brand = 'Brand#11');

-- Q159 | subquery | semi-join group B2 (brand Brand#23); three equivalent formulations, IN form
SELECT count(*) FROM lineitem WHERE l_partkey IN (SELECT p_partkey FROM part WHERE p_brand = 'Brand#23');

-- Q160 | subquery | semi-join group B2 (brand Brand#23); three equivalent formulations, JOIN form
SELECT count(*) FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey WHERE p.p_brand = 'Brand#23';

-- Q161 | subquery | semi-join group B2 (brand Brand#23); three equivalent formulations, EXISTS form
SELECT count(*) FROM lineitem l WHERE EXISTS (SELECT 1 FROM part p WHERE p.p_partkey = l.l_partkey AND p.p_brand = 'Brand#23');

-- Q162 | subquery | semi-join group B3 (brand Brand#45); three equivalent formulations, IN form
SELECT count(*) FROM lineitem WHERE l_partkey IN (SELECT p_partkey FROM part WHERE p_brand = 'Brand#45');

-- Q163 | subquery | semi-join group B3 (brand Brand#45); three equivalent formulations, JOIN form
SELECT count(*) FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey WHERE p.p_brand = 'Brand#45';

-- Q164 | subquery | semi-join group B3 (brand Brand#45); three equivalent formulations, EXISTS form
SELECT count(*) FROM lineitem l WHERE EXISTS (SELECT 1 FROM part p WHERE p.p_partkey = l.l_partkey AND p.p_brand = 'Brand#45');

-- Q165 | subquery | decorrelation group C1 (Brand#23, MED BOX), correlated scalar subquery (can be extremely slow)
SELECT sum(l_extendedprice) / 7.0 AS avg_yearly FROM lineitem, part WHERE p_partkey = l_partkey AND p_brand = 'Brand#23' AND p_container = 'MED BOX' AND l_quantity < (SELECT 0.2 * avg(l_quantity) FROM lineitem WHERE l_partkey = p_partkey);

-- Q166 | subquery | decorrelation group C1 (Brand#23, MED BOX), rewritten with a grouped join
SELECT sum(l.l_extendedprice) / 7.0 AS avg_yearly FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey JOIN (SELECT l_partkey, 0.2 * avg(l_quantity) AS thr FROM lineitem GROUP BY l_partkey) a ON a.l_partkey = l.l_partkey WHERE p.p_brand = 'Brand#23' AND p.p_container = 'MED BOX' AND l.l_quantity < a.thr;

-- Q167 | subquery | decorrelation group C2 (Brand#12, SM CASE), correlated scalar subquery (can be extremely slow)
SELECT sum(l_extendedprice) / 7.0 AS avg_yearly FROM lineitem, part WHERE p_partkey = l_partkey AND p_brand = 'Brand#12' AND p_container = 'SM CASE' AND l_quantity < (SELECT 0.2 * avg(l_quantity) FROM lineitem WHERE l_partkey = p_partkey);

-- Q168 | subquery | decorrelation group C2 (Brand#12, SM CASE), rewritten with a grouped join
SELECT sum(l.l_extendedprice) / 7.0 AS avg_yearly FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey JOIN (SELECT l_partkey, 0.2 * avg(l_quantity) AS thr FROM lineitem GROUP BY l_partkey) a ON a.l_partkey = l.l_partkey WHERE p.p_brand = 'Brand#12' AND p.p_container = 'SM CASE' AND l.l_quantity < a.thr;

-- Q169 | subquery | decorrelation group C3 (Brand#34, LG PACK), correlated scalar subquery (can be extremely slow)
SELECT sum(l_extendedprice) / 7.0 AS avg_yearly FROM lineitem, part WHERE p_partkey = l_partkey AND p_brand = 'Brand#34' AND p_container = 'LG PACK' AND l_quantity < (SELECT 0.2 * avg(l_quantity) FROM lineitem WHERE l_partkey = p_partkey);

-- Q170 | subquery | decorrelation group C3 (Brand#34, LG PACK), rewritten with a grouped join
SELECT sum(l.l_extendedprice) / 7.0 AS avg_yearly FROM lineitem l JOIN part p ON p.p_partkey = l.l_partkey JOIN (SELECT l_partkey, 0.2 * avg(l_quantity) AS thr FROM lineitem GROUP BY l_partkey) a ON a.l_partkey = l.l_partkey WHERE p.p_brand = 'Brand#34' AND p.p_container = 'LG PACK' AND l.l_quantity < a.thr;

-- Q171 | aggregate | group by cardinality sweep on l_shipmode
SELECT l_shipmode AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY l_shipmode;

-- Q172 | aggregate | group by cardinality sweep on l_returnflag
SELECT l_returnflag AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY l_returnflag;

-- Q173 | aggregate | group by cardinality sweep on l_shipinstruct
SELECT l_shipinstruct AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY l_shipinstruct;

-- Q174 | aggregate | group by cardinality sweep on extract(year FROM l_shipdate)
SELECT extract(year FROM l_shipdate) AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY extract(year FROM l_shipdate);

-- Q175 | aggregate | group by cardinality sweep on l_suppkey
SELECT l_suppkey AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY l_suppkey;

-- Q176 | aggregate | group by cardinality sweep on l_partkey
SELECT l_partkey AS g, count(*) AS cnt, sum(l_extendedprice) AS total FROM lineitem WHERE l_shipdate >= DATE '1995-01-01' AND l_shipdate < DATE '1996-01-01' GROUP BY l_partkey;

-- Q177 | sort | top-N sweep: LIMIT 1
SELECT l_orderkey, l_extendedprice FROM lineitem ORDER BY l_extendedprice DESC LIMIT 1;

-- Q178 | sort | top-N sweep: LIMIT 10
SELECT l_orderkey, l_extendedprice FROM lineitem ORDER BY l_extendedprice DESC LIMIT 10;

-- Q179 | sort | top-N sweep: LIMIT 100
SELECT l_orderkey, l_extendedprice FROM lineitem ORDER BY l_extendedprice DESC LIMIT 100;

-- Q180 | sort | top-N sweep: LIMIT 10000
SELECT l_orderkey, l_extendedprice FROM lineitem ORDER BY l_extendedprice DESC LIMIT 10000;

-- Q181 | sort | top-N sweep: LIMIT 1000000
SELECT l_orderkey, l_extendedprice FROM lineitem ORDER BY l_extendedprice DESC LIMIT 1000000;

-- Q182 | pattern | sargability group D1 (year 1994), function on column
SELECT count(*) FROM lineitem WHERE extract(year FROM l_shipdate) = 1994;

-- Q183 | pattern | sargability group D1 (year 1994), plain range
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1994-01-01' AND l_shipdate < DATE '1995-01-01';

-- Q184 | pattern | sargability group D2 (year 1996), function on column
SELECT count(*) FROM lineitem WHERE extract(year FROM l_shipdate) = 1996;

-- Q185 | pattern | sargability group D2 (year 1996), plain range
SELECT count(*) FROM lineitem WHERE l_shipdate >= DATE '1996-01-01' AND l_shipdate < DATE '1997-01-01';

-- Q186 | pattern | sargability group D3 (March 1995), date_trunc on column
SELECT count(*) FROM orders WHERE date_trunc('month', o_orderdate) = DATE '1995-03-01';

-- Q187 | pattern | sargability group D3 (March 1995), plain range
SELECT count(*) FROM orders WHERE o_orderdate >= DATE '1995-03-01' AND o_orderdate < DATE '1995-04-01';

-- Q188 | pattern | substring on column inside IN list
SELECT count(*) FROM customer WHERE substring(c_phone FROM 1 FOR 2) IN ('13','31','23','29','30','18','17');

-- Q189 | pattern | arithmetic between two columns in the filter
SELECT count(*) FROM lineitem WHERE l_receiptdate - l_shipdate > 20;

-- Q190 | pattern | LIKE 'forest%' on p_name
SELECT p_partkey, p_name FROM part WHERE p_name LIKE 'forest%';

-- Q191 | pattern | LIKE '%forest' on p_name
SELECT p_partkey, p_name FROM part WHERE p_name LIKE '%forest';

-- Q192 | pattern | LIKE '%forest%' on p_name
SELECT p_partkey, p_name FROM part WHERE p_name LIKE '%forest%';

-- Q193 | pattern | LIKE 'PROMO%' on p_type
SELECT p_partkey, p_type FROM part WHERE p_type LIKE 'PROMO%';

-- Q194 | pattern | LIKE '%BRASS' on p_type
SELECT p_partkey, p_type FROM part WHERE p_type LIKE '%BRASS';

-- Q195 | estimation | correlated flags returnflag=R, linestatus=F
SELECT count(*) FROM lineitem WHERE l_returnflag = 'R' AND l_linestatus = 'F';

-- Q196 | estimation | correlated flags returnflag=A, linestatus=F
SELECT count(*) FROM lineitem WHERE l_returnflag = 'A' AND l_linestatus = 'F';

-- Q197 | estimation | correlated flags returnflag=N, linestatus=O
SELECT count(*) FROM lineitem WHERE l_returnflag = 'N' AND l_linestatus = 'O';

-- Q198 | estimation | correlated flags returnflag=N, linestatus=F
SELECT count(*) FROM lineitem WHERE l_returnflag = 'N' AND l_linestatus = 'F';

-- Q199 | estimation | correlated status and date (F before 1995)
SELECT count(*) FROM orders WHERE o_orderstatus = 'F' AND o_orderdate < DATE '1995-01-01';

-- Q200 | estimation | correlated status and date (O in 1998)
SELECT count(*) FROM orders WHERE o_orderstatus = 'O' AND o_orderdate >= DATE '1998-01-01';

-- Q201 | join | part-partsupp-supplier, size 1, type %BRASS
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 1 AND p_type LIKE '%BRASS';

-- Q202 | join | part-partsupp-supplier, size 15, type %COPPER
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 15 AND p_type LIKE '%COPPER';

-- Q203 | join | part-partsupp-supplier, size 30, type %NICKEL
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 30 AND p_type LIKE '%NICKEL';

-- Q204 | join | part-partsupp-supplier, size 45, type %STEEL
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 45 AND p_type LIKE '%STEEL';

-- Q205 | join | part-partsupp-supplier, size 50, type %TIN
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 50 AND p_type LIKE '%TIN';

-- Q206 | join | part-partsupp-supplier, size 7, type %BRASS
SELECT s_name, p_name, ps_supplycost FROM part JOIN partsupp ON p_partkey = ps_partkey JOIN supplier ON s_suppkey = ps_suppkey WHERE p_size = 7 AND p_type LIKE '%BRASS';

-- Q207 | subquery | CTE group E1 (total > 100000), plain CTE
WITH c AS (SELECT o_custkey, sum(o_totalprice) AS total FROM orders GROUP BY o_custkey) SELECT count(*) FROM c WHERE total > 100000;

-- Q208 | subquery | CTE group E1 (total > 100000), MATERIALIZED CTE
WITH c AS MATERIALIZED (SELECT o_custkey, sum(o_totalprice) AS total FROM orders GROUP BY o_custkey) SELECT count(*) FROM c WHERE total > 100000;

-- Q209 | subquery | CTE group E2 (total > 400000), plain CTE
WITH c AS (SELECT o_custkey, sum(o_totalprice) AS total FROM orders GROUP BY o_custkey) SELECT count(*) FROM c WHERE total > 400000;

-- Q210 | subquery | CTE group E2 (total > 400000), MATERIALIZED CTE
WITH c AS MATERIALIZED (SELECT o_custkey, sum(o_totalprice) AS total FROM orders GROUP BY o_custkey) SELECT count(*) FROM c WHERE total > 400000;

-- Q211 | sort | dedupe group F1 on l_shipmode, l_returnflag, DISTINCT form
SELECT DISTINCT l_shipmode, l_returnflag FROM lineitem;

-- Q212 | sort | dedupe group F1 on l_shipmode, l_returnflag, GROUP BY form
SELECT l_shipmode, l_returnflag FROM lineitem GROUP BY l_shipmode, l_returnflag;

-- Q213 | sort | dedupe group F2 on o_custkey, DISTINCT form
SELECT DISTINCT o_custkey FROM orders;

-- Q214 | sort | dedupe group F2 on o_custkey, GROUP BY form
SELECT o_custkey FROM orders GROUP BY o_custkey;

-- Q215 | pattern | OR rewrite group G1 (custkey 100, price > 450000), OR form
SELECT o_orderkey FROM orders WHERE o_custkey = 100 OR o_totalprice > 450000;

-- Q216 | pattern | OR rewrite group G1 (custkey 100, price > 450000), UNION ALL form
SELECT o_orderkey FROM orders WHERE o_custkey = 100 UNION ALL SELECT o_orderkey FROM orders WHERE o_totalprice > 450000 AND o_custkey <> 100;

-- Q217 | pattern | OR rewrite group G2 (custkey 77777, price > 520000), OR form
SELECT o_orderkey FROM orders WHERE o_custkey = 77777 OR o_totalprice > 520000;

-- Q218 | pattern | OR rewrite group G2 (custkey 77777, price > 520000), UNION ALL form
SELECT o_orderkey FROM orders WHERE o_custkey = 77777 UNION ALL SELECT o_orderkey FROM orders WHERE o_totalprice > 520000 AND o_custkey <> 77777;

-- Q219 | window | row_number per customer over recent orders
SELECT o_custkey, o_orderkey, row_number() OVER (PARTITION BY o_custkey ORDER BY o_orderdate) AS rn FROM orders WHERE o_orderdate >= DATE '1997-01-01';

-- Q220 | window | sum over partition on lineitem
SELECT l_orderkey, sum(l_extendedprice) OVER (PARTITION BY l_orderkey) AS order_total FROM lineitem WHERE l_shipdate >= DATE '1998-06-01';

-- Q221 | window | moving average over ordered orders
SELECT o_orderkey, o_totalprice, avg(o_totalprice) OVER (ORDER BY o_orderdate ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS moving_avg FROM orders WHERE o_orderdate >= DATE '1998-01-01';

-- Q222 | join | aggregate join with top-N, LIMIT 5
SELECT o_orderkey, o_orderdate, sum(l_extendedprice) AS rev FROM orders JOIN lineitem ON o_orderkey = l_orderkey WHERE o_orderdate >= DATE '1997-01-01' GROUP BY o_orderkey, o_orderdate ORDER BY rev DESC LIMIT 5;

-- Q223 | join | aggregate join with top-N, LIMIT 50
SELECT o_orderkey, o_orderdate, sum(l_extendedprice) AS rev FROM orders JOIN lineitem ON o_orderkey = l_orderkey WHERE o_orderdate >= DATE '1997-01-01' GROUP BY o_orderkey, o_orderdate ORDER BY rev DESC LIMIT 50;

-- Q224 | join | aggregate join with top-N, LIMIT 5000
SELECT o_orderkey, o_orderdate, sum(l_extendedprice) AS rev FROM orders JOIN lineitem ON o_orderkey = l_orderkey WHERE o_orderdate >= DATE '1997-01-01' GROUP BY o_orderkey, o_orderdate ORDER BY rev DESC LIMIT 5000;

-- Q225 | subquery | min-cost correlated subquery, size 15, type %BRASS
SELECT s_acctbal, s_name, p_partkey FROM part, supplier, partsupp WHERE p_partkey = ps_partkey AND s_suppkey = ps_suppkey AND p_size = 15 AND p_type LIKE '%BRASS' AND ps_supplycost = (SELECT min(ps_supplycost) FROM partsupp WHERE p_partkey = ps_partkey) ORDER BY s_acctbal DESC LIMIT 100;

-- Q226 | subquery | min-cost correlated subquery, size 25, type %TIN
SELECT s_acctbal, s_name, p_partkey FROM part, supplier, partsupp WHERE p_partkey = ps_partkey AND s_suppkey = ps_suppkey AND p_size = 25 AND p_type LIKE '%TIN' AND ps_supplycost = (SELECT min(ps_supplycost) FROM partsupp WHERE p_partkey = ps_partkey) ORDER BY s_acctbal DESC LIMIT 100;

-- Q227 | subquery | min-cost correlated subquery, size 40, type %STEEL
SELECT s_acctbal, s_name, p_partkey FROM part, supplier, partsupp WHERE p_partkey = ps_partkey AND s_suppkey = ps_suppkey AND p_size = 40 AND p_type LIKE '%STEEL' AND ps_supplycost = (SELECT min(ps_supplycost) FROM partsupp WHERE p_partkey = ps_partkey) ORDER BY s_acctbal DESC LIMIT 100;

-- Q228 | join | join selectivity sweep: filter on orders, 1 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE o.o_orderdate >= DATE '1996-01-01' AND o.o_orderdate < DATE '1996-01-02';

-- Q229 | join | join selectivity sweep: filter on orders, 30 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE o.o_orderdate >= DATE '1996-01-01' AND o.o_orderdate < DATE '1996-01-31';

-- Q230 | join | join selectivity sweep: filter on orders, 365 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE o.o_orderdate >= DATE '1996-01-01' AND o.o_orderdate < DATE '1996-12-31';

-- Q231 | join | join selectivity sweep: filter on lineitem, 1 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE l.l_shipdate >= DATE '1996-01-01' AND l.l_shipdate < DATE '1996-01-02';

-- Q232 | join | join selectivity sweep: filter on lineitem, 30 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE l.l_shipdate >= DATE '1996-01-01' AND l.l_shipdate < DATE '1996-01-31';

-- Q233 | join | join selectivity sweep: filter on lineitem, 365 day window
SELECT count(*) FROM orders o JOIN lineitem l ON l.l_orderkey = o.o_orderkey WHERE l.l_shipdate >= DATE '1996-01-01' AND l.l_shipdate < DATE '1996-12-31';
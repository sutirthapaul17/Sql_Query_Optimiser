# RAG-Powered SQL Query Optimization Assistant

A Java/Spring Boot project that uses Retrieval-Augmented Generation
(RAG), PostgreSQL query-plan analysis, and controlled benchmarking to
identify SQL query candidates that may execute more
efficiently---without assuming that an LLM-generated query is
automatically correct or faster.

> **Core objective:** Improve database query execution performance,
> primarily measured by execution time, while preserving query semantics
> and enforcing read-only safety constraints.

## Table of Contents

-   [1. Project Overview](#1-project-overview)
-   [2. Problem Statement](#2-problem-statement)
-   [3. Goals and Non-Goals](#3-goals-and-non-goals)
-   [4. Technology Stack](#4-technology-stack)
-   [5. Current Scope](#5-current-scope)
-   [6. High-Level Architecture](#6-high-level-architecture)
-   [7. How the System Works](#7-how-the-system-works)
-   [8. What Is Being Optimized?](#8-what-is-being-optimized)
-   [9. RAG and Knowledge Base](#9-rag-and-knowledge-base)
-   [10. Candidate Generation and
    Evaluation](#10-candidate-generation-and-evaluation)
-   [11. SQL Safety and Validation](#11-sql-safety-and-validation)
-   [12. Benchmarking Methodology](#12-benchmarking-methodology)
-   [13. Experimental Baseline](#13-experimental-baseline)
-   [14. API and Result Reporting](#14-api-and-result-reporting)
-   [15. Project Structure](#15-project-structure)
-   [16. Configuration and Running
    Locally](#16-configuration-and-running-locally)
-   [17. Remaining Work and Validation
    Checklist](#17-remaining-work-and-validation-checklist)
-   [18. Known Limitations](#18-known-limitations)
-   [19. Future Enhancements](#19-future-enhancements)
-   [20. Success Criteria](#20-success-criteria)

------------------------------------------------------------------------

## 1. Project Overview

The RAG-Powered SQL Query Optimization Assistant is intended to help
analyze a SQL query, identify possible inefficiencies, retrieve relevant
optimization knowledge and historical experiments, generate a limited
number of candidate alternatives, and use the target database to
evaluate those candidates.

The project starts with PostgreSQL and a TPC-H dataset running in
Docker. The planned backend uses Java, Spring Boot, Spring AI, JDBC, and
PostgreSQL with pgvector for vector retrieval. An LLM proposes
optimization hypotheses; PostgreSQL remains responsible for generating
its own execution plans. The system should recommend an optimization
only when measurements and correctness checks support it.

The assistant is **not intended to replace PostgreSQL's query
optimizer** or to blindly force a plan chosen by an LLM. It acts as an
evidence-based analysis and experimentation layer around the database.

## 2. Problem Statement

SQL performance can vary with:

-   Query structure, joins, filters, grouping, sorting, and subqueries.
-   Table size, data distribution, and cardinality estimates.
-   Index definitions and table statistics.
-   Database engine/version and planner configuration.
-   Available memory, cache state, disk I/O, and system load.

Manually inspecting `EXPLAIN` plans and testing tuning options can be
time-consuming. An LLM used without controls can suggest invalid SQL,
alter query semantics, hallucinate database behavior, or recommend
changes that are slower than the original.

This project combines:

1.  Live database and query-plan context.
2.  Retrieval of relevant database documentation and prior experiment
    records.
3.  LLM-generated optimization hypotheses.
4.  Candidate SQL/configuration validation.
5.  Controlled execution measurements.
6.  Result-equivalence checking.
7.  Persistent experiment records that can inform later recommendations.

## 3. Goals and Non-Goals

### Goals

-   Analyze supported read-only SQL queries, beginning with PostgreSQL
    `SELECT` queries.
-   Capture a baseline query plan and relevant database context.
-   Use RAG to retrieve relevant optimization documentation and similar
    historical experiments.
-   Generate a small, constrained set of candidate optimizations.
-   Measure actual query performance rather than relying only on an LLM
    explanation or estimated plan cost.
-   Validate that a candidate preserves the original query's result
    semantics.
-   Reject unsafe or unsupported SQL before it reaches database
    execution.
-   Store experiment details so results can be reproduced and reused.
-   Explain recommendations with plan and measurement evidence.

### Non-goals for the initial version

-   Optimizing or executing arbitrary write statements through the
    optimizer endpoint.
-   Automatically applying destructive changes or creating/dropping
    indexes in production.
-   Replacing PostgreSQL's internal query optimizer.
-   Guaranteeing that every query can be made faster.
-   Supporting every SQL dialect from the start.
-   Treating a lower estimated planner cost as proof of a faster real
    execution.

## 4. Technology Stack

  -----------------------------------------------------------------------
  Component               Technology / choice     Purpose
  ----------------------- ----------------------- -----------------------
  Backend API and         Java, Spring Boot       Accept requests and
  orchestration                                   coordinate optimization

  LLM integration         Spring AI               Candidate generation
                                                  and model integration

  Model provider          OpenAI-compatible API   LLM inference
                          through OpenRouter in   
                          the current setup       

  Relational database     PostgreSQL              Target query workload
                                                  and structured records

  Vector retrieval        pgvector with Spring AI Semantic search over
                          vector-store            knowledge and
                          integration             experiment summaries

  Database connectivity   JDBC and adapter        Execute queries,
                          interfaces              inspect metadata, and
                                                  collect plans

  SQL parsing             PostgreSQL-aware SQL    Structural query
                          parser / AST library to validation and feature
                          be selected and         extraction
                          validated               

  Benchmark workload      TPC-H data in           Repeatable
                          PostgreSQL              analytical-query
                                                  experiments

  Frontend (planned)      React                   Submit queries and
                                                  inspect plans, metrics,
                                                  and recommendations

  Local database          Docker                  Isolated development
  environment             (`tpch-postgres`,       and experiment
                          database `tpch`)        environment
  -----------------------------------------------------------------------

The exact dependency versions and configuration should be taken from the
current source repository. This README describes the project direction
and known setup; it does not claim every planned component is already
implemented.

## 5. Current Scope

The current development focus is PostgreSQL and read-only query
optimization. The expected primary path is:

-   Receive a SQL query through the API.
-   Validate that it is a supported, read-only query.
-   Inspect the baseline plan and assess whether optimization should be
    attempted.
-   Retrieve relevant evidence and generate a candidate when
    appropriate.
-   Validate and evaluate the candidate.
-   Return the original query if no candidate is sufficiently supported.
-   Return selected query results subject to configured row and timeout
    limits.

`INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, DDL statements such as
`DROP`/`ALTER`, and multiple statements should be rejected by the
optimizer endpoint. They should **not** be passed through to the
database for automatic execution by this read-query optimizer. A
separate write-execution feature would require its own explicit
authorization and safety design.

A statement that begins with `SELECT` is not automatically safe. For
example, a `WITH` clause may contain a data-modifying CTE, and functions
can have side effects. Classification must be structural and backed by
database permissions.

## 6. High-Level Architecture

``` text
Client
  |
  v
EngineController (HTTP/API boundary)
  |
  v
SQL classification and safety validation
  |
  +---- Unsupported / unsafe / non-read-only SQL
  |       |
  |       +--> Reject request; do not execute it
  |
  v
OptimizerEngine
  |
  +--> Normalize query / derive query features
  |
  +--> Target database adapter
  |      +--> Live metadata and schema context
  |      +--> Baseline EXPLAIN plan and cost
  |      +--> Baseline measurement when configured
  |
  +--> PlanAssessor
  |      +--> Decide whether an optimization attempt is warranted
  |
  +--> KnowledgeSearchTool / RAG
  |      +--> Relevant documentation
  |      +--> Similar historical query-plan experiments
  |
  +--> CandidateGenerator (LLM)
  |      +--> Propose a limited set of evidence-backed candidates
  |
  +--> Candidate safety and validity checks
  |
  +--> Target database adapter
  |      +--> Candidate EXPLAIN / controlled benchmark
  |
  +--> Result-equivalence verifier
  |
  +--> Candidate selection policy
  |      +--> Select candidate only when required checks pass
  |      +--> Otherwise retain original query or report failure
  |
  +--> QueryResultService
  |      +--> Execute the selected read query within configured limits
  |
  v
OptimizationOutcome / API response
  |
  +--> SQL, decision, evidence, costs, timings, warnings, result rows
  |
  v
Persist experiment record for future retrieval (planned/ongoing)
```

### Responsibility boundaries

-   **EngineController:** HTTP request handling, input checks, response
    mapping, and routing.
-   **SQL classifier/validator:** Determines whether the statement
    belongs to the supported read-only query subset.
-   **OptimizerEngine:** Coordinates baseline analysis, assessment,
    retrieval, candidate generation, evaluation, and decision-making.
-   **TargetDatabaseTool / database adapter:** Database-specific
    metadata access, `EXPLAIN`, permitted settings, and controlled
    measurements.
-   **PlanAssessor:** Uses deterministic facts from the baseline plan to
    identify potential issues or decide whether to skip candidate
    generation.
-   **KnowledgeSearchTool:** Retrieves documentation and similar
    historical experiments from the knowledge store.
-   **CandidateGenerator:** Uses the LLM to propose a limited candidate
    based on query context and retrieved evidence.
-   **QueryResultService:** Retrieves rows from the selected query with
    row limits and timeouts.
-   **Result comparator/verifier (remaining work):** Checks
    original-versus-candidate output equivalence under controlled
    conditions.
-   **Persistence/evaluation layer (planned):** Saves inputs, context,
    plans, measurements, validation outcomes, and notes for
    reproducibility.

## 7. How the System Works

### Step 1 --- Receive and classify SQL

The client submits SQL to the API. The request is checked for empty
input, supported statement type, multiple statements, and disallowed
constructs. The initial optimization endpoint is intended for read-only
queries only.

Unsupported statements must be rejected before baseline `EXPLAIN`, LLM
invocation, candidate evaluation, or result retrieval.

### Step 2 --- Capture the baseline

The system obtains the original query's baseline plan, typically with
PostgreSQL `EXPLAIN`. The plan provides estimated costs and plan
structure. When enabled and safe, controlled benchmark runs capture
actual execution time and buffer statistics.

`EXPLAIN` estimates are useful diagnostic information. `EXPLAIN ANALYZE`
actually executes the query, so it should only be used against a
suitable test database with timeouts and resource controls.

### Step 3 --- Assess the plan

A deterministic assessor inspects facts such as plan nodes, estimated
cost, joins, scans, and other available signals. If no actionable flags
are found and the configuration does not require an LLM consultation,
the engine may return `KEEP_ORIGINAL` without generating a candidate.

This early-return behavior is intentional for efficiency; it means that
a `KEEP_ORIGINAL` decision does not necessarily indicate that a
candidate was generated and tested.

### Step 4 --- Retrieve relevant knowledge (RAG)

The retrieval layer searches for relevant database optimization
documentation and similar historical experiments. Retrieved material can
include guidance about joins, indexes, filters, aggregation, sorting,
statistics, and plan behavior.

Retrieval should consider database engine/version and workload context.
Live schema, indexes, settings, and current plans must be obtained from
the target database rather than treated as timeless knowledge.

RAG supplies evidence and context. It does not prove that a
recommendation is correct or faster.

### Step 5 --- Generate a candidate

The LLM receives the original SQL, relevant schema and plan context, and
retrieved evidence. It proposes a limited candidate query or permitted
optimization hypothesis and explains the expected benefit and risks.

The candidate is untrusted until independently validated. The LLM's
explanation is not evidence of semantic equivalence or performance
improvement.

### Step 6 --- Validate and evaluate the candidate

The candidate should be parsed and checked against the supported SQL
policy. The database then validates the candidate plan. If enabled,
controlled tests measure actual execution and compare its result with
the original query.

The candidate-selection policy must not accept a candidate merely
because it parses, executes, or has a lower estimated cost. A candidate
with failed or inconclusive required verification must not be marked as
verified.

### Step 7 --- Decide whether to keep or replace the query

The system compares the candidate with the baseline using correctness
checks and performance evidence. If the candidate meets the configured
acceptance policy, it can be selected. Otherwise, the original query
should be retained or the request should report that optimization could
not be safely established.

### Step 8 --- Return results and evidence

The response can include the decision, original SQL, selected SQL,
reasoning, flags, costs, timings, evidence, warnings, validation status,
and retrieved rows. Row limits and timeouts protect the API from
unexpectedly large or expensive results.

### Step 9 --- Record the experiment

A reproducible record should capture the query, database/version, schema
and indexes, settings, plans, measurements, validation result, and
notes. Future RAG retrieval can use these records as historical
evidence, with uncertain or outdated records clearly labelled.

## 8. What Is Being Optimized?

The primary objective is **database query execution performance**.

  --------------------------------------------------------------------------------
  Metric                  Meaning                 Role
  ----------------------- ----------------------- --------------------------------
  Actual execution time   Time spent executing a  Primary performance metric
                          query and producing its 
                          result                  

  Estimated planner cost  PostgreSQL's relative   Supporting plan-selection metric
                          estimate of plan work;  
                          not milliseconds or     
                          money                   

  Planning time           Time PostgreSQL spends  Supporting metric, especially
                          planning the query      for complex queries

  Buffer and disk         Buffer hits/reads and   Diagnostic/resource-efficiency
  activity                other available I/O     metric
                          signals                 

  CPU and memory usage    Resources consumed      Secondary efficiency/scalability
                          during execution, when  metrics
                          measurable              

  Plan structure          Scan, join, sort, and   Helps explain performance
                          aggregate choices       changes

  Correctness             Whether original and    Mandatory acceptance constraint
                          candidate preserve the  
                          same result semantics   

  Safety                  Whether SQL and         Mandatory acceptance constraint
                          execution remain within 
                          the read-only policy    
  --------------------------------------------------------------------------------

### Optimization overhead matters

The system must also measure its own overhead:

-   RAG retrieval latency.
-   LLM generation latency.
-   SQL parsing and validation latency.
-   Candidate verification and benchmarking time.
-   Total optimization latency.
-   End-to-end latency seen by the caller.

A query that saves a few milliseconds but requires a long LLM call may
not be worthwhile for a one-time execution. For repeated queries, a
verified candidate may pay off if it can be safely reused. Candidate
caching, invalidation rules, and repeat-execution break-even analysis
are future design considerations; cache reuse must account for query
parameters, schema/index changes, settings, and other relevant context.

## 9. RAG and Knowledge Base

The knowledge strategy separates general guidance, historical
experiments, and live database context.

### General optimization knowledge

Potential topics include:

-   Hash joins, merge joins, and nested-loop joins.
-   Index selectivity, composite-index column order, index scans, bitmap
    scans, and index-only scans.
-   Predicate pushdown, aggregation, sorting, and unnecessary scans.
-   Cardinality estimates and table statistics.
-   PostgreSQL-specific planner behavior and version constraints.

### Historical experiment records

Each useful experiment should preserve:

-   Query text and normalized fingerprint.
-   Engine and version.
-   Dataset and scale factor.
-   Schema/index context and relevant statistics.
-   Database settings.
-   Baseline and candidate plan text/JSON.
-   Planning time, execution time, buffer metrics, and repeated-run
    statistics.
-   Result-equivalence and safety-validation status.
-   Outcome, limitations, and notes.

### Live database context

Fetch the current schema, column types, indexes, constraints, table
sizes, relevant statistics, engine version, permitted settings, and
current plan when needed. These details may change and should not be
treated as general static documentation.

### Retrieval principles

-   Retrieve evidence relevant to the query's structure and context, not
    just raw SQL similarity.
-   Preserve source, engine, version, and experiment status metadata.
-   Distinguish measured facts from LLM-generated explanations.
-   Do not promote an unverified historical result into a guaranteed
    optimization rule.

## 10. Candidate Generation and Evaluation

The LLM is a **candidate generator**, not the final authority.

A candidate evaluation should capture:

1.  Why the candidate was proposed.
2.  Which retrieved documentation or experiments support it.
3.  The expected plan change or performance mechanism.
4.  Whether the SQL parses and is permitted.
5.  Whether the database can plan it.
6.  Whether results are equivalent to the original.
7.  Actual timing and relevant resource measurements.
8.  Whether the result is repeatable and passes the acceptance policy.
9.  Why the candidate was accepted or rejected.

Potential candidate classes include equivalent SQL rewrites and, where
explicitly permitted, test-only session settings or index
recommendations. Any index or persistent configuration change should
require explicit review and should not be applied automatically to
production in the initial version.

## 11. SQL Safety and Validation

Safety and correctness are required guardrails; they are not
interchangeable with performance optimization.

### Read-only boundary

The optimizer endpoint should accept only supported, read-only query
structures. Reject write statements such as `INSERT`, `UPDATE`,
`DELETE`, `TRUNCATE`, and schema-changing statements such as `DROP` and
`ALTER`. Reject multiple statements and data-modifying CTEs.

Do not rely only on checking the first keyword. Use a dialect-aware
parser/AST or another structural validation approach, test its coverage,
and enforce a database role that has only the required read permissions.
Parser checks and database permissions are complementary layers.

### Execution controls

-   Use a dedicated test database or disposable clone for benchmarking.
-   Use read-only credentials for query analysis and result retrieval.
-   Set statement timeouts, cancellation, concurrency limits, and
    resource caps.
-   Do not automatically apply destructive changes or global setting
    changes.
-   Use controlled transactions/snapshots where appropriate.
-   Do not send sensitive schema or data to an external model unless
    policy permits it; minimize or redact context where appropriate.
-   Log the validation outcome and any rejection reason.

## 12. Benchmarking Methodology

A fair comparison must keep conditions as consistent as practical.

1.  Capture the exact SQL and database context.
2.  Save the baseline plan and settings.
3.  Warm up the workload where appropriate.
4.  Run multiple measured trials for baseline and candidate.
5.  Record median execution time and spread, not just the fastest
    result.
6.  Record planning time and `EXPLAIN (ANALYZE, BUFFERS, SETTINGS)`
    output when supported and safe.
7.  Record cache conditions, test order, timeouts, and failures.
8.  Compare outputs for semantic equivalence.
9.  Report estimated cost separately from actual execution time.
10. Retain full plan output so a result can be reproduced and audited.

For analytical queries, `EXPLAIN ANALYZE` runs the statement. Benchmark
only in a safe test environment with suitable resource limits.
Cold-cache and warm-cache results should be identified separately if
both are measured.

## 13. Experimental Baseline

The project has an initial set of exploratory experiments on PostgreSQL
using the TPC-H dataset in a Docker container named `tpch-postgres`,
with database `tpch`.

The canonical query tested was:

``` sql
SELECT c.c_name, SUM(o.o_totalprice)
FROM customer c
JOIN orders o ON o.o_custkey = c.c_custkey
WHERE c.c_mktsegment = 'BUILDING'
GROUP BY c.c_name;
```

It joins customers to orders, filters customers to the `BUILDING` market
segment, and aggregates order total prices by customer name.

### Historical observations recorded so far

  ------------------------------------------------------------------------
  Scenario                   Recorded execution time Status / caveat
  --------------------- ---------------------------- ---------------------
  Default plan, JIT                       479.241 ms Preliminary
  enabled                                            single-run value

  JIT disabled                            382.716 ms Preliminary
                                                     single-run value

  Hash join forced                        371.364 ms One join-method
                                                     experiment

  Merge join forced                       525.618 ms Slower in this
                                                     recorded run

  Nested loop forced                      744.049 ms Slower in this
                                                     recorded run

  Index experiment A                      252.539 ms Exact index/plan
                                                     pairing needs
                                                     rechecking

  Index experiment B                      261.856 ms Exact index/plan
                                                     pairing needs
                                                     rechecking

  Best recorded                           239.337 ms Preliminary best
  index-related run                                  observed value; not
                                                     yet a reproducible
                                                     benchmark
  ------------------------------------------------------------------------

These numbers are **historical exploratory observations, not validated
claims**. The original full terminal output is not preserved in the
experiment log, and some index-to-timing pairings are uncertain. Re-run
the scenarios with the exact SQL, index definitions, settings,
PostgreSQL version, and complete plans before drawing conclusions.

The experiments suggest that planner settings and indexes can materially
change the plan and runtime for this query. They do not prove that
forcing a join type or adding an index is generally superior to
PostgreSQL's default optimizer.

### Revalidation tasks for these experiments

-   Capture PostgreSQL version, TPC-H scale factor, schema, indexes, and
    settings.
-   Inspect current index definitions rather than relying on uncertain
    historical mappings.
-   Save exact commands, DDL, and complete
    `EXPLAIN (ANALYZE, BUFFERS, SETTINGS, VERBOSE)` output where
    supported.
-   Repeat scenarios after warm-up, ideally 5--10 measured runs.
-   Report median and spread.
-   Verify result equivalence for every candidate.
-   Mark historical values unverified until their exact scenario is
    confirmed.

## 14. API and Result Reporting

The current response model is designed around an
`OptimizationOutcome`-style result, including fields such as:

-   `decision`: `KEEP_ORIGINAL`, `OPTIMIZED`, or `FAILED`.
-   `originalSql` and `finalSql`.
-   `reasoning`, `flags`, and supporting `evidence`.
-   Index and setting recommendations.
-   `baselineCost` and `candidateCost`.
-   Baseline/candidate timing fields when collected.
-   `resultsMatch` when a real equivalence check has been performed.
-   `warnings`.
-   `retrievedData` and `dataTruncated`.

The exact response fields depend on the current code version. A field
should not imply that a check passed if that check has not actually run.
In particular, `resultsMatch` should remain unknown/inconclusive until
the comparison is completed. A capped result list is for response
display and must not be used as proof that two full query results are
equivalent.

Recommended additional audit fields include:

-   SQL safety status.
-   SQL parse/validity status.
-   Verification status and method.
-   Candidate acceptance/rejection reason.
-   Optimization overhead and end-to-end latency.
-   Number of candidate runs and repeated benchmark samples.

## 15. Project Structure

The following is a conceptual organization based on the current
architecture; actual package names may differ.

``` text
src/main/java/.../
├── Controller/
│   └── EngineController
├── Optimizer/
│   ├── OptimizerEngine
│   ├── PlanAssessor
│   └── Dto/
│       └── OptimizationOutcome
├── Database/
│   ├── TargetDatabaseTool
│   ├── QueryResultService
│   └── DatabaseAdapter (planned abstraction)
├── Knowledge/
│   ├── KnowledgeSearchTool
│   ├── KnowledgeRetriever
│   └── OptimizationKnowledgeTool
└── AI/
    ├── CandidateGenerator
    └── SqlOptimizationPolicy

src/main/resources/
└── prompts/
    └── sql-optimization-retrieval-policy.txt
```

This diagram is illustrative, not a guarantee that every class or
package is present under exactly these names in the current working
tree.

## 16. Configuration and Running Locally

### Prerequisites

-   Java version compatible with the current build (the project has been
    developed around Java 21).
-   Maven or the build tool used by the repository.
-   Docker.
-   PostgreSQL with the TPC-H dataset loaded.
-   A configured model provider and API key if LLM candidate generation
    is enabled.
-   pgvector and Spring AI vector-store configuration if using RAG
    retrieval.

### Database environment used in experiments

-   Docker container: `tpch-postgres`
-   Database: `tpch`
-   User used during local experiments: `postgres`

For local experimentation, prefer a dedicated restricted database user
rather than an administrative account. Do not expose credentials in
source control.

### Example commands

Inspect the PostgreSQL container:

``` bash
docker ps
```

Connect to the test database:

``` bash
docker exec -it tpch-postgres psql -U postgres -d tpch
```

Inspect a query plan without executing the query:

``` sql
EXPLAIN
SELECT c.c_name, SUM(o.o_totalprice)
FROM customer c
JOIN orders o ON o.o_custkey = c.c_custkey
WHERE c.c_mktsegment = 'BUILDING'
GROUP BY c.c_name;
```

Measure a query in the test environment:

``` sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.c_name, SUM(o.o_totalprice)
FROM customer c
JOIN orders o ON o.o_custkey = c.c_custkey
WHERE c.c_mktsegment = 'BUILDING'
GROUP BY c.c_name;
```

`EXPLAIN ANALYZE` executes the query. Use it only in the appropriate
test environment and with timeouts/resource controls.

### Application startup

Use the commands and configuration documented in the current repository
(for example, the project's Maven wrapper if present). Required database
URLs, credentials, vector-store settings, model endpoint, and model API
key should be supplied through environment variables or local
configuration excluded from version control. Exact application run
commands and endpoint paths should be added once confirmed from the
current source tree.

## 17. Remaining Work and Validation Checklist

The following checklist focuses on validating generated SQL and proving
that optimization claims are supported.

### A. SQL safety and statement classification

-   [ ] Implement or verify parser-based classification for the
    supported read-only SQL subset.
-   [ ] Reject `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `DROP`,
    `ALTER`, and other unsupported statement types at the API boundary.
-   [ ] Reject multiple statements and data-modifying CTEs.
-   [ ] Check for disallowed functions or constructs with side effects.
-   [ ] Ensure rejected SQL never reaches `EXPLAIN`, candidate
    generation, or result retrieval.
-   [ ] Enforce read-only database permissions as defense in depth.
-   [ ] Add tests for valid `SELECT`, `WITH ... SELECT`, comments, mixed
    case, quoted identifiers, semicolons, and malicious/ambiguous
    inputs.

### B. Candidate SQL validity

-   [ ] Parse the LLM-generated SQL independently of the prompt.
-   [ ] Confirm it contains exactly one permitted read-only statement.
-   [ ] Verify referenced tables, columns, aliases, and types against
    the live schema where possible.
-   [ ] Confirm that the target database can plan the candidate.
-   [ ] Reject malformed, unsupported, or policy-violating candidates.
-   [ ] Never treat the LLM's explanation as proof of validity.

### C. Semantic correctness / result equivalence

-   [ ] Compare original and candidate results using the same database
    snapshot where practical.
-   [ ] Compare all result rows, not only the first configured display
    limit.
-   [ ] Preserve duplicate-row multiplicity.
-   [ ] Handle `NULL` values correctly.
-   [ ] Check column count, compatible types, and output values.
-   [ ] Account for `ORDER BY` and result-order semantics where
    applicable.
-   [ ] Include edge cases involving joins, aggregation, subqueries,
    `NULL`, duplicates, and empty results.
-   [ ] Mark verification as failed or inconclusive if either query
    errors, times out, or cannot be fully compared.
-   [ ] Do not accept a candidate as verified when required correctness
    checks are inconclusive.

### D. Performance validation

-   [ ] Measure baseline and candidate execution times under comparable
    conditions.
-   [ ] Repeat measurements and report median and spread.
-   [ ] Capture estimated plan cost separately from actual runtime.
-   [ ] Record planning time, buffers/I/O, and plan structure when
    available.
-   [ ] Confirm that claimed improvement exceeds ordinary measurement
    noise.
-   [ ] Detect and report performance regressions.
-   [ ] Keep the original query when no candidate demonstrates a
    reliable benefit.

### E. End-to-end overhead

-   [ ] Measure RAG retrieval latency.
-   [ ] Measure LLM generation latency.
-   [ ] Measure parsing, safety checks, and verification time.
-   [ ] Measure total optimization latency and end-to-end response
    latency.
-   [ ] Evaluate whether one-time optimization cost is worthwhile for
    repeated query execution.
-   [ ] If caching verified candidates is added, implement invalidation
    for relevant parameter, schema, index, statistics, version, and
    settings changes.

### F. Persistence, explainability, and tests

-   [ ] Persist the original query, candidate, database/version context,
    settings, plans, and benchmark samples.
-   [ ] Store validation outcomes and the reason for accepting or
    rejecting each candidate.
-   [ ] Keep measured facts separate from LLM-generated explanations.
-   [ ] Add unit and integration tests for unsafe SQL, invalid
    candidates, equivalent candidates, incorrect rewrites, timeouts, and
    benchmark regressions.
-   [ ] Re-run and verify historical TPC-H experiments before using them
    as RAG evidence.
-   [ ] Ensure every performance claim can be reproduced from the stored
    experiment record.

## 18. Known Limitations

-   The recorded TPC-H timings are preliminary and some index mappings
    are uncertain.
-   A lower PostgreSQL estimated cost does not guarantee a lower
    execution time.
-   A valid query is not necessarily semantically equivalent to the
    original.
-   Matching only a limited number of output rows is not a proof of full
    result equivalence.
-   An LLM can produce invalid or slower SQL even with relevant RAG
    context.
-   Optimization, retrieval, and verification add latency and may
    outweigh the benefit for one-off queries.
-   The initial implementation targets PostgreSQL; database-agnostic
    behavior requires engine-specific adapters and tests.
-   Current code paths and class names may evolve; verify the source
    repository before relying on the illustrative structure in this
    README.

## 19. Future Enhancements

-   Add a database adapter interface and introduce MySQL only after the
    PostgreSQL workflow is reliable.
-   Improve SQL parsing, query normalization, fingerprinting, and
    feature extraction.
-   Build a reliable experiment collector and repeatable benchmark
    runner.
-   Add verified candidate caching with safe invalidation rules.
-   Build a React interface for baseline/candidate plan comparison and
    benchmark reporting.
-   Expand the trusted documentation corpus and historical experiment
    retrieval.
-   Add retrieval evaluation and ablation experiments to measure the
    contribution of RAG.
-   Evaluate query classes across a broader TPC-H workload rather than
    relying on one canonical query.
-   Add human-reviewed index recommendations and test-only configuration
    experiments.
-   Report performance distributions, regressions, and optimization
    overhead alongside speedup.

## 20. Success Criteria

The project should be considered successful when it can demonstrate:

1.  **Safety:** Unsupported or unsafe statements are rejected before
    execution.
2.  **Correctness:** Accepted candidates pass a documented
    result-equivalence process.
3.  **Performance:** Claimed improvements are based on repeatable
    measurements, not only estimated cost or one best run.
4.  **Reproducibility:** Experiments preserve enough context to be
    repeated.
5.  **Explainability:** Recommendations cite relevant retrieved evidence
    and actual plan/measurement differences.
6.  **Efficiency:** The system reports its own overhead and can explain
    when optimization is not worthwhile.
7.  **Reliability:** Timeouts, cancellation, resource limits, and
    failure paths behave predictably.
8.  **Extensibility:** Database-specific behavior is isolated so future
    database support does not require rewriting the core workflow.

------------------------------------------------------------------------

## Project motto

**Use RAG and an LLM to discover promising SQL optimizations, then rely
on independent validation and repeatable database measurements---not
model confidence---to decide whether a candidate is safe, equivalent,
and faster.**

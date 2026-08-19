<p align="center">
  <img src="https://img.shields.io/badge/Socket-Reachability%20Demo-666666?style=for-the-badge" alt="Socket Reachability Demo">
</p>

<h1 align="center">Northwind Order Service</h1>
<p align="center"><b>A Java reachability demo with real, reproducible numbers — not a scripted story.</b></p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-11%2B-orange?logo=openjdk&logoColor=white" alt="Java 11+">
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License">
  <img src="https://img.shields.io/badge/Vulnerable%20by%20design-do%20not%20deploy-critical" alt="Vulnerable by design">
</p>

---

## Why this exists

This is a small, realistic-looking Java/Maven order-processing service, built to show what [Socket](https://socket.dev)'s reachability analysis actually does to alert noise on a JVM codebase: **Tier 2 (precomputed)** and **Tier 1 (`--reach`, full application analysis)**.

It's shaped like a typical Spring Boot service: a couple of libraries the app deliberately calls, a couple of old dependencies left behind by a cleanup that never finished, some libraries added for a feature that shipped differently, and a big pile of transitive dependencies the web framework itself drags in that nobody on the team ever thinks about. That last category turns out to be most of a real app's CVE volume, and it's exactly where Tier 2 already does real work. Tier 1 picks up the rest: the direct, headline dependencies Tier 2 can't see into.

Every number below came from actually running `socket scan create` and `socket scan create --reach` against this exact repository. Nothing here is a mockup.

## The 60-second version

| | CVE findings | Resolved | Noise cleared |
|---|---|---|---|
| **Tier 2** (precomputed, automatic) | 229 | 140 unreachable | **61%** |
| **Tier 1** (`--reach`, full app analysis) | 229 | 188 unreachable | **82%** |

Tier 2 clears **zero** of the 61 findings that sit on this repo's *direct* dependencies — not because Tier 2 is weak, but because it has no visibility into whether your own code calls a direct dependency's vulnerable function. Every one of those 61 shows up as `direct_dependency`, an unresolved verdict, not a clean or a dirty one. What Tier 2 *does* resolve well is the 168 findings on **transitive** dependencies: the libraries Spring Boot itself pulls in. It clears 140 of those (83%).

Tier 1 full-application analysis reads the actual source and resolves the direct dependencies Tier 2 couldn't touch, on top of everything Tier 2 already had. That's the 61% → 82% jump.

## The critical-CVE story

42 of the 229 findings are critical severity. Only **3 are actually reachable**:

| Package | Critical CVE | Wired into code? | Tier 1 verdict |
|---|---|---|---|
| `log4j-core` 2.14.1 | [CVE-2021-44228](https://socket.dev/maven/package/org.apache.logging.log4j:log4j-core/overview/2.14.1) (Log4Shell) + CVE-2021-45046 | Yes — logs a customer-supplied order comment | **Reachable** (both) |
| `commons-text` 1.9 | [CVE-2022-42889](https://socket.dev/maven/package/org.apache.commons:commons-text/overview/1.9) (Text4Shell) | Yes — renders an admin-editable notification template | **Reachable** |
| `jackson-databind` 2.8.10 (transitive) | 22 distinct critical CVEs | No — Spring Boot pulls it in, nothing calls it | **Unreachable** (all 22) |
| `fastjson` 1.2.24 | 2 critical (autoType RCE family) | No — declared, never called | 1 unreachable, 1 no verdict yet |
| 5 more packages | 1 critical each | No | **Unreachable** |

35 of the 42 criticals (83%) resolve unreachable. Tier 2 alone resolves 33 of the 42 (79%) — mostly by clearing the transitive `jackson-databind` pile, but it can't touch any of the 9 criticals sitting on direct dependencies, including the 2 that are genuinely Log4Shell. Tier 1 is what actually tells you Log4Shell and Text4Shell are real here, and the other 39 criticals aren't.

## Package breakdown

**Wired into code — genuinely reachable:**

| Package | CVEs | Tier 1 verdict |
|---|---|---|
| `log4j-core` 2.14.1 | 7 (2 critical) | 5 reachable, 2 unreachable |
| `commons-text` 1.9 | 1 (1 critical) | 1 reachable |

**Dead code — declared, and called by a class nothing invokes anymore:**

| Package | CVEs | Tier 1 verdict |
|---|---|---|
| `xstream` 1.4.19 | 3 | 2 reachable, 1 unreachable* |
| `commons-collections` 3.2.1 | 2 (1 critical) | 2 unreachable |

*`LegacyOrderXmlImporter` (xstream) and `SessionCacheDeserializer` (commons-collections) are real classes in this repo — they're just never called from `Main`. Tier 1's static analysis still finds and evaluates them, which is why xstream still shows 2 reachable: reachability tracks the call graph in the class, not whether `Main` happens to invoke it. That's the more literal reading of "reachable from this codebase," and worth knowing going in.

**Declared, never touched at all:**

`spring-core` 4.3.14.RELEASE, `snakeyaml` 1.17, `guava` 24.1-jre, `commons-beanutils` 1.9.2, `commons-fileupload` 1.3.2 (1 critical), `bcprov-jdk15on` 1.51, `bcpkix-jdk15on` 1.51, `velocity` 1.6.4, `dom4j` 1.6.1, `fastjson` 1.2.24 (2 critical) — 10 packages, 42 CVEs, all direct dependencies, all resolved unreachable by Tier 1. `spring-core` and `snakeyaml` are pinned here explicitly at the same version Spring Boot already resolves them to — a common real pattern (teams pin a transitive version for their own reasons) that also happens to make them visible to Tier 2 as direct rather than transitive.

**What the web framework brings along (`spring-boot-starter-web` 1.5.10.RELEASE, transitive):**

`jackson-databind` 2.8.10 (55 CVEs, 22 critical), `tomcat-embed-core` 8.5.27 (50 CVEs), `spring-webmvc`/`spring-web`/`spring-beans`/`spring-context`/`spring-expression`/`spring-boot`/`spring-boot-autoconfigure` 4.3.14.RELEASE / 1.5.10.RELEASE, `logback-classic`/`logback-core` 1.1.11, `hibernate-validator` 5.3.6.Final, `xercesimpl` 2.8.1, `commons-io` 2.2, `jackson-core` 2.8.10 — 168 CVEs nobody on this team chose to add. Tier 2 already resolves most of this tier on its own; that's the biggest chunk of its 61%.

## Methodology

- Every "reachable" call path uses inert, hardcoded example data. Nothing in this repo sends network traffic, spawns a process, or reads/writes outside memory. See [SAFETY.md](SAFETY.md).
- Tier 1's own pre-install step fails on one unrelated transitive package (`xerces:xercesimpl`) while setting up the analysis sandbox, which pushes 11 findings to a Tier 2 fallback instead of a full Tier 1 verdict. Included in the numbers above rather than hidden. 12 more findings (mostly inside `tomcat-embed-core` and `spring-webmvc`) come back `undeterminable_reachability`, and 8 come back `missing_support` — real, current limits of Coana's Java coverage on this dependency shape, not tuned away.
- This org runs Coana in **legacy mode**, which downgrades install/analysis failures to Tier 2 instead of halting the scan. A strict-mode org would halt on the `xerces` error unless `--reach-continue-on-install-errors` was passed explicitly.
- Every number in this README came from `socket scan view <scan-id> --json` against a real scan, not from reading the source and guessing.

## Reproduce it

```bash
# Run the app itself
mvn compile exec:java

# Tier 2: precomputed reachability, automatic on any scan
socket scan create . --repo <name> --branch main --default-branch --org <your-org> --report

# Tier 1: full application reachability analysis
socket scan create . --repo <name> --branch main --org <your-org> --reach \
  --reach-continue-on-install-errors --reach-continue-on-analysis-errors \
  --report --json
```

Then compare `reachability.head.type` per alert in `socket scan view <scan-id> --json`: `precomputed` before, `full-scan` (with a `reachable` / `unreachable` verdict) after.

## Requirements

Java 11+, Maven 3.6+. A Socket organization on a plan with reachability analysis enabled, and the [Socket CLI](https://github.com/SocketDev/socket-cli), to reproduce the scans.

---

<p align="center"><sub>Built to demo <a href="https://socket.dev">Socket</a> reachability analysis. Not a production application — see <a href="SAFETY.md">SAFETY.md</a>.</sub></p>

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

It declares 12 popular Java libraries, each on a version with real, publicly documented CVEs — Log4Shell, Text4Shell, and others most engineering teams will recognize on sight. Six of them are wired into the application code exactly the way the CVE describes: an untrusted-looking input flows into the vulnerable call. The other six are declared in `pom.xml` and never called, the way dependency bloat actually happens in real codebases — added for a feature that shipped differently, inherited from a template, pulled in and forgotten.

Every number below came from actually running `socket scan create` and `socket scan create --reach` against this exact repository. Nothing here is a mockup.

## The 60-second version

| | Vulnerabilities analyzed | Reachable | Unreachable | No reachability support | Noise reduction |
|---|---|---|---|---|---|
| **Tier 1 (`--reach`)**, whole repo | 120 | 77 | 36 | 2 (5 more fell back to Tier 2 — see [Methodology](#methodology)) | **30%** |

That 30% is Socket's own Coana engine reporting on the *entire* dependency graph, transitive packages included. The sharper, easier-to-explain-to-a-customer number is what happens on just the 12 libraries this repo deliberately ships:

| | CVE findings | Tier 2 resolves | Tier 1 resolves |
|---|---|---|---|
| 6 libraries actually called by the app | 75 | 0 (all direct deps — see below) | 72 reachable, 3 unreachable |
| 6 libraries declared but never called | 33 | 0 (all direct deps) | 31 unreachable, 2 no-support-yet |
| **Total** | **108** | **0** | **103 resolved, 34 cleared as noise** |

**Tier 2 clears zero of these 108 findings** — not because Tier 2 is weak, but because all 12 packages are *direct* dependencies, and Tier 2 has no visibility into whether your own code calls a direct dependency's vulnerable function. That's the exact gap Tier 1 full-application analysis closes: it reads the actual source, and it clears **34 findings (31%)** with zero false clears on the six libraries this app never touches.

## The critical-CVE story

All 17 critical-severity CVEs in this repo sit on 5 of the 12 target packages. Tier 2 resolves none of them (again: all direct deps). Tier 1 resolves all 17:

| Package | Critical CVE | Wired into code? | Tier 1 verdict |
|---|---|---|---|
| `log4j-core` 2.14.1 | [CVE-2021-44228](https://socket.dev/maven/package/org.apache.logging.log4j:log4j-core/overview/2.14.1) (Log4Shell) + CVE-2021-45046 | Yes — logs a customer-supplied order comment | **Reachable** (both) |
| `commons-text` 1.9 | [CVE-2022-42889](https://socket.dev/maven/package/org.apache.commons:commons-text/overview/1.9) (Text4Shell) | Yes — renders an admin-editable notification template | **Reachable** |
| `commons-collections` 3.2.1 | [CVE-2015-7501](https://socket.dev/maven/package/commons-collections:commons-collections/overview/3.2.1) | Yes — deserializes a cached session | **Reachable** |
| `jackson-databind` 2.9.8 | 12 distinct critical CVEs | Yes — deserializes a partner webhook with default typing enabled | **Reachable** (all 12) |
| `commons-fileupload` 1.3.2 | [CVE-2016-1000031](https://socket.dev/maven/package/commons-fileupload:commons-fileupload/overview/1.3.2) | **No** — declared, never called | **Unreachable** |

That last row is the one worth pausing on in a demo: a critical, headline-severity CVE, sitting in `pom.xml` as a direct dependency, that Tier 1 correctly clears because nothing in this codebase ever calls it. That's not a guess or a heuristic — it's the same source-level analysis that confirmed the other 16 are real.

## Full package breakdown

| Package (version) | In the code? | CVE findings | Tier 1 verdict |
|---|---|---|---|
| `log4j-core` 2.14.1 | Logs order comments | 7 (2 critical) | 5 reachable, 2 unreachable |
| `commons-text` 1.9 | Renders notification templates | 1 (1 critical) | 1 reachable |
| `snakeyaml` 1.30 | Loads tenant YAML config | 7 | 7 reachable |
| `jackson-databind` 2.9.8 | Deserializes partner webhooks | 55 (12 critical) | 55 reachable |
| `xstream` 1.4.19 | Imports legacy XML order exports | 3 | 2 reachable, 1 unreachable |
| `commons-collections` 3.2.1 | Deserializes cached sessions | 2 (1 critical) | 2 reachable |
| `spring-core` 5.2.0.RELEASE | Declared only | 3 | 3 unreachable |
| `guava` 24.1-jre | Declared only | 3 | 3 unreachable |
| `commons-beanutils` 1.9.2 | Declared only | 3 | 3 unreachable |
| `commons-fileupload` 1.3.2 | Declared only | 3 (1 critical) | 3 unreachable |
| `hibernate-validator` 6.0.17.Final | Declared only | 4 | 3 unreachable, 1 no-support |
| `bcprov-jdk15on` 1.51 | Declared only | 17 | 16 unreachable, 1 no-support |

Two nuances worth pointing out live, because they show this is real analysis and not a package-level allowlist:

- **`log4j-core` splits 5/7, not 7/7.** Log4Shell itself and its follow-up (CVE-2021-45046) are reachable through the logging call this app makes. Two other, lower-severity CVEs in the same library version cover code paths this app never touches, and Tier 1 correctly separates them *within the same package*.
- **`xstream` splits 2/3** the same way — one of its three CVEs sits on a path this app's XML import method doesn't exercise.

## Methodology

- Every "reachable" call path uses inert, hardcoded example data (a benign order comment, a plain YAML block, a JSON payload deserializing into `java.util.HashMap`). Nothing in this repo sends network traffic, spawns a process, or reads/writes outside memory. See [SAFETY.md](SAFETY.md).
- `xstream` 1.4.19 ships a default-deny type allowlist. The import service explicitly reopens it with `addPermission(AnyTypePermission.ANY)` — the same fix teams commonly reach for after upgrading XStream and hitting a `ForbiddenClassException`, and the reason that "fix" re-exposes the original issue.
- Tier 1's own pre-install step failed on one unrelated package (`xerces:xercesimpl`, not one of this repo's 12 declared dependencies) while setting up the analysis sandbox. That took down 5 of 120 vulnerabilities to a Tier 2 fallback instead of a full Tier 1 verdict — included in the numbers above rather than hidden.
- This org runs Coana in **legacy mode**, which downgrades install/analysis failures to Tier 2 instead of halting the scan. That's why the run above completed cleanly despite the `xerces` error; a strict-mode org would have halted unless `--reach-continue-on-install-errors` was passed explicitly.
- Every number in this README came from `socket scan view <scan-id> --json` against a real scan, not from reading the source and guessing.

## Reproduce it

```bash
# Run the app itself — six benign calls into six real vulnerable APIs
mvn compile exec:java

# Tier 2: precomputed reachability, automatic on any scan
socket scan create . --repo <name> --branch main --default-branch --org <your-org> --report

# Tier 1: full application reachability analysis
socket scan create . --repo <name> --branch main --org <your-org> --reach \
  --reach-continue-on-install-errors --reach-continue-on-analysis-errors \
  --report --json
```

Then compare `reachability.head.type` per alert in `socket scan view <scan-id> --json`: `precomputed` before, `full-scan` (with a `reachable` / `unreachable` verdict) after.

## What's in the code

| File | Package | Vulnerable pattern |
|---|---|---|
| `OrderCommentLogger.java` | log4j-core | Logging an externally-sourced string with lookups enabled |
| `NotificationTemplateService.java` | commons-text | `StringSubstitutor.createInterpolator()` over an editable template |
| `FeatureFlagConfigLoader.java` | snakeyaml | `new Yaml().load(...)` on tenant-editable config |
| `PartnerWebhookDeserializer.java` | jackson-databind | `enableDefaultTyping()` + `readValue(..., Object.class)` |
| `LegacyOrderXmlImporter.java` | xstream | `AnyTypePermission.ANY` re-opened after upgrade |
| `SessionCacheDeserializer.java` | commons-collections | `ObjectInputStream.readObject()` on cached bytes |

The other six libraries (`spring-core`, `guava`, `commons-beanutils`, `commons-fileupload`, `hibernate-validator`, `bcprov-jdk15on`) appear only in `pom.xml`.

## Requirements

Java 11+, Maven 3.6+. A Socket organization on a plan with reachability analysis enabled, and the [Socket CLI](https://github.com/SocketDev/socket-cli), to reproduce the scans.

---

<p align="center"><sub>Built to demo <a href="https://socket.dev">Socket</a> reachability analysis. Not a production application — see <a href="SAFETY.md">SAFETY.md</a>.</sub></p>

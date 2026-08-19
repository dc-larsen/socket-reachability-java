# Safety notes

This repository intentionally depends on Java libraries with known critical and high-severity CVEs, used to demo [Socket](https://socket.dev)'s reachability analysis. A few things worth being explicit about:

- **No working exploits.** Every "vulnerable" call in this codebase runs on inert, hardcoded example data: a benign order comment, a plain YAML block, a JSON payload that deserializes into `java.util.HashMap`, a small XML document. None of it triggers a JNDI lookup against a real host, runs a deserialization gadget chain, executes a script lookup, or does anything else that requires network egress, a listener, or code execution to demonstrate.
- **Nothing here reaches the network, the filesystem, or a shell.** The whole program is in-memory string/object manipulation. `mvn compile exec:java` is safe to run on a laptop, in CI, or in a container with no network access.
- **The vulnerable versions are pinned on purpose.** `pom.xml` deliberately uses old, CVE-bearing versions of log4j-core, commons-text, snakeyaml, jackson-databind, xstream, commons-collections, spring-core, guava, commons-beanutils, commons-fileupload, hibernate-validator, and bouncycastle. Do not copy these dependency versions into a real application, and do not deploy this repository as a service.
- **`AnyTypePermission.ANY`** in `LegacyOrderXmlImporter.java` explicitly disables XStream's default-deny type allowlist. This is here to demonstrate a real, common regression (teams reopen it after hitting a `ForbiddenClassException` post-upgrade), not as something to reuse.

If you're extending this repo with a new vulnerable-library example, keep the same rule: call the real, documented vulnerable API, on data that looks externally-sourced, but never wire in a payload that would actually need to reach outside the JVM to prove the point. Reachability analysis cares about the call graph, not about whether the demo data is weaponized.

# Security Policy

## Vessel is an educational project

Vessel is a didactic Java 21 micro-framework built to learn the mental model behind Spring Boot, not to run production workloads. It deliberately has **no authentication, no authorization, no persistence layer, and no security hardening beyond basic input validation** — see [`CLAUDE.md`](CLAUDE.md) for why those are permanent, out-of-scope decisions rather than missing features.

**Do not deploy Vessel-based applications to handle real user data or run in production.** If you're evaluating it for anything beyond learning or a toy project, assume it has not been hardened against the kinds of threats a production HTTP framework needs to withstand.

## Reporting a vulnerability

That said, real implementation bugs — a request that crashes the server, a routing or JSON-parsing flaw that behaves unsafely on malformed input, a dependency-injection edge case that leaks state between requests — are worth reporting and fixing, since they undermine the project's own goal of being a correct, well-understood implementation.

If you find one:

- **Preferred:** open a [GitHub Security Advisory](https://github.com/nadezhdkov/vessel/security/advisories/new) on the repository, so the report stays private until a fix is available.
- **Alternative:** email nadezhdkov@gmail.com with a description of the issue and, if possible, a minimal reproduction.

Please avoid filing security-sensitive reports as public GitHub issues.

## What to include

- The module affected (`vessel-core`, `vessel-http`, `vessel-web`, `vessel-config`, or `vessel-app`).
- Steps to reproduce, or a minimal failing test if you have one.
- What you expected to happen versus what actually happened.
- Whether you believe it's exploitable beyond a crash/DoS (e.g. it could lead to unintended code execution or data exposure) — this helps prioritize triage.

## Response expectations

This is a solo educational project maintained outside of any organization, so there's no SLA — but genuine reports will be acknowledged and looked at as soon as reasonably possible. There is no bug bounty program.

# AI-Hallucination Reviewer

Web tool that checks GitHub Java/Spring Boot repos for likely AI-generated code hallucinations. LLM output is treated as untrusted and verified with deterministic checks (e.g. Maven Central), then labeled VERIFIED, LIKELY, or NEEDS_HUMAN_REVIEW.

**Status: early MVP.**
- Working: public GitHub repo fetching, `pom.xml` parsing (including property placeholders), Maven Central verification of dependencies and versions, labeled findings with evidence, unit tests
- In progress: Gemini analysis of findings, and validation of the LLM's output before display

## The problem
AI coding assistants produce code that looks right but may depend on libraries, versions, or APIs that don't exist. Student developers often merge it without checking, and there is no quick way to verify it.

## How it works
1. The user submits a public GitHub repo URL (Java/Maven only).
2. The Spring Boot backend fetches `pom.xml` and selected files through the GitHub API (nothing is cloned or executed).
3. Deterministic checks run: each dependency and version is looked up on Maven Central.
4. Each finding gets a label and the evidence behind it:
   - **VERIFIED**: confirmed by a deterministic check
   - **LIKELY**: heuristic match
   - **NEEDS_HUMAN_REVIEW**: plausible but not checkable

## Design decisions
- **Report only, no auto-fix.** A fix from the same kind of model that hallucinated can add new errors, and running generated code adds security risk. A human makes the final call.
- **Treat LLM output as untrusted.** Every claim must pass a deterministic check or be labeled for human review.
- **No cloning or execution.** Files are fetched read-only through the GitHub API, with size limits.

## Known limitations
- A dependency missing from Maven Central may still be legitimate (custom repositories, modules of the same project). These findings should be read as "not found on Maven Central", not as proof of a hallucination.
- Java/Maven projects only; public repositories only.

## Run locally
```bash
mvn spring-boot:run
curl -X POST http://localhost:8080/api/scan \
  -H "Content-Type: application/json" \
  -d '{"repoUrl": "https://github.com/spring-projects/spring-petclinic"}'
```

## Out of scope
Private repositories, other languages, auto-fixing, CI/CD integration, user authentication.

# ai-hallucination-reviewer
Web tool that checks GitHub Java/Spring Boot repos for likely AI-generated code hallucinations. LLM output is treated as untrusted and verified with deterministic checks (e.g. Maven Central), then labeled VERIFIED, LIKELY, or NEEDS_HUMAN_REVIEW.

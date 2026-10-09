# DANASEA AI features with Quyet decisions

Scope approved in this chat: ten customer features, text classification/moderation and transaction risk signals. Keep Groq for generation; use the already downloaded Quyet Small for structured decisions. Florence is excluded. Backend APIs and assistant tools are the implementation scope.

- [x] Add the offline Quyet HTTP worker with fixed/versioned rubrics, bounded inputs, authentication and validated outputs; verify real local inference and failure paths.
- [x] Add clean read contracts for published catalog/options, current slot availability, review evidence and owned orders; prevent assistant reads from increasing view counters.
- [x] Implement natural language search, preference ranking, comparison and nearby discovery with hard filters/quotes in code and optional Quyet relevance decisions.
- [x] Implement stored itineraries and owner-checked re-planning with current inventory, price, travel-time and weather validation.
- [x] Implement source-backed review summaries and owned-order support; reuse Groq only for wording, with explicit source/fallback responses.
- [x] Add text/service classification, moderation suggestions and admin risk cases from backend-calculated signals; retain human review and financial invariants.
- [x] Connect customer features to the assistant, add configuration/Compose and update API documentation; retire Florence from the active proposal.
- [x] Run focused behavior/security/contract tests, full applicable backend tests, Python checks, migration/runtime smoke tests and document measured limits.

Done when the approved APIs are implemented and callable, critical ownership and hard-constraint paths are tested, and unsupported inputs/model failures return explicit states rather than invented data or automatic financial actions. Model quality is reported separately from implementation correctness.

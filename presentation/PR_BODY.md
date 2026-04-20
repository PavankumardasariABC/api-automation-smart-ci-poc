## Summary
- **Presentation website** (`presentation/index.html`) for VP and Indian GM demo: advantages, speed vs MT scripts, CI/CD & PR integration, failure categorization, future roadmap, Billing demo notes.
- **Billing Account Transfer API** automation (template): TDD test suite, data provider, test data JSON, template README; uses bearer token with auto-refresh.
- **CI**: PR runs default to Regression group; workflow comment updated.

## How to verify the website
1. After merge (or from this branch): open `presentation/index.html` in a browser, or run `cd presentation && python3 -m http.server 8080` and open http://localhost:8080
2. Walk through: Advantages → Speed → CI/CD → Failure categories → Future → Demo

## Ready for manager review
Please review and merge to `main` when approved.

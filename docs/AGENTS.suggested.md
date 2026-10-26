# Suggested AGENTS.md (IMP-08)

SPEC §19 says the user writes the agent rules file (`AGENTS.md` or its alternative name). This is a proposal; copy it to the repository root and edit as you like.

---

# Planejamento Mensal — agent rules

## Start of every session
1. Read `docs/IMPLEMENTATION_PLAN.md`: Progress line, Open questions, Decisions and the last Iteration log entries.
2. Resume the item marked `(in progress)`, or take the next item whose `after:` dependencies are all `[x]` and that is not `(blocked: …)`.
3. `docs/SPEC.md` is the only source of requirements. Never edit it without the user's approval (AGT-11).

## Iteration cycle (SPEC §19)
1. Mark the item `(in progress)`.
2. Write or adjust the tests first, then implement.
3. Run the full build:
   - backend: `cd backend && ./mvnw verify` (JDK 25 in `JAVA_HOME`; tests, Javadoc doclint, JaCoCo gates)
   - frontend: `cd frontend && npm run typecheck && npm run lint && npm run i18n:check && npm test && npm run build`
   - shell: `cd src-tauri && cargo test`
   - plan: `python3 scripts/check_plan_coverage.py`
4. When everything is green: mark `[x]` with the commit hash, run `python3 scripts/plan_progress.py`, and add an Iteration log entry (date, items, decisions, deviations).
5. One item, or a small group of related items, per commit.

## Rules
- Code, identifiers, Javadoc, tests and commit messages in English (I18N-01); the SPEC stays in Portuguese.
- Commit messages never mention AI tools.
- Ambiguity: do not invent rules. Add an Open question, mark the item `(blocked: Qn)`, and move on (AGT-10).
- Deviation from the SPEC: record it in Decisions with the reason (AGT-11).
- Layers per module: `api` → `application` → `domain`, with `infra` implementing ports; modules talk only through `application` (SOL-05, SOL-06, enforced by `LayerDependencyTest`).
- Test names: `@DisplayName("RN-13: behaviour")`, Given / When / Then (TST-02, TST-03).
- Javadoc cites rule ids ("Implements RN-04") instead of copying the rule text (DOC-03).
- No literal UI text in components: always `t('key')`, with keys in both `locales/*.json` (I18N-04, I18N-13).
- After changing the API, refresh the contract: `./mvnw test -Dtest=OpenApiContractIT -Dopenapi.update=true`, then `npm run api:generate` in `frontend/`.

## Real data
Real Itaú bills and spreadsheets stay outside the repository. Tests only use the masked fixtures in `backend/src/test/resources/fixtures/itau`. New fixtures must be masked the same way (see that folder's README).

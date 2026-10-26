"""AGT-08: every requirement id of docs/SPEC.md must have an item in docs/IMPLEMENTATION_PLAN.md.

Also checks that every dependency (`after: …`) points to an existing item. Exits with 1 on failure.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ID = r"\b(?:RF|RN|RNF|POR|API|UI|DOC|TST|TPL|I18N|XLS|AGT|SOL|IMP)-\d+[a-z]?\b"

spec_ids = set(re.findall(ID, (ROOT / "docs/SPEC.md").read_text(encoding="utf-8")))
plan = (ROOT / "docs/IMPLEMENTATION_PLAN.md").read_text(encoding="utf-8")
items = re.findall(r"^- \[[ x]\] (\S+) —", plan, re.M)
item_ids = set(items)

errors = []
errors += [f"SPEC id without plan item: {i}" for i in sorted(spec_ids - item_ids)]
errors += [f"duplicate plan item: {i}" for i in sorted({i for i in items if items.count(i) > 1})]
for line in plan.splitlines():
    if line.startswith("- [") and "after:" in line:
        deps = line.split("after:", 1)[1]
        errors += [f"unknown dependency {d} in: {line[:60]}…" for d in re.findall(ID, deps) if d not in item_ids]

if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Plan covers all {len(spec_ids)} SPEC ids with {len(items)} items")

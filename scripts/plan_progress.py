"""Recomputes the Progress line of docs/IMPLEMENTATION_PLAN.md from its checkboxes (AGT-07)."""
import re
import sys
from pathlib import Path

plan = Path(sys.argv[1] if len(sys.argv) > 1 else "docs/IMPLEMENTATION_PLAN.md")
text = plan.read_text(encoding="utf-8")
counts, done_total, all_total = [], 0, 0
for section in re.split(r"^## ", text, flags=re.M)[1:]:
    name = section.split()[0]
    if not re.fullmatch(r"M\d+", name):
        continue
    done = len(re.findall(r"^- \[x\]", section, re.M))
    total = len(re.findall(r"^- \[[ x]\]", section, re.M))
    done_total, all_total = done_total + done, all_total + total
    counts.append(f"{name} {done}/{total}")
line = "Progress: " + " · ".join(counts) + f" (total {done_total}/{all_total})"
plan.write_text(re.sub(r"^Progress: .*$", line, text, count=1, flags=re.M), encoding="utf-8")
print(line)

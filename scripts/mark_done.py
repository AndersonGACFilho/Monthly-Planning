"""Marks plan items as done with a note: python scripts/mark_done.py "ID=note" ... (AGT-07 helper)."""
import re
import sys
from pathlib import Path

plan = Path("docs/IMPLEMENTATION_PLAN.md")
text = plan.read_text(encoding="utf-8")
for arg in sys.argv[1:]:
    item, note = arg.split("=", 1)
    match = re.search(r"^- \[[ x]\] (" + re.escape(item) + r" — .*?)$", text, re.M)
    if not match:
        sys.exit(f"unknown item {item}")
    line = re.sub(r" · \((?:blocked|in progress|partially blocked)[^)]*\)", "", match.group(1))
    line = re.sub(r" · (?:ongoing|done \([^)]*\))$", "", line)
    text = text[: match.start()] + f"- [x] {line} · done ({note})" + text[match.end():]
plan.write_text(text, encoding="utf-8")

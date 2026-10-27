/**
 * Updates a selection after a click on a row, like a file manager: plain click selects only that row,
 * Ctrl/Cmd+click toggles it, Shift+click selects the range from the last clicked row (UI-04).
 *
 * @param current - selected ids
 * @param ids - ids of the rows in display order
 * @param clicked - id of the clicked row
 * @param anchor - id of the last clicked row, or `null`
 * @param modifiers - keys held during the click
 * @returns the new selection
 */
export function selectRows(
  current: ReadonlySet<number>,
  ids: readonly number[],
  clicked: number,
  anchor: number | null,
  modifiers: { ctrl: boolean; shift: boolean },
): Set<number> {
  if (modifiers.shift && anchor != null) {
    const from = ids.indexOf(anchor);
    const to = ids.indexOf(clicked);
    if (from >= 0 && to >= 0) {
      const [start, end] = from < to ? [from, to] : [to, from];
      return new Set([...current, ...ids.slice(start, end + 1)]);
    }
  }
  if (modifiers.ctrl) {
    const next = new Set(current);
    if (next.has(clicked)) next.delete(clicked);
    else next.add(clicked);
    return next;
  }
  return new Set([clicked]);
}

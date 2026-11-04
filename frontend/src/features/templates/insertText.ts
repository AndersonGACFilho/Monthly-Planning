/**
 * Inserts a snippet into a text at the selection of its field, replacing the selected text.
 *
 * @param value   - current text
 * @param snippet - text to insert
 * @param start   - selection start, or `null` to append at the end
 * @param end     - selection end, or `null` to use `start`
 * @returns the new text and where the caret goes, right after the snippet
 */
export function insertText(value: string, snippet: string, start: number | null, end: number | null): { value: string; caret: number } {
  const from = start ?? value.length;
  const to = end ?? from;
  return { value: value.slice(0, from) + snippet + value.slice(to), caret: from + snippet.length };
}

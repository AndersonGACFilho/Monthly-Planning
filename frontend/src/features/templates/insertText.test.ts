import { describe, expect, it } from 'vitest';
import { insertText } from './insertText';

describe('insertText', () => {
  it('TPL-07: puts the snippet at the caret and moves the caret after it', () => {
    expect(insertText('<div></div>', '{{header}}', 5, 5)).toEqual({ value: '<div>{{header}}</div>', caret: 15 });
  });

  it('TPL-07: replaces the selected text', () => {
    expect(insertText('a XX b', '{{total}}', 2, 4)).toEqual({ value: 'a {{total}} b', caret: 11 });
  });

  it('TPL-07: appends when the field has no selection', () => {
    expect(insertText('h1 {}', '\n.x {}', null, null)).toEqual({ value: 'h1 {}\n.x {}', caret: 11 });
  });
});

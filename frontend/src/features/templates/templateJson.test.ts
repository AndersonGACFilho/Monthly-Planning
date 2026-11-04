import { describe, expect, it } from 'vitest';
import type { TemplateSettings } from '../../api/generated/model';
import { applyTemplateJson, toTemplateJson } from './templateJson';

const current: TemplateSettings = {
  primaryColor: '#2563EB',
  secondaryColor: '#475569',
  font: 'NOTO_SANS',
  logo: 'data:image/png;base64,AAAA',
  blocks: ['SUMMARY', 'CARDS'],
  texts: { 'pt-BR': { header: 'Extrato', footer: 'Rodapé', empty: false } },
};

describe('templateJson', () => {
  it('IMP-61: exports everything but the logo', () => {
    const json = JSON.parse(toTemplateJson(current)) as Record<string, unknown>;
    expect(json).not.toHaveProperty('logo');
    expect(json).toMatchObject({ font: 'NOTO_SANS', blocks: ['SUMMARY', 'CARDS'], layoutHtml: null });
    expect(json.texts).toEqual({ 'pt-BR': { header: 'Extrato', footer: 'Rodapé' } });
  });

  it('IMP-61: applies a JSON answer inside a code block, keeps the logo and reports what it ignored', () => {
    const answer = [
      'Aqui está:',
      '```json',
      JSON.stringify({
        primaryColor: '#0f766e',
        font: 'COMIC',
        blocks: ['CARDS', 'SUMMARY', 'CARDS'],
        texts: { 'pt-BR': { header: 'Cobrança {{member.name}}', color: 'x' }, 'fr-FR': { header: 'x' } },
        layoutHtml: '<div>{{block.cards}}</div>',
        advancedCss: null,
        logo: 'data:image/png;base64,BBBB',
      }),
      '```',
    ].join('\n');
    const result = applyTemplateJson(answer, current);
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.settings).toMatchObject({
      primaryColor: '#0F766E',
      font: 'NOTO_SANS',
      logo: 'data:image/png;base64,AAAA',
      blocks: ['CARDS', 'SUMMARY'],
      layoutHtml: '<div>{{block.cards}}</div>',
      advancedCss: undefined,
    });
    expect(result.settings.texts?.['pt-BR']).toMatchObject({ header: 'Cobrança {{member.name}}', footer: 'Rodapé' });
    expect(result.ignored).toEqual(['font', 'texts.pt-BR.color', 'texts.fr-FR', 'logo']);
  });

  it('IMP-61: text without a JSON object is refused', () => {
    expect(applyTemplateJson('sem json aqui', current)).toEqual({ ok: false, error: 'invalidJson' });
    expect(applyTemplateJson('{ "a": ', current)).toEqual({ ok: false, error: 'invalidJson' });
    expect(applyTemplateJson('[1]', current)).toEqual({ ok: false, error: 'invalidJson' });
  });
});

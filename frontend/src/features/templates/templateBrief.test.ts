import { describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { buildTemplateBrief } from './templateBrief';

describe('buildTemplateBrief', () => {
  it('IMP-61: carries the answer format, the rules, every selector, the current template and the sample HTML', async () => {
    await i18n.changeLanguage('pt-BR');
    const brief = buildTemplateBrief(
      {
        reference: {
          standardLayout: '<div class="statement">{{header}}</div>',
          placeholders: ['header', 'block.cards'],
          variables: ['member.name'],
          selectors: [{ selector: '#block-cards', description: 'Bloco de cartões' }],
          rules: {
            htmlTags: ['div', 'table'],
            layoutTags: ['section'],
            htmlAttributes: ['class', 'style'],
            layoutAttributes: ['class', 'style', 'id'],
            forbiddenCss: ['@import'],
            maxCss: 20000,
            maxHeaderHtml: 20000,
            maxLayout: 50000,
            fonts: { NOTO_SANS: 'Noto Sans' },
            blocks: ['SUMMARY', 'CARDS'],
            languages: ['pt-BR', 'en-US'],
          },
        },
        settings: { primaryColor: '#2563EB', logo: 'data:image/png;base64,QUFB', blocks: ['CARDS'] },
        sampleHtml: '<html><img src="data:image/png;base64,QUFBQUFB"><h1>Extrato de Maria</h1></html>',
      },
      i18n.t,
    );
    expect(brief).toContain('"font": "NOTO_SANS"');
    expect(brief).toContain('"blocks": ["SUMMARY", "CARDS"]');
    expect(brief).toContain('`{{block.cards}}`');
    expect(brief).toContain('`{{member.name}}`');
    expect(brief).toContain('- `#block-cards`: Bloco de cartões');
    expect(brief).toContain('layoutHtml 50000');
    expect(brief).toContain('Não use flexbox');
    expect(brief).toContain('Extrato de Maria');
    expect(brief).not.toContain('QUFB');
    expect(brief.trim().endsWith('(Descreva aqui o estilo que você quer para o extrato.)')).toBe(true);
  });
});

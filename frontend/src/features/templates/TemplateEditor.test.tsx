import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { TemplateSettings } from '../../api/generated/model';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { TemplateEditor } from './TemplateEditor';

const base = 'http://127.0.0.1:9999';
const settings: TemplateSettings = {
  primaryColor: '#2563EB',
  secondaryColor: '#475569',
  font: 'NOTO_SANS',
  blocks: ['SUMMARY', 'CARDS'],
  texts: { 'pt-BR': { header: 'Extrato de {{member.name}}' } },
};

describe('TemplateEditor', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(http.get(`${base}/api/members`, () => HttpResponse.json({ _embedded: { members: [{ id: 2, code: 'M', name: 'Mãe' }] } })));
  });

  it('TPL-03, I18N-11: the live preview follows the edits, with the language and the fallback warning', async () => {
    const previews: Record<string, unknown>[] = [];
    server.use(
      http.post(`${base}/api/templates/preview`, async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        previews.push(body);
        return HttpResponse.json({ html: '<p>prévia</p>', subject: 'Assunto X', fallback: body.language === 'en-US' });
      }),
    );
    renderWithProviders(<TemplateEditor base={settings} period="2026-11" onDone={() => undefined} />);
    await waitFor(() => expect(previews.length).toBeGreaterThan(0));
    expect(previews[0]).toMatchObject({ billingPeriod: '2026-11', language: 'pt-BR' });
    fireEvent.change(screen.getByLabelText('Cabeçalho'), { target: { value: 'Olá {{member.name}}' } });
    fireEvent.change(screen.getByLabelText('Idioma da prévia'), { target: { value: 'en-US' } });
    expect(await screen.findByText(/Sem textos neste idioma/)).toBeInTheDocument();
    const last = previews.at(-1) as { settings: TemplateSettings; language: string };
    expect(last.language).toBe('en-US');
    expect((last.settings.texts as Record<string, { header: string }>)['pt-BR']!.header).toBe('Olá {{member.name}}');
    expect(screen.getByText('Assunto X')).toBeInTheDocument();
  }, 10_000);

  it('TPL-02: blocks can be turned off and reordered; saving creates the template', async () => {
    let created: Record<string, unknown> | undefined;
    server.use(
      http.post(`${base}/api/templates/preview`, () => HttpResponse.json({ html: '' })),
      http.post(`${base}/api/templates`, async ({ request }) => {
        created = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 7, ...created }, { status: 201 });
      }),
    );
    const done = vi.fn();
    renderWithProviders(<TemplateEditor base={settings} period="2026-11" onDone={done} />);
    fireEvent.click(screen.getAllByRole('button', { name: 'Subir' })[1]!);
    fireEvent.click(screen.getByRole('checkbox', { name: 'Observação' }));
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Moderno' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(done).toHaveBeenCalled());
    expect(created).toMatchObject({ name: 'Moderno', settings: { blocks: ['CARDS', 'SUMMARY', 'NOTE'] } });
  });
});

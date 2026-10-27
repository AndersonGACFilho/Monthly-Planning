import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { MembersSection } from './MembersSection';

const base = 'http://127.0.0.1:9999';
const member = (id: number, code: string, links: Record<string, { href: string }>) => ({
  id,
  code,
  name: `Nome ${code}`,
  kind: 'MAIN',
  color: '#2563EB',
  active: true,
  preferredLanguage: 'pt-BR',
  version: 0,
  _links: { self: { href: `${base}/api/members/${id}` }, ...links },
});

describe('MembersSection', () => {
  useMockApi();
  beforeEach(() => i18n.changeLanguage('pt-BR'));

  it('RF-01, API-09: lists members and offers delete only when the backend allows it', async () => {
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: {
            members: [
              member(1, 'AB', { inactivate: { href: `${base}/api/members/1` } }),
              member(2, 'M', { inactivate: { href: `${base}/api/members/2` }, delete: { href: `${base}/api/members/2` } }),
            ],
          },
        }),
      ),
    );
    renderWithProviders(<MembersSection />);
    expect(await screen.findByText('AB')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Excluir' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Inativar' })).toHaveLength(2);
  });

  it('RF-01: creating a member posts the form and validates the code', async () => {
    let posted: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/members`, () => HttpResponse.json({})),
      http.post(`${base}/api/members`, async ({ request }) => {
        posted = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(member(3, 'T', {}), { status: 201 });
      }),
    );
    renderWithProviders(<MembersSection />);
    fireEvent.click(screen.getByRole('button', { name: /Nova conta/ }));
    fireEvent.change(screen.getByLabelText('Sigla'), { target: { value: 'toolong' } });
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Tia' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    expect(await screen.findByText('De 1 a 4 letras maiúsculas ou dígitos.')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Sigla'), { target: { value: 't' } });
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'THIRD_PARTY' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(posted).toBeDefined());
    expect(posted).toMatchObject({ code: 'T', name: 'Tia', kind: 'THIRD_PARTY', preferredLanguage: 'pt-BR' });
  });
});

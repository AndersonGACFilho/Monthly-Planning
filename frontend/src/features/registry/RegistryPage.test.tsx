import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { RegistryPage } from './RegistryPage';

const base = 'http://127.0.0.1:9999';

describe('RegistryPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/members`, () => HttpResponse.json({})),
      http.get(`${base}/api/cards`, () => HttpResponse.json({})),
      http.get(`${base}/api/categories`, () => HttpResponse.json({})),
    );
  });

  it('RF-01, RF-02, RF-06: members, cards and categories are registered on one screen', async () => {
    renderWithProviders(<RegistryPage />);
    for (const title of ['Contas (cotas)', 'Cartões', 'Categorias']) {
      expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument();
    }
    expect(screen.getByText(/edite o apelido/)).toBeInTheDocument();
  });
});

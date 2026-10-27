import { readFileSync } from 'node:fs';
import { api } from './support';

const FIXTURES = new URL('../../backend/src/test/resources/fixtures/itau/', import.meta.url);

/**
 * Seeds the fresh end-to-end backend like a first real use: two members, the default categories, one card and the
 * two masked Itaú bills confirmed (October closed, November open).
 */
export default async function globalSetup(): Promise<void> {
  const client = await api();
  const json = async (path: string, data: unknown) => (await client.post(path, { data })).json() as Promise<{ id: number }>;
  const house = await json('/api/members', { code: 'AB', name: 'Casa', kind: 'MAIN', color: '#2563EB', preferredLanguage: 'pt-BR' });
  await json('/api/members', {
    code: 'M', name: 'Mãe', kind: 'THIRD_PARTY', color: '#DB2777', preferredLanguage: 'pt-BR', whatsappPhone: '+5551999990000',
  });
  await client.post('/api/categories/defaults', { data: { language: 'pt-BR' } });
  const card = await json('/api/cards', {
    nickname: 'Itaú Platinum', bank: 'ITAU', lastDigits: ['4102', '4090', '9103'], holderName: 'MARIANA S F SILVA',
    defaultMemberId: house.id, closingDay: 2, dueDay: 9,
  });
  for (const [file, period] of [['itau-closed-bill-2026-10.pdf', '2026-10'], ['itau-open-bill-2026-11.xlsx', '2026-11']] as const) {
    const batch = (await (
      await client.post('/api/imports', {
        multipart: {
          file: { name: file, mimeType: 'application/octet-stream', buffer: readFileSync(new URL(file, FIXTURES)) },
          cardId: String(card.id),
          billingPeriod: period,
        },
      })
    ).json()) as { id: number };
    await client.post(`/api/imports/${batch.id}/confirmation`);
  }
  await client.dispose();
}

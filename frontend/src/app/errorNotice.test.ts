import { beforeEach, describe, expect, it } from 'vitest';
import { ProblemError } from '../api/http';
import i18n from '../i18n/i18n';
import { describeError } from './errorNotice';

const problem = new ProblemError(
  { status: 409, title: 'Fatura já paga', detail: 'A fatura já está paga.', code: 'bill-already-paid' },
  { method: 'POST', url: '/api/cards/7/bills/2026-11/payment', requestId: 'abc123' },
);

describe('describeError', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('IMP-15: a problem shows the texts of the backend', () => {
    expect(describeError(problem, false)).toEqual({ kind: 'error', title: 'Fatura já paga', detail: 'A fatura já está paga.' });
  });

  it('RNF-11: in debug mode a problem also gives code, status, request id and address, and stays on screen', () => {
    const notice = describeError(problem, true);
    expect(notice.details).toBe('code: bill-already-paid\nstatus: 409\nrequest: abc123\nPOST /api/cards/7/bills/2026-11/payment');
    expect(notice.sticky).toBe(true);
  });

  it('RNF-11: only a failed connection is a network error; any other error is unexpected', () => {
    expect(describeError(new TypeError('Failed to fetch'), false).title).toBe('Não foi possível falar com o serviço local.');
    const bug = describeError(new Error('x is undefined'), true);
    expect(bug.title).toBe(i18n.t('error.unexpected'));
    expect(bug.details).toContain('Error: x is undefined');
  });
});

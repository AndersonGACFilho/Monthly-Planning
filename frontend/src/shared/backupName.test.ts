import { describe, expect, it } from 'vitest';
import { backupLabel, parseBackupName } from './backupName';

describe('backupName', () => {
  it('IMP-58: reads the date and the reason of a backup made by the app', () => {
    expect(parseBackupName('planejamento-mensal-2026-11-04T10-16-09-178-before-restore.db')).toEqual({
      date: new Date(2026, 10, 4, 10, 16, 9),
      reason: 'before-restore',
    });
    expect(backupLabel('planejamento-mensal-2026-11-04T10-16-09-178-start.db', 'pt-BR')).toBe('04/11/2026, 10:16');
  });

  it('IMP-72: an imported database file is listed as imported', () => {
    expect(parseBackupName('planejamento-mensal-2026-11-13T10-44-50-123-imported.db').reason).toBe('imported');
  });

  it('IMP-58: any other name is shown as it is', () => {
    expect(parseBackupName('copy.db')).toEqual({ date: null, reason: 'other' });
    expect(backupLabel('copy.db', 'pt-BR')).toBe('copy.db');
  });
});

import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as desktop from '../desktop';
import i18n from '../../i18n/i18n';
import { useNotifications } from '../state/notifications';
import { EXPORT_COOLDOWN_MS, useFileExport } from './useFileExport';

describe('useFileExport', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    vi.useFakeTimers();
    useNotifications.setState({ notices: [] });
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('IMP-62: saves the file, says so and keeps the buttons resting for a second; a second click meanwhile is ignored', async () => {
    const saved = vi.spyOn(desktop, 'downloadFile').mockImplementation(() => undefined);
    const fetchFile = vi.fn().mockResolvedValue({ blob: new Blob(['x']), fileName: 'extrato.pdf' });
    const { result } = renderHook(() => useFileExport());

    let first = false;
    await act(async () => {
      first = await result.current.run(fetchFile, 'fallback.pdf');
    });
    expect(first).toBe(true);
    expect(saved).toHaveBeenCalledWith(expect.any(Blob), 'extrato.pdf');
    expect(useNotifications.getState().notices.map((n) => n.title)).toContain('Exportação concluída: extrato.pdf');
    expect(result.current.busy).toBe(true);

    await act(async () => {
      expect(await result.current.run(fetchFile, 'fallback.pdf')).toBe(false);
    });
    expect(fetchFile).toHaveBeenCalledTimes(1);

    act(() => vi.advanceTimersByTime(EXPORT_COOLDOWN_MS));
    expect(result.current.busy).toBe(false);
  });

  it('IMP-62: an error is reported and the buttons still rest before the next try', async () => {
    vi.spyOn(desktop, 'downloadFile').mockImplementation(() => undefined);
    const { result } = renderHook(() => useFileExport());
    let saved = true;
    await act(async () => {
      saved = await result.current.run(() => Promise.reject(new Error('falhou')), 'x.pdf');
    });
    expect(saved).toBe(false);
    expect(result.current.busy).toBe(true);
    act(() => vi.advanceTimersByTime(EXPORT_COOLDOWN_MS));
    expect(result.current.busy).toBe(false);
  });
});

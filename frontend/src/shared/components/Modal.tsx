import { X } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';

/** Props of {@link Modal}. */
export interface ModalProps {
  /** Title, also the accessible name. */
  title: string;
  /** Called when the user closes the modal (button, Escape or backdrop). */
  onClose: () => void;
  /** Content. */
  children: ReactNode;
}

/**
 * Modal dialog over the screen: focuses its first field, closes with Escape or a click outside.
 *
 * @param props - {@link ModalProps}
 * @returns the dialog
 */
export function Modal({ title, onClose, children }: ModalProps) {
  const { t } = useTranslation();
  const box = useRef<HTMLDivElement>(null);
  useEffect(() => {
    box.current?.querySelector<HTMLElement>('input, select, textarea')?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onClose();
      }
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [onClose]);
  return createPortal(
    // Key presses stay inside: the modal may be rendered within a row that reacts to Enter (RF-78)
    <div
      onKeyDown={(event) => event.stopPropagation()}
      className="fixed inset-0 z-50 flex items-start justify-center overflow-auto bg-black/40 p-4 pt-16"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div ref={box} role="dialog" aria-modal="true" aria-label={title} className="w-full max-w-3xl rounded-md bg-(--color-surface) shadow-xl">
        <div className="flex items-center justify-between border-b border-(--color-border) px-4 py-2">
          <h2 className="font-semibold">{title}</h2>
          <button type="button" className="icon-button" aria-label={t('action.close')} onClick={onClose}>
            <X size={16} aria-hidden />
          </button>
        </div>
        <div className="p-2">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

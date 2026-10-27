import { FileUp } from 'lucide-react';
import { useRef, useState, type DragEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import { previewImport } from '../../api/generated/endpoints/import-controller/import-controller';
import type { CardModel, ImportModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { embedded } from '../../shared/hal';
import { useBillingPeriod } from '../../shared/state/billingPeriod';

/** Props of {@link ImportUpload}. */
export interface ImportUploadProps {
  /** Called with the preview returned by the backend. */
  onPreview: (batch: ImportModel) => void;
}

/**
 * Chooses card, billing period and file, by drag and drop or the file dialog (RF-60, RF-61, UI-06).
 *
 * @param props - {@link ImportUploadProps}
 * @returns the upload form
 */
export function ImportUpload({ onPreview }: ImportUploadProps) {
  const { t } = useTranslation();
  const globalPeriod = useBillingPeriod();
  const { data } = useCards({ size: 200, sort: ['nickname,asc'] });
  const cards = embedded<CardModel>(data, 'cards').filter((c) => c.active && c.primaryCardId == null);
  const [cardId, setCardId] = useState<number | null>(null);
  const [period, setPeriod] = useState(globalPeriod);
  const [dragging, setDragging] = useState(false);
  const [busy, setBusy] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const selectedCard = cardId ?? cards[0]?.id ?? null;

  const send = async (file: File | undefined) => {
    if (!file || selectedCard == null) return;
    setBusy(true);
    try {
      onPreview(await previewImport({ cardId: selectedCard, billingPeriod: period }, { file }));
    } catch (error) {
      reportError(error);
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  };

  const onDrop = (event: DragEvent) => {
    event.preventDefault();
    setDragging(false);
    void send(event.dataTransfer.files[0]);
  };

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="import-upload-title">
      <h2 id="import-upload-title" className="text-lg font-semibold">
        {t('import.newImport')}
      </h2>
      <div className="flex flex-wrap gap-3">
        <Field label={t('import.card')} htmlFor="import-card">
          <select id="import-card" className="input" value={selectedCard ?? ''} onChange={(e) => setCardId(Number(e.target.value))}>
            {cards.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nickname} ({c.lastDigits?.join(', ')})
              </option>
            ))}
          </select>
        </Field>
        <Field label={t('period.label')} htmlFor="import-period">
          <input id="import-period" type="month" className="input" value={period} onChange={(e) => setPeriod(e.target.value)} />
        </Field>
      </div>
      {cards.length === 0 ? (
        <p className="text-(--color-text-muted)">{t('import.noCards')}</p>
      ) : (
        <div
          role="button"
          tabIndex={0}
          aria-busy={busy}
          aria-label={t('import.dropHint')}
          onClick={() => input.current?.click()}
          onKeyDown={(event) => (event.key === 'Enter' || event.key === ' ') && input.current?.click()}
          onDragOver={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          className={`flex cursor-pointer flex-col items-center gap-2 rounded border-2 border-dashed p-8 ${
            dragging ? 'border-(--color-accent) bg-(--color-surface-muted)' : 'border-(--color-border)'
          }`}
        >
          <FileUp size={28} aria-hidden />
          <span>{busy ? t('app.loading') : t('import.dropHint')}</span>
          <span className="text-sm text-(--color-text-muted)">{t('import.formats')}</span>
          <input
            ref={input}
            type="file"
            accept=".pdf,.xlsx"
            className="hidden"
            data-testid="import-file"
            onChange={(event) => void send(event.target.files?.[0])}
          />
        </div>
      )}
    </section>
  );
}

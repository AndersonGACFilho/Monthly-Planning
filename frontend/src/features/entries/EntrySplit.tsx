import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { editEntry } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import type { BillEntryModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { SplitEditor } from '../../shared/components/SplitEditor';
import { useSaved } from '../../shared/hooks/useSaved';
import { remainderOf, sharesOf, type Split, splitOf } from '../../shared/split';

/**
 * Member split of an entry's purchase, by percentage or by amount per installment, saved for every
 * installment (RF-26, RF-43, RN-08).
 *
 * @param props - the entry
 * @returns the split section
 */
export function EntrySplit({ entry }: { entry: BillEntryModel }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const [split, setSplit] = useState<Split>(() => splitOf(entry.splits ?? []));
  const [dirty, setDirty] = useState(false);
  const complete = remainderOf(split, entry.amount ?? 0) === 0 && split.shares.every((s) => s.memberId != null);

  const save = async () => {
    try {
      await editEntry(entry.id!, { splits: sharesOf(split), clearCategory: false }, ifMatch(entry.version));
      setDirty(false);
      await saved();
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <section className="mt-2 flex flex-col gap-1" aria-label={t('entries.split')}>
      <h3 className="font-medium">{t('entries.split')}</h3>
      <SplitEditor
        idPrefix={`entry-${entry.id}`}
        value={split}
        total={entry.amount ?? 0}
        onChange={(next) => {
          setSplit(next);
          setDirty(true);
        }}
      />
      {dirty && (
        <button type="button" className="button button-primary self-start" disabled={!complete} onClick={() => void save()}>
          {t('entries.saveSplit')}
        </button>
      )}
    </section>
  );
}

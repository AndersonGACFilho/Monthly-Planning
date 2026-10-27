import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useImports, importBatch as loadBatch } from '../../api/generated/endpoints/import-controller/import-controller';
import type { ImportModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { MoneyText } from '../../shared/components/MoneyText';
import { embedded } from '../../shared/hal';
import { ImportPreview } from './ImportPreview';
import { ImportUpload } from './ImportUpload';

/**
 * Tab 5, import of Itaú bills: upload, preview, confirmation and history (SPEC section 8).
 *
 * @returns the page
 */
export function ImportPage() {
  const { t, i18n } = useTranslation();
  const [batch, setBatch] = useState<ImportModel | null>(null);
  const { data, refetch } = useImports({ size: 20 });
  const history = embedded<ImportModel>(data, 'imports');

  const open = async (id: number) => {
    try {
      setBatch(await loadBatch(id));
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">{t('nav.import')}</h1>
      {batch ? (
        <ImportPreview
          batch={batch}
          onChange={(next) => {
            setBatch(next);
            void refetch();
          }}
        />
      ) : (
        <ImportUpload onPreview={setBatch} />
      )}
      {batch && (
        <button type="button" className="button self-start" onClick={() => setBatch(null)}>
          {t('import.newImport')}
        </button>
      )}
      <section aria-labelledby="import-history-title">
        <h2 id="import-history-title" className="mb-2 text-lg font-semibold">
          {t('import.history')}
        </h2>
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-(--color-border) text-left text-(--color-text-muted)">
              <th className="px-2 py-1">{t('import.file')}</th>
              <th className="px-2 py-1">{t('period.label')}</th>
              <th className="px-2 py-1">{t('common.status')}</th>
              <th className="px-2 py-1 text-right">{t('import.computedTotal')}</th>
              <th className="hidden px-2 py-1 wide:table-cell">{t('import.when')}</th>
            </tr>
          </thead>
          <tbody>
            {history.map((item) => (
              <tr key={item.id} className="border-b border-(--color-border) hover:bg-(--color-surface-muted)">
                <td className="px-2 py-1">
                  <button type="button" className="text-(--color-accent) underline" onClick={() => void open(item.id!)}>
                    {item.fileName}
                  </button>
                </td>
                <td className="px-2 py-1">{item.billingPeriod}</td>
                <td className="px-2 py-1">{t(`import.status.${item.status}`)}</td>
                <td className="px-2 py-1 text-right">
                  <MoneyText value={item.computedTotal} />
                </td>
                <td className="hidden px-2 py-1 wide:table-cell">
                  {item.importedAt ? new Date(item.importedAt).toLocaleString(i18n.language) : ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

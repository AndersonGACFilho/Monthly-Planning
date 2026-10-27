import { useQuery } from '@tanstack/react-query';
import { Copy, FileDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useMemberStatement } from '../../api/generated/endpoints/statement-controller/statement-controller';
import type { MemberStatementParams } from '../../api/generated/model';
import { apiFetchBlob, apiFetchFile } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { MoneyText } from '../../shared/components/MoneyText';
import { copyImage, downloadFile } from '../../shared/desktop';
import { linkOf } from '../../shared/hal';
import { useNotifications } from '../../shared/state/notifications';

/** Props of {@link StatementPreview}. */
export interface StatementPreviewProps {
  /** Billing period. */
  period: string;
  /** Member shown. */
  memberId: number;
  /** Export options, language, template and note. */
  params: MemberStatementParams;
}

const FORMATS = ['pdf', 'png', 'xlsx', 'csv'] as const;

/**
 * Preview of one member statement before sending or downloading, with the pending warning and the downloads
 * (RF-79b, RF-84, RF-85).
 *
 * @param props - {@link StatementPreviewProps}
 * @returns the preview
 */
export function StatementPreview({ period, memberId, params }: StatementPreviewProps) {
  const { t } = useTranslation();
  const push = useNotifications((state) => state.push);
  const { data: statement } = useMemberStatement(period, memberId, params);
  const previewHref = linkOf(statement, 'preview');
  const { data: html } = useQuery({
    queryKey: ['statement-html', previewHref],
    queryFn: async () => (await apiFetchBlob(previewHref!)).text(),
    enabled: Boolean(previewHref),
  });
  const pending = statement?.pendingItems?.length ?? 0;

  const download = async (format: (typeof FORMATS)[number]) => {
    const href = linkOf(statement, format);
    if (!href) return;
    try {
      const file = await apiFetchFile(href);
      downloadFile(file.blob, file.fileName ?? `${statement?.memberCode ?? memberId}-${period}.${format}`);
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      {pending > 0 && (
        <p className="panel flex flex-wrap items-center justify-between gap-2 text-amber-800" role="status">
          {t('statements.pending', { count: pending })}
          <Link className="button" to={`/review-queue?billingPeriod=${period}`}>
            {t('statements.validateFirst')}
          </Link>
        </p>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm">
          {statement?.memberCode} — <MoneyText className="font-semibold" value={statement?.total} />
        </span>
        <span className="ml-auto flex flex-wrap gap-2">
          {FORMATS.map((format) => (
            <button key={format} type="button" className="button" disabled={!linkOf(statement, format)} onClick={() => void download(format)}>
              <FileDown size={14} aria-hidden /> {format.toUpperCase()}
            </button>
          ))}
          <button
            type="button"
            className="button"
            disabled={!linkOf(statement, 'png')}
            onClick={async () => {
              try {
                await copyImage(await apiFetchBlob(linkOf(statement, 'png')!));
                push({ kind: 'success', title: t('statements.imageCopied') });
              } catch (error) {
                reportError(error);
              }
            }}
          >
            <Copy size={14} aria-hidden /> {t('statements.copyImage')}
          </button>
        </span>
      </div>
      <iframe
        title={t('statements.previewFrame')}
        className="h-[70vh] w-full rounded border border-(--color-border) bg-white"
        sandbox=""
        srcDoc={html ?? ''}
      />
    </div>
  );
}

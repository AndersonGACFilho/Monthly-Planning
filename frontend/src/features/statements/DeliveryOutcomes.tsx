import { Check, FolderOpen, MessageCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { markDeliverySent } from '../../api/generated/endpoints/statement-delivery-controller/statement-delivery-controller';
import type { DeliveryOutcomeModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { openExternal, revealFile } from '../../shared/desktop';
import { useMemberMap } from '../../shared/hooks/useLookups';
import { useSaved } from '../../shared/hooks/useSaved';

/**
 * Status of each member after a batch delivery, with the follow-up actions: open WhatsApp, show the saved file
 * and mark as sent (RF-87, RF-89, RF-91, SOL-03).
 *
 * @param props - outcomes of the last delivery
 * @returns the list
 */
export function DeliveryOutcomes({ outcomes }: { outcomes: DeliveryOutcomeModel[] }) {
  const { t } = useTranslation();
  const members = useMemberMap();
  const saved = useSaved();
  if (outcomes.length === 0) return null;
  return (
    <section className="panel flex flex-col gap-2" aria-labelledby="outcomes-title">
      <h2 id="outcomes-title" className="font-semibold">
        {t('statements.outcomes')}
      </h2>
      <ul className="flex flex-col divide-y divide-(--color-border)">
        {outcomes.map((o) => (
          <li key={o.deliveryId} className="flex flex-wrap items-center gap-2 py-2">
            <span className="min-w-24 font-medium">{members.get(o.memberId ?? 0)?.code}</span>
            <span className={`badge ${o.status === 'SENT' ? 'badge-success' : o.status === 'FAILED' ? 'text-red-700' : 'badge-warning'}`}>
              {t(`statements.status.${o.status ?? 'FAILED'}`)}
            </span>
            {o.status === 'FAILED' && <span className="text-sm text-red-700">{o.detail}</span>}
            <span className="ml-auto flex gap-2">
              {o.action && (
                <button type="button" className="button" onClick={() => void openExternal(o.action!)}>
                  <MessageCircle size={14} aria-hidden /> {t('statements.openWhatsapp')}
                </button>
              )}
              {o.detail && o.status !== 'FAILED' && (
                <button type="button" className="button" onClick={() => void revealFile(o.detail!)}>
                  <FolderOpen size={14} aria-hidden /> {t('statements.showFile')}
                </button>
              )}
              {o.status === 'AWAITING_USER' && o.deliveryId !== undefined && (
                <button
                  type="button"
                  className="button"
                  onClick={async () => {
                    try {
                      await markDeliverySent(o.deliveryId!);
                      await saved();
                    } catch (error) {
                      reportError(error);
                    }
                  }}
                >
                  <Check size={14} aria-hidden /> {t('statements.markSent')}
                </button>
              )}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

import { Mail, MessageCircle, Printer } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import { sendStatements } from '../../api/generated/endpoints/statement-delivery-controller/statement-delivery-controller';
import { useTemplates } from '../../api/generated/endpoints/template-controller/template-controller';
import type {
  DeliveryOutcomeModel,
  MemberModel,
  MemberStatementParams,
  SendStatementsRequestChannel,
  TemplateModel,
} from '../../api/generated/model';
import { apiFetchBlob, ProblemError } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { copyImage, openFile } from '../../shared/desktop';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { DeliveryHistory } from './DeliveryHistory';
import { DeliveryOutcomes } from './DeliveryOutcomes';
import { StatementPreview } from './StatementPreview';

const CHANNELS: SendStatementsRequestChannel[] = ['EMAIL', 'WHATSAPP', 'CLIPBOARD', 'FOLDER'];

/**
 * Member statements of the billing period: choose members and options, preview, download, send through a
 * channel or print, and follow the history (RF-81 to RF-93).
 *
 * @returns the page
 */
export function StatementsPage() {
  const { t } = useTranslation();
  const period = useBillingPeriod();
  const saved = useSaved();
  const { data: memberPage } = useMembers({ size: 200, sort: ['code,asc'] });
  const { data: templatePage } = useTemplates();
  const members = embedded<MemberModel>(memberPage, 'members').filter((m) => m.active);
  const templates = embedded<TemplateModel>(templatePage, 'templates');
  const [selected, setSelected] = useState<number[]>([]);
  const [previewId, setPreviewId] = useState<number | null>(null);
  const [showCategories, setShowCategories] = useState(true);
  const [useAlias, setUseAlias] = useState(true);
  const [includeFuture, setIncludeFuture] = useState(true);
  const [language, setLanguage] = useState('');
  const [templateId, setTemplateId] = useState<number | null>(null);
  const [note, setNote] = useState('');
  const [outcomes, setOutcomes] = useState<DeliveryOutcomeModel[]>([]);
  const [busy, setBusy] = useState(false);

  const shown = previewId != null && selected.includes(previewId) ? previewId : selected[0];
  const params: MemberStatementParams = {
    showCategories,
    useAlias,
    includeFuture,
    ...(language ? { language } : {}),
    ...(templateId != null ? { template: templateId } : {}),
    ...(note.trim() ? { note: note.trim() } : {}),
  };
  const toggle = (id: number) => setSelected((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]));

  const send = async (channel: SendStatementsRequestChannel, sendAnyway = false): Promise<DeliveryOutcomeModel[]> => {
    try {
      return await sendStatements({
        billingPeriod: period,
        memberIds: selected,
        channel,
        showCategories,
        useAlias,
        includeFuture,
        language: language ? (language as 'pt-BR' | 'en-US') : undefined,
        templateId: templateId ?? undefined,
        note: note.trim() || undefined,
        sendAnyway,
      });
    } catch (error) {
      if (!sendAnyway && error instanceof ProblemError && error.problem.code === 'statement-has-pending') {
        const count = /\d+/.exec(error.problem.detail ?? '')?.[0] ?? '';
        if (window.confirm(t('statements.sendAnyway', { count }))) return send(channel, true);
        return [];
      }
      throw error;
    }
  };

  const run = async (channel: SendStatementsRequestChannel, after?: (list: DeliveryOutcomeModel[]) => Promise<void>) => {
    setBusy(true);
    try {
      const list = await send(channel);
      setOutcomes(list);
      if (after) await after(list);
      await saved();
    } catch (error) {
      reportError(error);
    } finally {
      setBusy(false);
    }
  };

  const copyFirstImage = async (list: DeliveryOutcomeModel[]) => {
    const first = list.find((o) => o.status === 'AWAITING_USER');
    if (!first) return;
    const png = await apiFetchBlob(`/api/statements/${period}/members/${first.memberId}/png?showCategories=${showCategories}&useAlias=${useAlias}&includeFuture=${includeFuture}${language ? `&language=${language}` : ''}${templateId != null ? `&template=${templateId}` : ''}`);
    await copyImage(png);
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">{t('nav.deliveries')}</h1>
      <div className="grid gap-4 wide:grid-cols-[320px_1fr]">
        <div className="flex flex-col gap-4">
          <fieldset className="panel flex flex-col gap-1">
            <legend className="font-semibold">{t('statements.members')}</legend>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={members.length > 0 && selected.length === members.length}
                onChange={(e) => setSelected(e.target.checked ? members.map((m) => m.id ?? 0) : [])}
              />
              {t('statements.selectAll')}
            </label>
            {members.map((m) => (
              <label key={m.id} className="flex items-center gap-2">
                <input type="checkbox" checked={selected.includes(m.id ?? 0)} onChange={() => toggle(m.id ?? 0)} />
                <span className="size-3 rounded-full" style={{ background: m.color }} aria-hidden />
                <span>
                  {m.code} — {m.name}
                </span>
                <span className="ml-auto text-xs text-(--color-text-muted)">
                  {!m.email && t('statements.noEmail')} {!m.whatsappPhone && t('statements.noPhone')}
                </span>
              </label>
            ))}
          </fieldset>
          <fieldset className="panel flex flex-col gap-2">
            <legend className="font-semibold">{t('statements.options')}</legend>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={showCategories} onChange={(e) => setShowCategories(e.target.checked)} />
              {t('statements.showCategories')}
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={useAlias} onChange={(e) => setUseAlias(e.target.checked)} />
              {t('statements.useAlias')}
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={includeFuture} onChange={(e) => setIncludeFuture(e.target.checked)} />
              {t('statements.includeFuture')}
            </label>
            <Field label={t('statements.language')} htmlFor="statement-language">
              <select id="statement-language" className="input" value={language} onChange={(e) => setLanguage(e.target.value)}>
                <option value="">{t('statements.memberLanguage')}</option>
                <option value="pt-BR">{t('language.pt-BR')}</option>
                <option value="en-US">{t('language.en-US')}</option>
              </select>
            </Field>
            <Field label={t('statements.template')} htmlFor="statement-template">
              <select
                id="statement-template"
                className="input"
                value={templateId ?? ''}
                onChange={(e) => setTemplateId(e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">{t('statements.memberTemplate')}</option>
                {templates.map((tpl) => (
                  <option key={tpl.id} value={tpl.id}>
                    {tpl.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={t('statements.note')} htmlFor="statement-note">
              <textarea id="statement-note" className="input min-h-16" value={note} maxLength={1000} onChange={(e) => setNote(e.target.value)} />
            </Field>
          </fieldset>
          <div className="panel flex flex-col gap-2" role="group" aria-label={t('statements.send')}>
            <span className="font-semibold">{t('statements.send')}</span>
            {CHANNELS.map((channel) => (
              <button
                key={channel}
                type="button"
                className={`button ${channel === 'EMAIL' ? 'button-primary' : ''}`}
                disabled={busy || selected.length === 0}
                onClick={() => void run(channel, channel === 'WHATSAPP' || channel === 'CLIPBOARD' ? copyFirstImage : undefined)}
              >
                {channel === 'EMAIL' && <Mail size={14} aria-hidden />}
                {channel === 'WHATSAPP' && <MessageCircle size={14} aria-hidden />} {t(`statements.channels.${channel}`)}
              </button>
            ))}
            <button
              type="button"
              className="button"
              disabled={busy || selected.length === 0}
              onClick={() =>
                void run('FOLDER', async (list) => {
                  for (const o of list) if (o.status === 'SENT' && o.detail) await openFile(o.detail);
                })
              }
            >
              <Printer size={14} aria-hidden /> {t('statements.print')}
            </button>
          </div>
        </div>
        <section className="flex flex-col gap-3" aria-labelledby="preview-title">
          <div className="flex flex-wrap items-center gap-2">
            <h2 id="preview-title" className="text-lg font-semibold">
              {t('statements.preview')}
            </h2>
            {selected.length > 1 && (
              <select
                className="input w-auto"
                aria-label={t('statements.previewOf')}
                value={shown}
                onChange={(e) => setPreviewId(Number(e.target.value))}
              >
                {selected.map((id) => (
                  <option key={id} value={id}>
                    {members.find((m) => m.id === id)?.code}
                  </option>
                ))}
              </select>
            )}
          </div>
          {shown === undefined ? (
            <p className="panel text-(--color-text-muted)">{t('statements.noSelection')}</p>
          ) : (
            <StatementPreview period={period} memberId={shown} params={params} />
          )}
          <DeliveryOutcomes outcomes={outcomes} />
        </section>
      </div>
      <DeliveryHistory period={period} />
    </div>
  );
}

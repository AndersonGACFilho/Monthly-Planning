import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import {
  assignTemplate,
  useTemplateAssignments,
  useTemplates,
  useTemplateVersions,
} from '../../api/generated/endpoints/template-controller/template-controller';
import type { MemberModel, TemplateModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { embedded } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';
import { useSaved } from '../../shared/hooks/useSaved';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { TemplateEditor } from './TemplateEditor';

/**
 * Statement templates: saved templates with the default one, versions, per-member choice and the editor
 * (TPL-02 to TPL-07).
 *
 * @returns the page
 */
export function TemplatesPage() {
  const { t } = useTranslation();
  const period = useBillingPeriod();
  const saved = useSaved();
  const { data } = useTemplates();
  const { data: assignments } = useTemplateAssignments();
  const { data: memberPage } = useMembers({ size: 200, sort: ['code,asc'] });
  const templates = embedded<TemplateModel>(data, 'templates');
  const members = embedded<MemberModel>(memberPage, 'members').filter((m) => m.active);
  const [editing, setEditing] = useState<TemplateModel | 'new' | null>(null);
  const standard = templates.find((tpl) => tpl.isDefault) ?? templates[0];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">{t('nav.templates')}</h1>
        <button type="button" className="button button-primary" disabled={!standard} onClick={() => setEditing('new')}>
          <Plus size={16} aria-hidden /> {t('templates.new')}
        </button>
      </div>
      {editing && standard?.settings && (
        <TemplateEditor
          key={editing === 'new' ? 'new' : editing.id}
          template={editing === 'new' ? undefined : editing}
          base={standard.settings}
          period={period}
          onDone={() => setEditing(null)}
        />
      )}
      <section className="flex flex-col gap-2" aria-labelledby="templates-list-title">
        <h2 id="templates-list-title" className="text-lg font-semibold">
          {t('templates.list')}
        </h2>
        <ul className="flex flex-col gap-2">
          {templates.map((tpl) => (
            <TemplateRow key={tpl.id} template={tpl} onEdit={() => setEditing(tpl)} />
          ))}
        </ul>
      </section>
      <section className="flex flex-col gap-2" aria-labelledby="assignments-title">
        <h2 id="assignments-title" className="text-lg font-semibold">
          {t('templates.assignments')}
        </h2>
        <div className="grid gap-2 sm:grid-cols-2 wide:grid-cols-3">
          {members.map((m) => (
            <label key={m.id} className="panel flex items-center justify-between gap-2" htmlFor={`assign-${m.id}`}>
              <span>
                {m.code} — {m.name}
              </span>
              <select
                id={`assign-${m.id}`}
                className="input w-auto"
                value={(assignments as Record<string, number> | undefined)?.[String(m.id)] ?? ''}
                onChange={async (e) => {
                  try {
                    await assignTemplate(m.id ?? 0, { templateId: e.target.value ? Number(e.target.value) : undefined });
                    await saved();
                  } catch (error) {
                    reportError(error);
                  }
                }}
              >
                <option value="">{t('templates.useDefault')}</option>
                {templates.map((tpl) => (
                  <option key={tpl.id} value={tpl.id}>
                    {tpl.name}
                  </option>
                ))}
              </select>
            </label>
          ))}
        </div>
      </section>
    </div>
  );
}

function TemplateRow({ template, onEdit }: { template: TemplateModel; onEdit: () => void }) {
  const { t, i18n } = useTranslation();
  const makeDefault = useAction(template, 'make-default');
  const [showVersions, setShowVersions] = useState(false);
  const { data: versions } = useTemplateVersions(template.id ?? 0, { query: { enabled: showVersions } });
  const date = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'short', timeStyle: 'short' });
  return (
    <li className="panel flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="size-4 rounded" style={{ background: template.settings?.primaryColor }} aria-hidden />
        <span className="font-semibold">{template.name}</span>
        {template.isDefault && <span className="badge badge-success">{t('templates.isDefault')}</span>}
        <span className="text-xs text-(--color-text-muted)">{t('templates.version', { number: template.currentVersion })}</span>
        <span className="ml-auto flex gap-2">
          <button type="button" className="button" onClick={() => setShowVersions((v) => !v)}>
            {t('templates.versions')}
          </button>
          {makeDefault.available && (
            <button type="button" className="button" onClick={() => makeDefault.mutate({ method: 'POST' })}>
              {t('templates.makeDefault')}
            </button>
          )}
          <button type="button" className="button" onClick={onEdit}>
            {t('action.edit')}
          </button>
          <DeleteButton resource={template} />
        </span>
      </div>
      {showVersions && (
        <ul className="text-sm text-(--color-text-muted)">
          {(versions ?? []).map((v) => (
            <li key={v.number}>
              {t('templates.version', { number: v.number })} · {v.createdAt ? date.format(new Date(v.createdAt)) : ''}
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}

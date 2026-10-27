import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import type { MemberModel } from '../../api/generated/model';
import { FilterableList } from '../../shared/components/FilterableList';
import { embedded } from '../../shared/hal';
import { ActiveToggle, DeleteButton } from '../../shared/components/ResourceActions';
import { MemberEditor } from './MemberForm';

/**
 * Members registry (RF-01, RF-20): list, create, edit, inactivate and delete when allowed.
 *
 * @returns the section
 */
export function MembersSection() {
  const { t } = useTranslation();
  const { data } = useMembers({ size: 200, sort: ['code,asc'] });
  const members = embedded<MemberModel>(data, 'members');
  const [editing, setEditing] = useState<MemberModel | 'new' | null>(null);

  return (
    <section className="flex flex-col gap-3" aria-labelledby="members-title">
      <div className="flex items-center justify-between">
        <h2 id="members-title" className="text-lg font-semibold">
          {t('member.title')}
        </h2>
        <button type="button" className="button" onClick={() => setEditing('new')}>
          <Plus size={16} aria-hidden /> {t('member.new')}
        </button>
      </div>
      {editing && (
        <MemberEditor
          key={editing === 'new' ? 'new' : editing.id}
          member={editing === 'new' ? undefined : editing}
          onDone={() => setEditing(null)}
        />
      )}
      <FilterableList
        rows={members}
        rowKey={(m) => m.id ?? 0}
        searchText={(m) => `${m.code} ${m.name}`}
        initialSort="code"
        columns={[
          {
            key: 'code',
            header: t('member.code'),
            sortValue: (m) => m.code ?? '',
            render: (m) => (
              <span className="inline-flex items-center gap-2">
                <span className="inline-block size-3 rounded-full" style={{ background: m.color }} aria-hidden />
                <strong>{m.code}</strong>
              </span>
            ),
          },
          { key: 'name', header: t('member.name'), sortValue: (m) => m.name ?? '', render: (m) => m.name },
          { key: 'kind', header: t('member.kind'), render: (m) => t(`member.kinds.${m.kind}`) },
          { key: 'email', header: t('member.email'), wideOnly: true, render: (m) => m.email },
          { key: 'phone', header: t('member.whatsappPhone'), wideOnly: true, render: (m) => m.whatsappPhone },
          {
            key: 'active',
            header: t('common.status'),
            render: (m) => (m.active ? t('common.active') : t('common.inactive')),
          },
        ]}
        actions={(m) => (
          <>
            <button type="button" className="icon-button" onClick={() => setEditing(m)} aria-label={t('action.edit')}>
              <Pencil size={16} aria-hidden />
            </button>
            <ActiveToggle resource={m} />
            <DeleteButton resource={m} />
          </>
        )}
      />
    </section>
  );
}

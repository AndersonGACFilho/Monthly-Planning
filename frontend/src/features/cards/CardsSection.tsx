import { CalendarClock, Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import type { CardModel, MemberModel } from '../../api/generated/model';
import { FilterableList } from '../../shared/components/FilterableList';
import { ActiveToggle, DeleteButton } from '../../shared/components/ResourceActions';
import { embedded, linkOf } from '../../shared/hal';
import { CardEditor } from './CardForm';
import { CycleForm } from './CycleForm';

/**
 * Cards registry (RF-02 to RF-05): finals, holder, default member, cycles and additional cards.
 *
 * @returns the section
 */
export function CardsSection() {
  const { t } = useTranslation();
  const { data } = useCards({ size: 200, sort: ['nickname,asc'] });
  const { data: memberPage } = useMembers({ size: 200 });
  const cards = embedded<CardModel>(data, 'cards');
  const members = new Map(embedded<MemberModel>(memberPage, 'members').map((m) => [m.id, m]));
  const byId = new Map(cards.map((c) => [c.id, c]));
  const [editing, setEditing] = useState<CardModel | 'new' | null>(null);
  const [cycleOf, setCycleOf] = useState<CardModel | null>(null);

  return (
    <section className="flex flex-col gap-3" aria-labelledby="cards-title">
      <div className="flex items-center justify-between">
        <h2 id="cards-title" className="text-lg font-semibold">
          {t('card.title')}
        </h2>
        <button type="button" className="button" onClick={() => setEditing('new')}>
          <Plus size={16} aria-hidden /> {t('card.new')}
        </button>
      </div>
      {editing && (
        <CardEditor key={editing === 'new' ? 'new' : editing.id} card={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
      )}
      {cycleOf && <CycleForm key={cycleOf.id} card={cycleOf} onDone={() => setCycleOf(null)} />}
      <FilterableList
        rows={cards}
        rowKey={(c) => c.id ?? 0}
        initialSort="nickname"
        columns={[
          { key: 'nickname', header: t('card.nickname'), sortValue: (c) => c.nickname ?? '', render: (c) => c.nickname },
          { key: 'finals', header: t('card.finals'), render: (c) => c.lastDigits?.join(', ') },
          { key: 'holder', header: t('card.holderName'), wideOnly: true, render: (c) => c.holderName },
          { key: 'member', header: t('card.defaultMember'), render: (c) => members.get(c.defaultMemberId)?.code },
          {
            key: 'cycle',
            header: t('card.cycle'),
            render: (c) => {
              const owner = c.primaryCardId != null ? byId.get(c.primaryCardId) : c;
              const cycle = owner?.cycles?.[owner.cycles.length - 1];
              return cycle ? t('card.cycleText', { closing: cycle.closingDay, due: cycle.dueDay }) : '';
            },
          },
          {
            key: 'primary',
            header: t('card.primaryCard'),
            wideOnly: true,
            render: (c) => (c.primaryCardId != null ? byId.get(c.primaryCardId)?.nickname : t('card.isPrimary')),
          },
          { key: 'active', header: t('common.status'), render: (c) => (c.active ? t('common.active') : t('common.inactive')) },
        ]}
        actions={(c) => (
          <>
            <button type="button" className="icon-button" onClick={() => setEditing(c)} aria-label={t('action.edit')}>
              <Pencil size={16} aria-hidden />
            </button>
            {linkOf(c, 'cycles') && (
              <button type="button" className="icon-button" onClick={() => setCycleOf(c)} aria-label={t('card.changeCycle')} title={t('card.changeCycle')}>
                <CalendarClock size={16} aria-hidden />
              </button>
            )}
            <ActiveToggle resource={c} />
            <DeleteButton resource={c} />
          </>
        )}
      />
    </section>
  );
}

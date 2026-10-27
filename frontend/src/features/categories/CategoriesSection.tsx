import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createCategory,
  replaceCategory,
  useCategories,
} from '../../api/generated/endpoints/category-controller/category-controller';
import type { CategoryModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { FilterableList } from '../../shared/components/FilterableList';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';

const ICONS = [
  'shopping-cart', 'utensils', 'car', 'heart-pulse', 'party-popper', 'repeat', 'house', 'shirt',
  'graduation-cap', 'plane', 'paw-print', 'circle-ellipsis', 'baby', 'fuel', 'gift', 'pill', 'wrench', 'zap',
];

function CategoryEditor({ category, onDone }: { category?: CategoryModel; onDone: () => void }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const [name, setName] = useState(category?.name ?? '');
  const [parentId, setParentId] = useState<number | null>(category?.parentId ?? null);
  const [color, setColor] = useState(category?.color ?? '#64748B');
  const [icon, setIcon] = useState(category?.icon ?? 'circle-ellipsis');

  return (
    <form
      className="panel flex flex-wrap items-end gap-3"
      aria-label={t('category.form')}
      onSubmit={async (event) => {
        event.preventDefault();
        const request = { name, parentId: parentId ?? undefined, color, icon };
        if (category?.id !== undefined) await replaceCategory(category.id, request, ifMatch(category.version));
        else await createCategory(request);
        await saved();
        onDone();
      }}
    >
      <Field label={t('category.name')} htmlFor="category-name">
        <input id="category-name" className="input" required maxLength={40} value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      {!category?.system && (
        <Field label={t('category.parent')} htmlFor="category-parent">
          <CategoryPicker id="category-parent" topLevelOnly allowEmpty value={parentId} onChange={setParentId} />
        </Field>
      )}
      <Field label={t('category.color')} htmlFor="category-color">
        <input id="category-color" type="color" className="input h-9 w-16 p-1" value={color} onChange={(e) => setColor(e.target.value)} />
      </Field>
      <Field label={t('category.icon')} htmlFor="category-icon">
        <select id="category-icon" className="input" value={icon} onChange={(e) => setIcon(e.target.value)}>
          {[...new Set([icon, ...ICONS])].map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
      </Field>
      <CategoryIcon icon={icon} color={color} />
      <button type="button" className="button" onClick={onDone}>
        {t('action.cancel')}
      </button>
      <button type="submit" className="button button-primary">
        {t('action.save')}
      </button>
    </form>
  );
}

/**
 * Categories registry with colour, icon and subcategories; "not classified" cannot be deleted (RF-06,
 * RF-07).
 *
 * @returns the section
 */
export function CategoriesSection() {
  const { t } = useTranslation();
  const { data } = useCategories({ size: 500, sort: ['name,asc'] });
  const categories = embedded<CategoryModel>(data, 'categories');
  const names = new Map(categories.map((c) => [c.id, c.name]));
  const [editing, setEditing] = useState<CategoryModel | 'new' | null>(null);

  return (
    <section className="flex flex-col gap-3" aria-labelledby="categories-title">
      <div className="flex items-center justify-between">
        <h2 id="categories-title" className="text-lg font-semibold">
          {t('category.title')}
        </h2>
        <button type="button" className="button" onClick={() => setEditing('new')}>
          <Plus size={16} aria-hidden /> {t('category.new')}
        </button>
      </div>
      {editing && (
        <CategoryEditor key={editing === 'new' ? 'new' : editing.id} category={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
      )}
      <FilterableList
        rows={categories}
        rowKey={(c) => c.id ?? 0}
        searchText={(c) => c.name ?? ''}
        initialSort="name"
        columns={[
          {
            key: 'name',
            header: t('category.name'),
            sortValue: (c) => `${c.parentId != null ? names.get(c.parentId) + ' ' : ''}${c.name}`,
            render: (c) => (
              <span className={`inline-flex items-center gap-2 ${c.parentId != null ? 'pl-6' : ''}`}>
                <CategoryIcon icon={c.icon} color={c.color} />
                {c.name}
              </span>
            ),
          },
          { key: 'parent', header: t('category.parent'), render: (c) => (c.parentId != null ? names.get(c.parentId) : '') },
          { key: 'system', header: t('common.type'), render: (c) => (c.system ? t('category.system') : '') },
        ]}
        actions={(c) => (
          <>
            <button type="button" className="icon-button" onClick={() => setEditing(c)} aria-label={t('action.edit')}>
              <Pencil size={16} aria-hidden />
            </button>
            <DeleteButton resource={c} />
          </>
        )}
      />
    </section>
  );
}

import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCategories } from '../../api/generated/endpoints/category-controller/category-controller';
import type { CategoryModel } from '../../api/generated/model';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { CategoryEditor } from './CategoryEditor';
import { FilterableList } from '../../shared/components/FilterableList';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { embedded } from '../../shared/hal';

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

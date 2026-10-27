import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createCategory, replaceCategory } from '../../api/generated/endpoints/category-controller/category-controller';
import type { CategoryModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { useSaved } from '../../shared/hooks/useSaved';

const ICONS = [
  'shopping-cart', 'utensils', 'car', 'heart-pulse', 'party-popper', 'repeat', 'house', 'shirt',
  'graduation-cap', 'plane', 'paw-print', 'circle-ellipsis', 'baby', 'fuel', 'gift', 'pill', 'wrench', 'zap',
];

/** Props of {@link CategoryEditor}. */
export interface CategoryEditorProps {
  /** Category being edited, or `undefined` to create one. */
  category?: CategoryModel;
  /** Called after saving (with the saved category) or cancelling (without). */
  onDone: (saved?: CategoryModel) => void;
}

/**
 * Form to create or edit a category: name, parent, colour and icon (RF-06, RF-07).
 *
 * @param props - {@link CategoryEditorProps}
 * @returns the form
 */
export function CategoryEditor({ category, onDone }: CategoryEditorProps) {
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
        const result = category?.id !== undefined
          ? await replaceCategory(category.id, request, ifMatch(category.version))
          : await createCategory(request);
        await saved();
        onDone(result);
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
      <button type="button" className="button" onClick={() => onDone()}>
        {t('action.cancel')}
      </button>
      <button type="submit" className="button button-primary">
        {t('action.save')}
      </button>
    </form>
  );
}

import { useTranslation } from 'react-i18next';
import { useCategories } from '../../api/generated/endpoints/category-controller/category-controller';
import type { CategoryModel } from '../../api/generated/model';
import { embedded } from '../hal';

/** Props of {@link CategoryPicker}. */
export interface CategoryPickerProps {
  /** Selected category id. */
  value: number | null | undefined;
  /** Called with the chosen category id, or `null`. */
  onChange: (id: number | null) => void;
  /** Input id. */
  id?: string;
  /** Offer an empty option. */
  allowEmpty?: boolean;
  /** Text of the empty option, such as "All members" in a filter; "— none —" by default. */
  emptyLabel?: string;
  /** Only top-level categories, for choosing a parent. */
  topLevelOnly?: boolean;
  /** Accessible label. */
  'aria-label'?: string;
}

/**
 * Category selector used by every form with a category; subcategories are listed under their parent
 * (SPEC section 13, front table).
 *
 * @param props - {@link CategoryPickerProps}
 * @returns a select of categories
 */
export function CategoryPicker({ value, onChange, id, allowEmpty, emptyLabel, topLevelOnly, ...rest }: CategoryPickerProps) {
  const { t } = useTranslation();
  const { data } = useCategories({ size: 500, sort: ['name,asc'] });
  const all = embedded<CategoryModel>(data, 'categories');
  const parents = all.filter((c) => c.parentId == null && !(topLevelOnly && c.system));
  return (
    <select
      id={id}
      className="input"
      value={value ?? ''}
      aria-label={rest['aria-label']}
      onChange={(event) => onChange(event.target.value ? Number(event.target.value) : null)}
    >
      {(allowEmpty || value == null) && <option value="">{emptyLabel ?? t('common.none')}</option>}
      {parents.map((parent) => [
        <option key={parent.id} value={parent.id}>
          {parent.name}
        </option>,
        ...(topLevelOnly
          ? []
          : all
              .filter((child) => child.parentId === parent.id)
              .map((child) => (
                <option key={child.id} value={child.id}>
                  {`  ${parent.name} › ${child.name}`}
                </option>
              ))),
      ])}
    </select>
  );
}

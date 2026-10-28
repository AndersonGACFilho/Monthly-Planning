import { DynamicIcon } from 'lucide-react/dynamic';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { Modal } from '../../shared/components/Modal';
import { searchIcons } from './iconSearch';

/** Most icons drawn at once; the search narrows the rest. */
const GRID_LIMIT = 120;

/** Props of {@link IconPicker}. */
export interface IconPickerProps {
  /** Id of the button, for the field label. */
  id: string;
  /** Chosen icon name. */
  value: string;
  /** Colour of the preview. */
  color: string;
  /** Called with the icon the user picks. */
  onChange: (icon: string) => void;
}

/**
 * Button showing the category icon; it opens a searchable grid of icon previews, with the name on hover.
 *
 * @param props - {@link IconPickerProps}
 * @returns the picker
 */
export function IconPicker({ id, value, color, onChange }: IconPickerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <>
      <button id={id} type="button" className="button" title={value} aria-label={t('category.pickIcon', { icon: value })} onClick={() => setOpen(true)}>
        <CategoryIcon icon={value} color={color} />
      </button>
      {open && (
        <Modal title={t('category.pickIconTitle')} onClose={() => setOpen(false)}>
          <IconGrid
            value={value}
            onPick={(icon) => {
              onChange(icon);
              setOpen(false);
            }}
          />
        </Modal>
      )}
    </>
  );
}

/**
 * Search box and the grid of matching icons.
 *
 * @param props - chosen icon and what to do with a pick
 * @returns the grid
 */
function IconGrid({ value, onPick }: { value: string; onPick: (icon: string) => void }) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const { icons, total } = searchIcons(query, GRID_LIMIT);
  return (
    <div className="flex flex-col gap-2">
      <input
        type="search"
        className="input"
        aria-label={t('category.searchIcon')}
        placeholder={t('category.searchIconHint')}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      {icons.length === 0 ? (
        <p className="p-2 text-sm text-(--color-text-muted)">{t('category.noIcon')}</p>
      ) : (
        <ul className="grid grid-cols-[repeat(auto-fill,minmax(2.5rem,1fr))] gap-1">
          {icons.map((name) => (
            <li key={name}>
              <button
                type="button"
                title={name}
                aria-label={name}
                aria-pressed={name === value}
                className={`icon-button size-10 ${name === value ? 'ring-2 ring-(--color-accent)' : ''}`}
                onClick={() => onPick(name)}
              >
                <DynamicIcon name={name} size={20} fallback={() => null} />
              </button>
            </li>
          ))}
        </ul>
      )}
      {total > icons.length && (
        <p className="text-xs text-(--color-text-muted)">{t('category.moreIcons', { shown: icons.length, total })}</p>
      )}
    </div>
  );
}

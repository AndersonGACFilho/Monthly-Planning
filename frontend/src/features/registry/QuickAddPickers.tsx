import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CategoryPicker, type CategoryPickerProps } from '../../shared/components/CategoryPicker';
import { MemberPicker, type MemberPickerProps } from '../../shared/components/MemberPicker';
import { Modal } from '../../shared/components/Modal';
import { CategoryEditor } from '../categories/CategoryEditor';
import { MemberEditor } from '../members/MemberForm';

/**
 * Member picker with a "+" button that registers a new member in a modal and selects it (RF-01, RF-76).
 *
 * @param props - the props of {@link MemberPicker}
 * @returns the picker and its button
 */
export function MemberPickerWithAdd(props: MemberPickerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <span className="inline-flex items-center gap-1">
      <MemberPicker {...props} />
      <button type="button" className="icon-button" aria-label={t('member.new')} title={t('member.new')} onClick={() => setOpen(true)}>
        <Plus size={14} aria-hidden />
      </button>
      {open && (
        <Modal title={t('member.new')} onClose={() => setOpen(false)}>
          <MemberEditor
            onDone={(saved) => {
              setOpen(false);
              if (saved?.id !== undefined) props.onChange(saved.id);
            }}
          />
        </Modal>
      )}
    </span>
  );
}

/**
 * Category picker with a "+" button that registers a new category in a modal and selects it (RF-06, RF-76).
 *
 * @param props - the props of {@link CategoryPicker}
 * @returns the picker and its button
 */
export function CategoryPickerWithAdd(props: CategoryPickerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <span className="inline-flex items-center gap-1">
      <CategoryPicker {...props} />
      <button type="button" className="icon-button" aria-label={t('category.new')} title={t('category.new')} onClick={() => setOpen(true)}>
        <Plus size={14} aria-hidden />
      </button>
      {open && (
        <Modal title={t('category.new')} onClose={() => setOpen(false)}>
          <CategoryEditor
            onDone={(saved) => {
              setOpen(false);
              if (saved?.id !== undefined) props.onChange(saved.id);
            }}
          />
        </Modal>
      )}
    </span>
  );
}

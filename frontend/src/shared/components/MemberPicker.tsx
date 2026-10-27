import { useTranslation } from 'react-i18next';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import type { MemberModel } from '../../api/generated/model';
import { embedded } from '../hal';

/** Props of {@link MemberPicker}. */
export interface MemberPickerProps {
  /** Selected member id. */
  value: number | null | undefined;
  /** Called with the chosen member id, or `null` for "none". */
  onChange: (id: number | null) => void;
  /** Input id. */
  id?: string;
  /** Offer an empty option. */
  allowEmpty?: boolean;
  /** Text of the empty option, such as "All members" in a filter; "— none —" by default. */
  emptyLabel?: string;
  /** Show inactive members too. */
  includeInactive?: boolean;
  /** Accessible label. */
  'aria-label'?: string;
}

/**
 * Member selector used by every form with a member (SPEC section 13, front table).
 *
 * @param props - {@link MemberPickerProps}
 * @returns a select listing members by code and name
 */
export function MemberPicker({ value, onChange, id, allowEmpty, emptyLabel, includeInactive, ...rest }: MemberPickerProps) {
  const { t } = useTranslation();
  const { data } = useMembers({ size: 200, sort: ['code,asc'] });
  const members = embedded<MemberModel>(data, 'members').filter((m) => includeInactive || m.active || m.id === value);
  return (
    <select
      id={id}
      className="input"
      value={value ?? ''}
      aria-label={rest['aria-label']}
      onChange={(event) => onChange(event.target.value ? Number(event.target.value) : null)}
    >
      {(allowEmpty || value == null) && <option value="">{emptyLabel ?? t('common.none')}</option>}
      {members.map((member) => (
        <option key={member.id} value={member.id}>
          {member.code} — {member.name}
        </option>
      ))}
    </select>
  );
}

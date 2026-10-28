import { Power, PowerOff, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { HalResource } from '../hal';
import { useAction } from '../hooks/useAction';

/** A HAL resource with a version for `If-Match`. */
type VersionedResource = HalResource & { version?: number };

/**
 * Activate or inactivate button, shown only when the backend offers the matching link (API-09, RF-01).
 *
 * @param props - the resource
 * @returns the button, or nothing
 */
export function ActiveToggle({ resource }: { resource: VersionedResource }) {
  const { t } = useTranslation();
  const inactivate = useAction(resource, 'inactivate');
  const activate = useAction(resource, 'activate');
  const action = inactivate.available ? inactivate : activate.available ? activate : null;
  if (!action) return null;
  const turningOff = action === inactivate;
  const label = turningOff ? t('action.inactivate') : t('action.activate');
  return (
    <button
      type="button"
      className="icon-button"
      title={label}
      aria-label={label}
      onClick={() =>
        action.mutate({ method: 'PATCH', body: { active: !turningOff }, version: versionTag(resource.version) })
      }
    >
      {turningOff ? <PowerOff size={16} aria-hidden /> : <Power size={16} aria-hidden />}
    </button>
  );
}

/**
 * Delete button, shown only when the backend offers `delete` (API-09); asks for confirmation.
 *
 * @param props - the resource and, optionally, a confirmation that says what else goes with it
 * @returns the button, or nothing
 */
export function DeleteButton({ resource, confirmMessage }: { resource: VersionedResource; confirmMessage?: string }) {
  const { t } = useTranslation();
  const remove = useAction(resource, 'delete');
  if (!remove.available) return null;
  return (
    <button
      type="button"
      className="icon-button button-danger"
      title={t('action.delete')}
      aria-label={t('action.delete')}
      onClick={() => {
        if (window.confirm(confirmMessage ?? t('action.confirmDelete'))) remove.mutate({ method: 'DELETE' });
      }}
    >
      <Trash2 size={16} aria-hidden />
    </button>
  );
}

function versionTag(version: number | undefined): string | null {
  return version === undefined ? null : `"${version}"`;
}

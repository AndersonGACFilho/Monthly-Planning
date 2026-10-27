import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changeSettings } from '../../api/generated/endpoints/settings-controller/settings-controller';
import { changeSmtpPassword, useSmtpPassword } from '../../api/generated/endpoints/smtp-password-controller/smtp-password-controller';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { useSaved } from '../../shared/hooks/useSaved';
import { type SettingsMap, useSettingsMap } from './GeneralSettings';

const KEYS = ['smtp-host', 'smtp-port', 'smtp-username', 'smtp-from', 'smtp-starttls'] as const;

/**
 * SMTP server for sending statements by e-mail; the password is write-only and stored encrypted (RF-86, Q9).
 *
 * @returns the section
 */
export function SmtpSettings() {
  const { t } = useTranslation();
  const saved = useSaved();
  const settings = useSettingsMap();
  const { data: password } = useSmtpPassword();
  const [changes, setChanges] = useState<SettingsMap>({});
  const [newPassword, setNewPassword] = useState('');
  const draft: SettingsMap = { ...settings, ...changes };
  const set = (key: string, value: string) => setChanges((d) => ({ ...d, [key]: value }));
  const configured = (password as { configured?: boolean } | undefined)?.configured ?? false;

  return (
    <form
      className="panel grid grid-cols-2 gap-3 wide:grid-cols-3"
      aria-labelledby="smtp-title"
      onSubmit={async (event) => {
        event.preventDefault();
        try {
          await changeSettings(Object.fromEntries(KEYS.filter((k) => draft[k] !== undefined).map((k) => [k, draft[k]!])));
          if (newPassword) await changeSmtpPassword({ password: newPassword });
          setChanges({});
          setNewPassword('');
          await saved();
        } catch (error) {
          reportError(error);
        }
      }}
    >
      <h2 id="smtp-title" className="col-span-full text-lg font-semibold">
        {t('smtp.title')}
      </h2>
      <Field label={t('smtp.host')} htmlFor="smtp-host">
        <input id="smtp-host" className="input" placeholder="smtp.gmail.com" value={draft['smtp-host'] ?? ''} onChange={(e) => set('smtp-host', e.target.value)} />
      </Field>
      <Field label={t('smtp.port')} htmlFor="smtp-port">
        <input id="smtp-port" type="number" min={1} max={65535} className="input" value={draft['smtp-port'] ?? ''} onChange={(e) => set('smtp-port', e.target.value)} />
      </Field>
      <Field label={t('smtp.username')} htmlFor="smtp-username">
        <input id="smtp-username" className="input" autoComplete="off" value={draft['smtp-username'] ?? ''} onChange={(e) => set('smtp-username', e.target.value)} />
      </Field>
      <Field label={t('smtp.from')} htmlFor="smtp-from">
        <input id="smtp-from" type="email" className="input" value={draft['smtp-from'] ?? ''} onChange={(e) => set('smtp-from', e.target.value)} />
      </Field>
      <Field label={t('smtp.password')} htmlFor="smtp-password">
        <input
          id="smtp-password"
          type="password"
          className="input"
          autoComplete="new-password"
          placeholder={configured ? '••••••••' : ''}
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
        />
      </Field>
      <label className="flex items-center gap-2 self-end text-sm">
        <input type="checkbox" checked={draft['smtp-starttls'] === 'true'} onChange={(e) => set('smtp-starttls', String(e.target.checked))} />
        {t('smtp.starttls')}
      </label>
      <p className="col-span-full text-xs text-(--color-text-muted)">
        {configured && `${t('smtp.passwordSet')} `}
        {t('smtp.passwordHint')}
      </p>
      <div className="col-span-full flex justify-end">
        <button type="submit" className="button button-primary">
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

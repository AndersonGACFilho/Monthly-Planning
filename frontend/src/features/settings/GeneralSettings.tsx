import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changeSettings, useSettings } from '../../api/generated/endpoints/settings-controller/settings-controller';
import { detectLanguage } from '../../i18n/i18n';
import { Field } from '../../shared/components/Field';
import { useSaved } from '../../shared/hooks/useSaved';
import { applyTheme } from '../../shared/theme';

/** Settings as sent by the API: key to value, plus links. */
export type SettingsMap = Record<string, string>;

/**
 * Reads the settings as a plain map.
 *
 * @returns the settings, or an empty map while loading
 */
export function useSettingsMap(): SettingsMap {
  const { data } = useSettings();
  return (data ?? {}) as unknown as SettingsMap;
}

/**
 * Language, colour theme (UI-08), alert thresholds, automatic validation and projection horizon (I18N-03, RF-16, RF-79d,
 * RN-09).
 *
 * @returns the section
 */
export function GeneralSettings() {
  const { t, i18n } = useTranslation();
  const saved = useSaved();
  const settings = useSettingsMap();
  const [changes, setChanges] = useState<SettingsMap>({});
  const draft: SettingsMap = { ...settings, ...changes };
  const set = (key: string, value: string) => setChanges((d) => ({ ...d, [key]: value }));
  const keys = ['language', 'theme', 'alert-warning-percent', 'alert-danger-percent', 'auto-validate', 'projection-horizon-months'];

  return (
    <form
      className="panel form-grid"
      aria-labelledby="general-settings-title"
      onSubmit={async (event) => {
        event.preventDefault();
        const changes = Object.fromEntries(keys.filter((k) => draft[k] !== undefined).map((k) => [k, draft[k]!]));
        await changeSettings(changes);
        await i18n.changeLanguage(draft.language || detectLanguage(navigator.languages));
        applyTheme(draft.theme);
        setChanges({});
        await saved();
      }}
    >
      <h2 id="general-settings-title" className="col-span-full text-lg font-semibold">
        {t('settings.general')}
      </h2>
      <Field label={t('settings.language')} htmlFor="settings-language">
        <select id="settings-language" className="input" value={draft.language ?? ''} onChange={(e) => set('language', e.target.value)}>
          <option value="">{t('settings.languageSystem')}</option>
          <option value="pt-BR">{t('language.pt-BR')}</option>
          <option value="en-US">{t('language.en-US')}</option>
        </select>
      </Field>
      <Field label={t('settings.theme')} htmlFor="settings-theme">
        <select id="settings-theme" className="input" value={draft.theme ?? ''} onChange={(e) => set('theme', e.target.value)}>
          <option value="">{t('theme.system')}</option>
          <option value="light">{t('theme.light')}</option>
          <option value="dark">{t('theme.dark')}</option>
        </select>
      </Field>
      <Field label={t('settings.alertWarning')} htmlFor="settings-warning">
        <input id="settings-warning" type="number" min={1} max={1000} className="input" value={draft['alert-warning-percent'] ?? ''} onChange={(e) => set('alert-warning-percent', e.target.value)} />
      </Field>
      <Field label={t('settings.alertDanger')} htmlFor="settings-danger">
        <input id="settings-danger" type="number" min={1} max={1000} className="input" value={draft['alert-danger-percent'] ?? ''} onChange={(e) => set('alert-danger-percent', e.target.value)} />
      </Field>
      <Field label={t('settings.horizon')} htmlFor="settings-horizon">
        <input id="settings-horizon" type="number" min={1} max={36} className="input" value={draft['projection-horizon-months'] ?? ''} onChange={(e) => set('projection-horizon-months', e.target.value)} />
      </Field>
      <label className="check-field">
        <input type="checkbox" checked={draft['auto-validate'] === 'true'} onChange={(e) => set('auto-validate', String(e.target.checked))} />
        {t('settings.autoValidate')}
      </label>
      <div className="col-span-full flex justify-end">
        <button type="submit" className="button button-primary">
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

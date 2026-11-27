import { useTranslation } from 'react-i18next'
import { LOCALES } from './i18n'

export default function LanguageSelect() {
  const { t, i18n } = useTranslation()
  return (
    <p>
      <label htmlFor="language">{t('language.label')}</label>{' '}
      <select id="language" value={i18n.language} onChange={(e) => i18n.changeLanguage(e.target.value)}>
        {LOCALES.map((l) => (
          <option key={l} value={l} lang={l}>{t(`language.${l}`)}</option>
        ))}
      </select>
    </p>
  )
}

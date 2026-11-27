import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import deDE from './locales/de-DE.json'
import enUS from './locales/en-US.json'

export const LOCALES = ['en-US', 'de-DE'] as const
export type Locale = (typeof LOCALES)[number]
export const resources = { 'en-US': enUS, 'de-DE': deDE }

// Keep <html lang> in step, so screen readers pronounce the page in the right language.
i18n.on('languageChanged', (lng) => {
  if (typeof document !== 'undefined') document.documentElement.lang = lng
})

i18n.use(initReactI18next).init({
  lng: 'en-US',
  fallbackLng: 'en-US',
  supportedLngs: [...LOCALES],
  load: 'currentOnly',
  resources: {
    'en-US': { translation: enUS },
    'de-DE': { translation: deDE },
  },
  interpolation: { escapeValue: false }, // React already escapes values
  initAsync: false, // resources are bundled, so the first render is already translated
})

export default i18n

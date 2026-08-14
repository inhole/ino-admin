import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import { ko } from './resources'

void i18n.use(LanguageDetector).use(initReactI18next).init({
  resources: { ko }, defaultNS: 'common', fallbackLng: 'ko', supportedLngs: ['ko'], load: 'languageOnly',
  interpolation: { escapeValue: false },
  detection: { order: ['localStorage', 'navigator'], lookupLocalStorage: 'ino-admin.locale', caches: ['localStorage'] },
  react: { useSuspense: false },
})
document.documentElement.lang = i18n.resolvedLanguage ?? 'ko'
i18n.on('languageChanged', (language) => { document.documentElement.lang = language })
export default i18n

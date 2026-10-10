import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { TRANSLATIONS } from '../i18n/translations';
import type { Language } from '../i18n/translations';

interface LanguageState {
  language: Language;
  setLanguage: (lang: Language) => void;
  toggleLanguage: () => void;
  t: typeof TRANSLATIONS['vi'];
}

export const useLanguageStore = create<LanguageState>()(
  persist(
    (set, get) => ({
      language: 'vi',
      setLanguage: (lang: Language) => {
        set({
          language: lang,
          t: TRANSLATIONS[lang] || TRANSLATIONS.vi,
        });
      },
      toggleLanguage: () => {
        const next = get().language === 'vi' ? 'en' : 'vi';
        set({
          language: next,
          t: TRANSLATIONS[next],
        });
      },
      t: TRANSLATIONS.vi,
    }),
    {
      name: 'danasea_language_storage',
      // Re-hydrate `t` on store re-hydration
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.t = TRANSLATIONS[state.language] || TRANSLATIONS.vi;
        }
      },
    }
  )
);

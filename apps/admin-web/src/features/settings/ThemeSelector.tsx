import { Moon, Monitor, Sun } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useTheme, type Theme } from './theme'

const options: Array<{ value: Theme; icon: typeof Sun; label: 'themeLight' | 'themeDark' | 'themeSystem' }> = [
  { value: 'light', icon: Sun, label: 'themeLight' }, { value: 'dark', icon: Moon, label: 'themeDark' }, { value: 'system', icon: Monitor, label: 'themeSystem' },
]
export function ThemeSelector() {
  const { t } = useTranslation('common'); const { theme, setTheme } = useTheme()
  return <fieldset><legend className="mb-2 text-xs font-semibold text-muted-foreground">{t('theme')}</legend><div className="grid grid-cols-3 gap-1 rounded-xl bg-muted p-1">{options.map(({ value, icon: Icon, label }) => <button aria-pressed={theme === value} className="flex min-h-10 items-center justify-center gap-1.5 rounded-lg px-2 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground aria-pressed:bg-background aria-pressed:text-foreground aria-pressed:shadow-sm" key={value} onClick={() => setTheme(value)} type="button"><Icon aria-hidden="true" size={15} /><span>{t(label)}</span></button>)}</div></fieldset>
}

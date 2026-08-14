import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type Theme = 'light' | 'dark' | 'system'
type ResolvedTheme = Exclude<Theme, 'system'>
const STORAGE_KEY = 'ino-admin.theme'
const DARK_QUERY = '(prefers-color-scheme: dark)'

type ThemeContextValue = { theme: Theme; resolvedTheme: ResolvedTheme; setTheme: (theme: Theme) => void }
const ThemeContext = createContext<ThemeContextValue | null>(null)

function storedTheme(): Theme {
  const value = localStorage.getItem(STORAGE_KEY)
  return value === 'light' || value === 'dark' || value === 'system' ? value : 'system'
}

function applyTheme(theme: ResolvedTheme) {
  document.documentElement.classList.toggle('dark', theme === 'dark')
  document.documentElement.style.colorScheme = theme
  document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#111827' : '#f7f8fa')
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(storedTheme)
  const [prefersDark, setPrefersDark] = useState(() => matchMedia(DARK_QUERY).matches)
  const resolvedTheme: ResolvedTheme = theme === 'system' ? (prefersDark ? 'dark' : 'light') : theme

  useEffect(() => {
    const media = matchMedia(DARK_QUERY)
    const update = (event: MediaQueryListEvent) => setPrefersDark(event.matches)
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])
  useEffect(() => applyTheme(resolvedTheme), [resolvedTheme])

  const setTheme = useCallback((next: Theme) => { localStorage.setItem(STORAGE_KEY, next); setThemeState(next) }, [])
  const value = useMemo(() => ({ theme, resolvedTheme, setTheme }), [resolvedTheme, setTheme, theme])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used within ThemeProvider')
  return context
}

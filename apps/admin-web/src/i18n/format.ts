import i18n from './index'
function locale() { return i18n.resolvedLanguage ?? 'ko' }
export function formatDate(value: string | Date) { return new Intl.DateTimeFormat(locale(), { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value)) }
export function formatDateTime(value: string | Date) { return new Intl.DateTimeFormat(locale(), { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) }
export function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${new Intl.NumberFormat(locale()).format(bytes)} B`
  const units = ['KB', 'MB', 'GB']; let size = bytes / 1024; let unit = 0
  while (size >= 1024 && unit < units.length - 1) { size /= 1024; unit += 1 }
  return `${new Intl.NumberFormat(locale(), { maximumFractionDigits: 1 }).format(size)} ${units[unit]}`
}

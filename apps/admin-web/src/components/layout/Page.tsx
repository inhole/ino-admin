import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export function PageHeader({ eyebrow, title, description, actions }: { eyebrow: string; title: string; description?: string; actions?: ReactNode }) {
  return <header className="mb-6 flex flex-col gap-4 sm:mb-8 sm:flex-row sm:items-end sm:justify-between"><div className="min-w-0"><p className="mb-2 text-xs font-bold tracking-[0.18em] text-primary">{eyebrow}</p><h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{title}</h1>{description && <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground sm:text-base">{description}</p>}</div>{actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}</header>
}
export function FormField({ label, htmlFor, hint, children, className }: { label: string; htmlFor: string; hint?: string; children: ReactNode; className?: string }) {
  return <div className={cn('grid content-start gap-2', className)}><label className="text-sm font-semibold" htmlFor={htmlFor}>{label}</label>{children}{hint && <p className="text-xs leading-5 text-muted-foreground">{hint}</p>}</div>
}
export function StatusPanel({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('rounded-xl border border-dashed bg-muted/35 px-4 py-8 text-center text-sm text-muted-foreground', className)}>{children}</div>
}

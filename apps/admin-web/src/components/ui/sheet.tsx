import { Dialog } from '@base-ui/react/dialog'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button } from './button'
import { cn } from '@/lib/utils'

export function Sheet({ open, onOpenChange, title, description, children, side = 'left', showClose = true }: { open: boolean; onOpenChange: (open: boolean) => void; title: string; description?: string; children: ReactNode; side?: 'left' | 'right'; showClose?: boolean }) {
  return <Dialog.Root onOpenChange={onOpenChange} open={open}><Dialog.Portal><Dialog.Backdrop className="fixed inset-0 z-50 bg-black/55 transition-opacity data-ending-style:opacity-0 data-starting-style:opacity-0" /><Dialog.Popup className={cn('safe-top safe-bottom fixed inset-y-0 z-50 flex w-[min(86vw,360px)] flex-col bg-card shadow-2xl transition-transform duration-200 data-ending-style:-translate-x-full data-starting-style:-translate-x-full', side === 'left' ? 'left-0' : 'right-0 data-ending-style:translate-x-full data-starting-style:translate-x-full')}><div className="sr-only"><Dialog.Title>{title}</Dialog.Title>{description && <Dialog.Description>{description}</Dialog.Description>}</div>{showClose && <Dialog.Close render={<Button aria-label="닫기" className="absolute right-3 top-3 z-10" size="icon" variant="ghost" />}><X /></Dialog.Close>}{children}</Dialog.Popup></Dialog.Portal></Dialog.Root>
}

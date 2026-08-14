import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'
export function Item({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn('flex flex-col gap-3 rounded-xl border p-4 sm:flex-row sm:items-center', className)} {...props} /> }
export function ItemContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn('min-w-0 flex-1', className)} {...props} /> }
export function ItemTitle({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) { return <p className={cn('font-semibold leading-none', className)} {...props} /> }
export function ItemDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) { return <p className={cn('mt-1 text-sm text-muted-foreground', className)} {...props} /> }
export function ItemActions({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn('flex shrink-0 gap-2', className)} {...props} /> }

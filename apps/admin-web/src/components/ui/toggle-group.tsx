import { Toggle } from '@base-ui/react/toggle'
import { ToggleGroup as ToggleGroupPrimitive } from '@base-ui/react/toggle-group'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'
export function ToggleGroup({ className, ...props }: ComponentProps<typeof ToggleGroupPrimitive>) { return <ToggleGroupPrimitive className={cn('grid gap-1 rounded-xl bg-muted p-1', className)} {...props} /> }
export function ToggleGroupItem({ className, ...props }: ComponentProps<typeof Toggle>) { return <Toggle className={cn('flex min-h-10 items-center justify-center gap-1.5 rounded-lg px-2 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground data-pressed:bg-background data-pressed:text-foreground data-pressed:shadow-sm', className)} {...props} /> }

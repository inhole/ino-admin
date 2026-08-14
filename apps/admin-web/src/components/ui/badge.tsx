import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

const badgeVariants = cva('inline-flex w-fit items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold', { variants: { variant: { default: 'border-transparent bg-primary text-primary-foreground', secondary: 'border-transparent bg-secondary text-secondary-foreground', outline: 'text-foreground', success: 'border-transparent bg-success/15 text-success', destructive: 'border-transparent bg-destructive/15 text-destructive' } }, defaultVariants: { variant: 'default' } })
export function Badge({ className, variant, ...props }: HTMLAttributes<HTMLSpanElement> & VariantProps<typeof badgeVariants>) { return <span className={cn(badgeVariants({ variant }), className)} {...props} /> }

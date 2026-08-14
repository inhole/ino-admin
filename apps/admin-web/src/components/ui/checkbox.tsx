import { Checkbox as CheckboxPrimitive } from '@base-ui/react/checkbox'
import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'

export function Checkbox({ className, ...props }: CheckboxPrimitive.Root.Props) {
  return <CheckboxPrimitive.Root className={cn('peer grid size-5 shrink-0 place-items-center rounded-md border border-input bg-background text-primary-foreground shadow-xs transition-colors data-checked:border-primary data-checked:bg-primary disabled:cursor-not-allowed disabled:opacity-50 focus-visible:ring-3 focus-visible:ring-ring/30', className)} {...props}><CheckboxPrimitive.Indicator className="data-unchecked:hidden"><Check size={14} strokeWidth={3} /></CheckboxPrimitive.Indicator></CheckboxPrimitive.Root>
}

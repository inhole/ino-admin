import type { RemixiconComponentType } from "@remixicon/react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function MetricCard({
  label,
  value,
  description,
  icon: Icon,
}: {
  label: string;
  value: string;
  description: string;
  icon: RemixiconComponentType;
}) {
  return (
    <Card className="rounded-2xl shadow-none">
      <CardHeader className="gap-2">
        <CardDescription className="flex items-center gap-2">
          <Icon aria-hidden />
          {label}
        </CardDescription>
        <CardTitle className="text-2xl font-semibold tabular-nums">
          {value}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-xs text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  );
}

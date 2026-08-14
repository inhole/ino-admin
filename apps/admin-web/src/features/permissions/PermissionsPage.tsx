import { useQuery } from '@tanstack/react-query'
import { ApiClientError, getPermissionCatalog } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function PermissionsPage() {
  const catalog = useQuery({ queryKey: ['permissions'], queryFn: getPermissionCatalog })
  return <><header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">IDENTITY</p><h1 className="text-3xl font-bold tracking-tight">권한 카탈로그</h1></header><Card><CardHeader><CardTitle>역할별 권한</CardTitle><CardDescription>API 접근에 사용되는 서버 권한 키입니다.</CardDescription></CardHeader><CardContent>
    {catalog.isPending && <p role="status">권한을 불러오는 중…</p>}
    {catalog.isError && <Alert variant="destructive" role="alert"><AlertTitle>조회 오류</AlertTitle><AlertDescription>{catalog.error instanceof ApiClientError ? catalog.error.message : '권한을 불러올 수 없습니다.'}</AlertDescription></Alert>}
    {catalog.data && <div className="grid gap-4 md:grid-cols-3">{catalog.data.map(item => <section className="rounded-md border p-4" key={item.role}><h2 className="mb-3 font-semibold">{item.role}</h2>{item.permissions.length === 0 ? <p className="text-sm text-muted-foreground">부여된 권한 없음</p> : <ul className="grid gap-2 text-sm">{item.permissions.map(permission => <li className="rounded bg-muted px-2 py-1 font-mono" key={permission}>{permission}</li>)}</ul>}</section>)}</div>}
  </CardContent></Card></>
}

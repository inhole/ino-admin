import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiClientError, getAvailablePermissions, getPermissionCatalog, updateRolePermissions } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/features/auth/model/useAuth'

export function PermissionsPage() {
  const catalog = useQuery({ queryKey: ['permissions'], queryFn: getPermissionCatalog })
  const available = useQuery({ queryKey: ['permissions', 'available'], queryFn: getAvailablePermissions })
  const client = useQueryClient(); const { user } = useAuth()
  const update = useMutation({ mutationFn: ({ role, permissions }: { role: string; permissions: string[] }) => updateRolePermissions(role, permissions), onSuccess: async () => client.invalidateQueries({ queryKey: ['permissions'] }) })
  return <><header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">IDENTITY</p><h1 className="text-3xl font-bold tracking-tight">권한 카탈로그</h1></header><Card><CardHeader><CardTitle>역할별 권한</CardTitle><CardDescription>API 접근에 사용되는 서버 권한 키입니다.</CardDescription></CardHeader><CardContent>
    {catalog.isPending && <p role="status">권한을 불러오는 중…</p>}
    {catalog.isError && <Alert variant="destructive" role="alert"><AlertTitle>조회 오류</AlertTitle><AlertDescription>{catalog.error instanceof ApiClientError ? catalog.error.message : '권한을 불러올 수 없습니다.'}</AlertDescription></Alert>}
    {catalog.data && <div className="grid gap-4 md:grid-cols-3">{catalog.data.map(item => <section className="rounded-md border p-4" key={item.role}><h2 className="mb-3 font-semibold">{item.role}</h2><div className="grid gap-2 text-sm">{available.data?.map(permission => { const checked = item.permissions.includes(permission); return <label className="flex items-center gap-2" key={permission}><input checked={checked} disabled={item.role === 'SUPER_ADMIN' || !user?.permissions.includes('permission:update') || update.isPending} onChange={() => update.mutate({ role: item.role, permissions: checked ? item.permissions.filter(value => value !== permission) : [...item.permissions, permission] })} type="checkbox" /><span className="font-mono">{permission}</span></label> })}</div>{item.permissions.length === 0 && <p className="mt-2 text-sm text-muted-foreground">부여된 권한 없음</p>}{update.isError && <Alert className="mt-3" variant="destructive"><AlertDescription>{update.error instanceof ApiClientError ? update.error.message : '권한을 변경할 수 없습니다.'}</AlertDescription></Alert>}</section>)}</div>}
  </CardContent></Card></>
}

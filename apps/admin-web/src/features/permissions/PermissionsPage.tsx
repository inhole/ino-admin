import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiClientError, createRole, getAvailablePermissions, getPermissionCatalog, updateRolePermissions, updateRoleStatus } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/features/auth/model/useAuth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'

export function PermissionsPage() {
  const catalog = useQuery({ queryKey: ['permissions'], queryFn: getPermissionCatalog })
  const available = useQuery({ queryKey: ['permissions', 'available'], queryFn: getAvailablePermissions })
  const client = useQueryClient(); const { user } = useAuth()
  const update = useMutation({ mutationFn: ({ role, permissions }: { role: string; permissions: string[] }) => updateRolePermissions(role, permissions), onSuccess: async () => client.invalidateQueries({ queryKey: ['permissions'] }) })
  const create = useMutation({ mutationFn: createRole, onSuccess: async () => client.invalidateQueries({ queryKey: ['permissions'] }) })
  const status = useMutation({ mutationFn: ({ role, enabled }: { role: string; enabled: boolean }) => updateRoleStatus(role, enabled), onSuccess: async () => client.invalidateQueries({ queryKey: ['permissions'] }) })
  return <><header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">IDENTITY</p><h1 className="text-3xl font-bold tracking-tight">권한 카탈로그</h1></header>{user?.permissions.includes('permission:update') && <Card className="mb-6"><CardHeader><CardTitle>커스텀 역할 생성</CardTitle></CardHeader><CardContent><form className="grid gap-3 md:grid-cols-3" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); create.mutate({ role: String(data.get('role')), displayName: String(data.get('displayName')), permissions: [] }); event.currentTarget.reset() }}><Input aria-label="역할 키" name="role" placeholder="CONTENT_EDITOR" required /><Input aria-label="역할 이름" name="displayName" placeholder="콘텐츠 편집자" required /><Button disabled={create.isPending} type="submit">역할 생성</Button></form></CardContent></Card>}<Card><CardHeader><CardTitle>역할별 권한</CardTitle><CardDescription>API 접근에 사용되는 서버 권한 키입니다.</CardDescription></CardHeader><CardContent>
    {catalog.isPending && <p role="status">권한을 불러오는 중…</p>}
    {catalog.isError && <Alert variant="destructive" role="alert"><AlertTitle>조회 오류</AlertTitle><AlertDescription>{catalog.error instanceof ApiClientError ? catalog.error.message : '권한을 불러올 수 없습니다.'}</AlertDescription></Alert>}
    {catalog.data && <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{catalog.data.map(item => <section className="rounded-xl border bg-card p-4 shadow-sm" key={item.role}><div className="mb-4 flex items-start justify-between gap-3"><div><h2 className="font-semibold">{item.displayName || item.role}</h2><Badge className="mt-1 font-mono" variant={item.enabled ? 'secondary' : 'outline'}>{item.role}</Badge></div>{!item.systemRole && <Button onClick={() => status.mutate({ role: item.role, enabled: !item.enabled })} size="sm" variant="outline">{item.enabled ? '비활성화' : '활성화'}</Button>}</div><div className="grid gap-3 text-sm">{available.data?.map(permission => { const checked = item.permissions.includes(permission); return <label className="flex min-h-10 cursor-pointer items-center gap-3 rounded-lg px-2 hover:bg-muted" key={permission}><Checkbox checked={checked} disabled={item.role === 'SUPER_ADMIN' || !user?.permissions.includes('permission:update') || update.isPending} onCheckedChange={() => update.mutate({ role: item.role, permissions: checked ? item.permissions.filter(value => value !== permission) : [...item.permissions, permission] })} /><span className="break-all font-mono text-xs">{permission}</span></label> })}</div>{item.permissions.length === 0 && <p className="mt-2 text-sm text-muted-foreground">부여된 권한 없음</p>}{update.isError && <Alert className="mt-3" variant="destructive"><AlertDescription>{update.error instanceof ApiClientError ? update.error.message : '권한을 변경할 수 없습니다.'}</AlertDescription></Alert>}</section>)}</div>}
  </CardContent></Card></>
}

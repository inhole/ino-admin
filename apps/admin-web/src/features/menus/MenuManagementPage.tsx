import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiClientError, createMenu, getMenus, updateMenu, type ManagedMenu } from '@/api/client'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

export function MenuManagementPage() {
  const queryClient = useQueryClient(); const menus = useQuery({ queryKey: ['menus', 'all'], queryFn: getMenus })
  const [error, setError] = useState<string | null>(null)
  const save = useMutation({ mutationFn: createMenu, onSuccess: async () => { setError(null); await queryClient.invalidateQueries({ queryKey: ['menus'] }) }, onError: (e) => setError(e instanceof ApiClientError ? e.message : '메뉴를 저장할 수 없습니다.') })
  const toggle = useMutation({ mutationFn: (menu: ManagedMenu) => updateMenu(menu.id, { ...menu, enabled: !menu.enabled }), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['menus'] }) }, onError: (e) => setError(e instanceof ApiClientError ? e.message : '메뉴 상태를 변경할 수 없습니다.') })
  const submit = (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const form = event.currentTarget; const data = new FormData(form); save.mutate({ id: String(data.get('id')), parentId: String(data.get('parentId')) || null, label: String(data.get('label')), route: String(data.get('route')), icon: String(data.get('icon')) as ManagedMenu['icon'], order: Number(data.get('order')), requiredPermission: String(data.get('requiredPermission')) || null, enabled: true }); form.reset() }
  return <><header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">NAVIGATION</p><h1 className="text-3xl font-bold tracking-tight">메뉴 관리</h1></header>
    <Card className="mb-6"><CardHeader><CardTitle>메뉴 생성</CardTitle><CardDescription>route, icon key, 권한과 정렬 순서를 등록합니다.</CardDescription></CardHeader><CardContent><form className="grid gap-3 md:grid-cols-4" onSubmit={submit}><Input aria-label="메뉴 ID" name="id" placeholder="menu-id" required /><Input aria-label="메뉴 이름" name="label" placeholder="메뉴 이름" required /><Input aria-label="경로" name="route" placeholder="/route" required /><Input aria-label="아이콘" name="icon" placeholder="menu" required /><Input aria-label="부모 메뉴 ID" name="parentId" placeholder="부모 ID (선택)" /><Input aria-label="필요 권한" name="requiredPermission" placeholder="resource:action" /><Input aria-label="정렬 순서" min="0" name="order" placeholder="10" required type="number" /><Button disabled={save.isPending} type="submit">추가</Button></form></CardContent></Card>
    <Card><CardHeader><CardTitle>전체 메뉴</CardTitle></CardHeader><CardContent>{error && <Alert className="mb-4" variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}{menus.isPending && <p role="status">메뉴를 불러오는 중…</p>}{menus.isError && <p role="alert">메뉴를 불러올 수 없습니다.</p>}{menus.data && <div className="grid gap-2">{menus.data.map(menu => <div className="flex items-center justify-between rounded-md border p-3" key={menu.id}><div><p className="font-medium">{menu.label} <span className="text-muted-foreground">{menu.route}</span></p><p className="text-xs text-muted-foreground">{menu.requiredPermission ?? '공개 메뉴'} · 순서 {menu.order}</p></div><Button onClick={() => toggle.mutate(menu)} size="sm" variant="outline">{menu.enabled ? '비활성화' : '활성화'}</Button></div>)}</div>}</CardContent></Card></>
}

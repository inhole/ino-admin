import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiClientError, createUser, getPermissionCatalog, getUser, getUsers, updateUserProfile, updateUserStatus, type UserSummary } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/features/auth/model/useAuth'

export function UsersPage() {
  const users = useQuery({ queryKey: ['users'], queryFn: () => getUsers() })
  const roles = useQuery({ queryKey: ['permissions'], queryFn: getPermissionCatalog })
  const { user: currentUser } = useAuth()
  const queryClient = useQueryClient()
  const [createError, setCreateError] = useState<string | null>(null)
  const [createdMessage, setCreatedMessage] = useState<string | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [editing, setEditing] = useState<UserSummary | null>(null)
  const create = useMutation({ mutationFn: createUser, onSuccess: async (created) => {
    setCreatedMessage(`${created.displayName} 사용자를 생성했습니다.`)
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  } })
  const changeStatus = useMutation({ mutationFn: ({ id, status }: { id: string; status: 'ACTIVE' | 'DISABLED' }) => updateUserStatus(id, status), onSuccess: async () => {
    setStatusError(null)
    await queryClient.invalidateQueries({ queryKey: ['users'] })
  }, onError: (error) => setStatusError(error instanceof ApiClientError ? error.message : '사용자 상태를 변경할 수 없습니다.') })
  const update = useMutation({ mutationFn: ({ id, displayName, role }: { id: string; displayName: string; role: string }) => updateUserProfile(id, { displayName, role }), onSuccess: async () => {
    setEditing(null); setStatusError(null); await queryClient.invalidateQueries({ queryKey: ['users'] })
  }, onError: (error) => setStatusError(error instanceof ApiClientError ? error.message : '사용자 정보를 변경할 수 없습니다.') })
  const startEditing = async (id: string) => {
    try { setEditing(await getUser(id)); setStatusError(null) }
    catch (error) { setStatusError(error instanceof ApiClientError ? error.message : '사용자 정보를 불러올 수 없습니다.') }
  }
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setCreateError(null); setCreatedMessage(null)
    const form = event.currentTarget; const data = new FormData(form)
    try {
      await create.mutateAsync({ email: String(data.get('email')), password: String(data.get('password')), displayName: String(data.get('displayName')), role: String(data.get('role')) })
      form.reset()
    } catch (error) { setCreateError(error instanceof ApiClientError ? error.message : '사용자를 생성할 수 없습니다.') }
  }
  return <>
    <header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">IDENTITY</p><h1 className="text-3xl font-bold tracking-tight">사용자 관리</h1></header>
    {currentUser?.permissions.includes('user:create') && <Card className="mb-6"><CardHeader><CardTitle>사용자 생성</CardTitle><CardDescription>ADMIN 또는 VIEWER 계정을 생성합니다.</CardDescription></CardHeader><CardContent><form className="grid gap-4 md:grid-cols-2" onSubmit={submit}>
      <div className="grid gap-2"><label htmlFor="displayName">이름</label><Input id="displayName" name="displayName" required /></div>
      <div className="grid gap-2"><label htmlFor="new-user-email">이메일</label><Input id="new-user-email" name="email" required type="email" /></div>
      <div className="grid gap-2"><label htmlFor="new-user-password">초기 비밀번호</label><Input id="new-user-password" minLength={12} name="password" required type="password" /></div>
      <div className="grid gap-2"><label htmlFor="role">역할</label><select className="h-8 rounded-lg border border-input bg-transparent px-2.5" defaultValue="VIEWER" id="role" name="role">{roles.data?.filter(role => role.role !== 'SUPER_ADMIN').map(role => <option key={role.role} value={role.role}>{role.role}</option>)}</select></div>
      {createError && <Alert className="md:col-span-2" variant="destructive" role="alert"><AlertDescription>{createError}</AlertDescription></Alert>}
      {createdMessage && <p className="text-sm text-emerald-700 md:col-span-2" role="status">{createdMessage}</p>}
      <Button className="md:col-span-2 md:w-fit" disabled={create.isPending} type="submit">{create.isPending ? '생성 중…' : '사용자 생성'}</Button>
    </form></CardContent></Card>}
    <Card><CardHeader><CardTitle>사용자 목록</CardTitle><CardDescription>등록된 관리자 계정과 현재 상태입니다.</CardDescription></CardHeader><CardContent>
      {statusError && <Alert className="mb-4" variant="destructive" role="alert"><AlertDescription>{statusError}</AlertDescription></Alert>}
      {users.isPending && <p role="status">사용자를 불러오는 중…</p>}
      {users.isError && <Alert variant="destructive" role="alert"><AlertTitle>조회 오류</AlertTitle><AlertDescription>{users.error instanceof ApiClientError && users.error.status === 403 ? '사용자 목록을 볼 권한이 없습니다.' : users.error.message}</AlertDescription><Button className="mt-3" onClick={() => users.refetch()} size="sm" variant="outline">다시 시도</Button></Alert>}
      {users.data?.content.length === 0 && <p>등록된 사용자가 없습니다.</p>}
      {editing && <form className="mb-5 grid gap-3 rounded-md border p-4 md:grid-cols-3" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); update.mutate({ id: editing.id, displayName: String(data.get('displayName')), role: String(data.get('role')) }) }}><Input aria-label="수정할 이름" defaultValue={editing.displayName} maxLength={100} name="displayName" required /><select aria-label="수정할 역할" className="h-9 rounded-md border bg-background px-3 text-sm" defaultValue={editing.role} name="role">{roles.data?.filter(role => role.role !== 'SUPER_ADMIN').map(role => <option key={role.role} value={role.role}>{role.role}</option>)}</select><div className="flex gap-2"><Button disabled={update.isPending} type="submit">저장</Button><Button onClick={() => setEditing(null)} type="button" variant="outline">취소</Button></div></form>}
      {users.data && users.data.content.length > 0 && <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b text-muted-foreground"><tr><th className="py-3">이름</th><th>이메일</th><th>역할</th><th>상태</th><th>등록일</th><th>관리</th></tr></thead><tbody className="divide-y">{users.data.content.map(user => <tr key={user.id}><td className="py-4 font-medium">{user.displayName}</td><td>{user.email}</td><td>{user.role}</td><td>{user.status}</td><td>{new Date(user.createdAt).toLocaleDateString('ko-KR')}</td><td>{currentUser?.permissions.includes('user:update') && currentUser.id !== user.id && <div className="flex gap-2"><Button onClick={() => startEditing(user.id)} size="sm" variant="outline">수정</Button><Button disabled={changeStatus.isPending} onClick={() => changeStatus.mutate({ id: user.id, status: user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })} size="sm" variant="outline">{user.status === 'ACTIVE' ? '비활성화' : user.status === 'LOCKED' ? '잠금 해제' : '활성화'}</Button></div>}</td></tr>)}</tbody></table></div>}
    </CardContent></Card>
  </>
}

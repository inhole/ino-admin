import { useQuery } from '@tanstack/react-query'
import { ApiClientError, getUsers } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function UsersPage() {
  const users = useQuery({ queryKey: ['users'], queryFn: () => getUsers() })
  return <>
    <header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">IDENTITY</p><h1 className="text-3xl font-bold tracking-tight">사용자 관리</h1></header>
    <Card><CardHeader><CardTitle>사용자 목록</CardTitle><CardDescription>등록된 관리자 계정과 현재 상태입니다.</CardDescription></CardHeader><CardContent>
      {users.isPending && <p role="status">사용자를 불러오는 중…</p>}
      {users.isError && <Alert variant="destructive" role="alert"><AlertTitle>조회 오류</AlertTitle><AlertDescription>{users.error instanceof ApiClientError && users.error.status === 403 ? '사용자 목록을 볼 권한이 없습니다.' : users.error.message}</AlertDescription><Button className="mt-3" onClick={() => users.refetch()} size="sm" variant="outline">다시 시도</Button></Alert>}
      {users.data?.content.length === 0 && <p>등록된 사용자가 없습니다.</p>}
      {users.data && users.data.content.length > 0 && <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b text-muted-foreground"><tr><th className="py-3">이름</th><th>이메일</th><th>상태</th><th>등록일</th></tr></thead><tbody className="divide-y">{users.data.content.map(user => <tr key={user.id}><td className="py-4 font-medium">{user.displayName}</td><td>{user.email}</td><td>{user.status}</td><td>{new Date(user.createdAt).toLocaleDateString('ko-KR')}</td></tr>)}</tbody></table></div>}
    </CardContent></Card>
  </>
}

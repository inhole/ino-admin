import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiClientError, getSamples } from './api/client'
import { useAuth } from './auth/useAuth'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { LayoutDashboard, LogOut } from 'lucide-react'

export function DashboardPage() {
  const { logout, user } = useAuth()
  const queryClient = useQueryClient()
  const samples = useQuery({ queryKey: ['samples'], queryFn: getSamples })
  const signOut = async () => {
    queryClient.clear()
    await logout().catch(() => undefined)
  }

  return (
    <div className="grid min-h-svh md:grid-cols-[260px_1fr]">
      <aside aria-label="주 메뉴" className="flex flex-col border-r bg-card p-5">
        <div className="mb-8 text-xl font-bold">INO Admin</div>
        <nav><a aria-current="page" className="flex items-center gap-3 rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground" href="/"><LayoutDashboard size={18} />대시보드</a></nav>
        <div className="mt-auto grid gap-3"><Separator /><div className="flex items-center gap-3"><Avatar><AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate text-sm font-medium">{user?.displayName}</p><p className="truncate text-xs text-muted-foreground">{user?.email}</p></div></div><Button className="justify-start" onClick={signOut} type="button" variant="outline"><LogOut size={16} />로그아웃</Button></div>
      </aside>
      <main className="p-6 md:p-10" id="main-content">
        <header className="mb-8"><p className="text-xs font-semibold tracking-[0.2em] text-primary">SYSTEM OVERVIEW</p><h1 className="text-3xl font-bold tracking-tight">관리자 시작 화면</h1></header>
        <Card aria-labelledby="connection-title">
          <CardHeader><CardTitle id="connection-title">백엔드 연결 상태</CardTitle><CardDescription>샘플 API의 현재 응답입니다.</CardDescription></CardHeader>
          <CardContent>
          {samples.isPending && <p role="status">불러오는 중…</p>}
          {samples.isError && <Alert variant="destructive" role="alert"><AlertTitle>연결 오류</AlertTitle><AlertDescription>{samples.error instanceof ApiClientError && samples.error.status === 403 ? '이 정보를 볼 권한이 없습니다.' : samples.error.message}</AlertDescription><Button className="mt-3" onClick={() => samples.refetch()} size="sm" variant="outline">다시 시도</Button></Alert>}
          {samples.data?.content.length === 0 && <p>표시할 항목이 없습니다.</p>}
          {samples.data && samples.data.content.length > 0 && <ul className="divide-y">{samples.data.content.map((sample) => <li className="flex justify-between py-4" key={sample.id}><span>{sample.name}</span><strong className="text-emerald-600">정상</strong></li>)}</ul>}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}

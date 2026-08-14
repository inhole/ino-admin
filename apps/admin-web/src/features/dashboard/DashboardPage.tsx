import { useQuery } from '@tanstack/react-query'
import { ApiClientError, getSamples } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function DashboardPage() {
  const samples = useQuery({ queryKey: ['samples'], queryFn: getSamples })

  return (
    <>
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
    </>
  )
}

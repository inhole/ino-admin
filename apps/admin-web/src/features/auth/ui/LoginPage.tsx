import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiClientError } from '@/api/client'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/features/auth/model/useAuth'

export function LoginPage() {
  const { isRestoring, login, user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isRestoring && user) return <Navigate replace to="/" />

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    const data = new FormData(event.currentTarget)
    try {
      await login(String(data.get('email')), String(data.get('password')))
      const target = typeof location.state?.from === 'string' ? location.state.from : '/'
      navigate(target, { replace: true })
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : '서버에 연결할 수 없습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-6">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader>
          <p className="text-xs font-semibold tracking-[0.2em] text-primary">SECURE ADMIN ACCESS</p>
          <CardTitle className="text-3xl"><h1 id="login-title">관리자 로그인</h1></CardTitle>
          <CardDescription>관리자 계정으로 로그인해 시스템을 관리하세요.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={submit}>
            <div className="grid gap-2"><label className="text-sm font-medium" htmlFor="email">이메일</label><Input autoComplete="username" id="email" name="email" required type="email" /></div>
            <div className="grid gap-2"><label className="text-sm font-medium" htmlFor="password">비밀번호</label><Input autoComplete="current-password" id="password" minLength={12} name="password" required type="password" /></div>
            {error && <Alert variant="destructive" role="alert"><AlertDescription>{error}</AlertDescription></Alert>}
            <Button className="mt-2 w-full" disabled={isSubmitting} type="submit">{isSubmitting ? '로그인 중…' : '로그인'}</Button>
          </form>
        </CardContent>
      </Card>
    </main>
  )
}

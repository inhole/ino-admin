import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { useAuth } from './useAuth'

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
    <main className="login-page">
      <section aria-labelledby="login-title" className="login-card">
        <p className="eyebrow">SECURE ADMIN ACCESS</p>
        <h1 id="login-title">관리자 로그인</h1>
        <p className="login-intro">관리자 계정으로 로그인해 시스템을 관리하세요.</p>
        <form onSubmit={submit}>
          <label htmlFor="email">이메일</label>
          <input autoComplete="username" id="email" name="email" required type="email" />
          <label htmlFor="password">비밀번호</label>
          <input autoComplete="current-password" id="password" minLength={12} name="password" required type="password" />
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" disabled={isSubmitting} type="submit">{isSubmitting ? '로그인 중…' : '로그인'}</button>
        </form>
      </section>
    </main>
  )
}

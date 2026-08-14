import { useQuery } from '@tanstack/react-query'
import { getSamples } from './api/client'
import './styles.css'

export function App() {
  const samples = useQuery({ queryKey: ['samples'], queryFn: getSamples })

  return (
    <div className="app-shell">
      <aside aria-label="주 메뉴" className="sidebar">
        <div className="brand">INO Admin</div>
        <nav><a aria-current="page" href="/">대시보드</a></nav>
      </aside>
      <main id="main-content">
        <header><p className="eyebrow">SYSTEM OVERVIEW</p><h1>관리자 시작 화면</h1></header>
        <section aria-labelledby="connection-title" className="panel">
          <div><h2 id="connection-title">백엔드 연결 상태</h2><p>샘플 API의 현재 응답입니다.</p></div>
          {samples.isPending && <p role="status">불러오는 중…</p>}
          {samples.isError && <div role="alert" className="error"><p>{samples.error.message}</p><button onClick={() => samples.refetch()}>다시 시도</button></div>}
          {samples.data?.content.length === 0 && <p>표시할 항목이 없습니다.</p>}
          {samples.data && samples.data.content.length > 0 && (
            <ul className="status-list">{samples.data.content.map((sample) => <li key={sample.id}><span>{sample.name}</span><strong>정상</strong></li>)}</ul>
          )}
        </section>
      </main>
    </div>
  )
}

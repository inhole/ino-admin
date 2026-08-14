import { useQueryClient } from '@tanstack/react-query'
import { LayoutDashboard, LogOut, Users } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/features/auth/model/useAuth'

export function AdminLayout() {
  const { logout, user } = useAuth()
  const queryClient = useQueryClient()
  const signOut = async () => {
    queryClient.clear()
    await logout().catch(() => undefined)
  }
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`

  return <div className="grid min-h-svh md:grid-cols-[260px_1fr]">
    <aside aria-label="주 메뉴" className="flex flex-col border-r bg-card p-5">
      <div className="mb-8 text-xl font-bold">INO Admin</div>
      <nav className="grid gap-1"><NavLink className={linkClass} end to="/"><LayoutDashboard size={18} />대시보드</NavLink><NavLink className={linkClass} to="/users"><Users size={18} />사용자</NavLink></nav>
      <div className="mt-auto grid gap-3"><Separator /><div className="flex items-center gap-3"><Avatar><AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate text-sm font-medium">{user?.displayName}</p><p className="truncate text-xs text-muted-foreground">{user?.email}</p></div></div><Button className="justify-start" onClick={signOut} type="button" variant="outline"><LogOut size={16} />로그아웃</Button></div>
    </aside>
    <main className="p-6 md:p-10" id="main-content"><Outlet /></main>
  </div>
}

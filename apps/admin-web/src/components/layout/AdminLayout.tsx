import { useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, LayoutDashboard, LogOut, Users } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/features/auth/model/useAuth'
import { getMyMenus } from '@/api/client'

export function AdminLayout() {
  const { logout, user } = useAuth()
  const queryClient = useQueryClient()
  const menus = useQuery({ queryKey: ['menus', 'me'], queryFn: getMyMenus, enabled: Boolean(user) })
  const signOut = async () => {
    queryClient.clear()
    await logout().catch(() => undefined)
  }
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`

  return <div className="grid min-h-svh md:grid-cols-[260px_1fr]">
    <aside aria-label="주 메뉴" className="flex flex-col border-r bg-card p-5">
      <div className="mb-8 text-xl font-bold">INO Admin</div>
      <nav aria-busy={menus.isPending} className="grid gap-1">{menus.data?.map(menu => { const Icon = menu.icon === 'users' ? Users : menu.icon === 'key-round' ? KeyRound : LayoutDashboard; return <NavLink className={linkClass} end={menu.route === '/'} key={menu.id} to={menu.route}><Icon size={18} />{menu.label}</NavLink> })}{menus.isError && <p className="px-3 text-sm text-destructive">메뉴를 불러올 수 없습니다.</p>}</nav>
      <div className="mt-auto grid gap-3"><Separator /><div className="flex items-center gap-3"><Avatar><AvatarFallback>{user?.displayName?.slice(0, 1)}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate text-sm font-medium">{user?.displayName}</p><p className="truncate text-xs text-muted-foreground">{user?.email}</p></div></div><Button className="justify-start" onClick={signOut} type="button" variant="outline"><LogOut size={16} />로그아웃</Button></div>
    </aside>
    <main className="p-6 md:p-10" id="main-content"><Outlet /></main>
  </div>
}

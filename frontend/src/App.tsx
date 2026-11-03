import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthProvider'
import { AppShell } from './components/AppShell'
import { LoginPage } from './pages/LoginPage'
import { SpaceFormPage } from './pages/SpaceFormPage'
import { SpacesPage } from './pages/SpacesPage'

function AdminOnly({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? children : <Navigate to="/espaces" replace />
}

export function App() {
  const { user, loading } = useAuth()
  if (loading) return <div className="boot-loading"><span className="spinner"/>Chargement de votre session…</div>
  if (!user) return <LoginPage />
  return <Routes><Route element={<AppShell/>}>
    <Route path="/" element={<Navigate to="/espaces" replace/>}/>
    <Route path="/espaces" element={<SpacesPage/>}/>
    <Route path="/espaces/nouveau" element={<AdminOnly><SpaceFormPage/></AdminOnly>}/>
    <Route path="/espaces/:id/modifier" element={<AdminOnly><SpaceFormPage/></AdminOnly>}/>
    <Route path="*" element={<Navigate to="/espaces" replace/>}/>
  </Route></Routes>
}

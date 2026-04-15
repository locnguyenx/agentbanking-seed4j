import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { api } from '../services/api'

interface User {
  id: string
  username: string
  role: 'VIEWER' | 'OPERATOR' | 'ADMIN'
  name: string
}

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  hasPermission: (permission: string) => boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const PERMISSIONS = {
  VIEWER: ['view_dashboard', 'view_agents', 'view_transactions', 'view_settlement', 'view_audit'],
  OPERATOR: ['view_dashboard', 'view_agents', 'view_transactions', 'view_settlement', 'view_audit', 'approve_ekyc'],
  ADMIN: ['view_dashboard', 'view_agents', 'view_transactions', 'view_settlement', 'view_audit', 'approve_ekyc', 'manage_agents', 'manage_config'],
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (token) {
      api.get<User>('/auth/me')
        .then(data => setUser(data))
        .catch(() => localStorage.removeItem('token'))
        .finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [])

  const login = async (username: string, password: string) => {
    const data = await api.post<{ token: string; user: User }>('/auth/login', { username, password })
    localStorage.setItem('token', data.token)
    setUser(data.user)
  }

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
  }

  const hasPermission = (permission: string) => {
    if (!user) return false
    return PERMISSIONS[user.role]?.includes(permission) ?? false
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
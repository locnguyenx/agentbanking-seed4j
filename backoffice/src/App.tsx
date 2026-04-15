import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Agents from './pages/Agents'
import Transactions from './pages/Transactions'
import Settlement from './pages/Settlement'
import EKycReview from './pages/EKycReview'
import AuditLogs from './pages/AuditLogs'
import Compliance from './pages/Compliance'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div>Loading...</div>
  if (!user) return <Navigate to="/login" />
  return <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <Layout>
              <Routes>
                <Route path="/" element={<Navigate to="/dashboard\" />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/agents" element={<Agents />} />
                <Route path="/transactions" element={<Transactions />} />
                <Route path="/settlement" element={<Settlement />} />
                <Route path="/ekyc" element={<EKycReview />} />
                <Route path="/audit" element={<AuditLogs />} />
                <Route path="/compliance" element={<Compliance />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}
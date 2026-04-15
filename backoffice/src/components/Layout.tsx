import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: '📊' },
  { path: '/agents', label: 'Agents', icon: '👥' },
  { path: '/transactions', label: 'Transactions', icon: '💳' },
  { path: '/settlement', label: 'Settlement', icon: '🏦' },
  { path: '/ekyc', label: 'e-KYC Review', icon: '📋' },
  { path: '/audit', label: 'Audit Logs', icon: '📝' },
  { path: '/compliance', label: 'Compliance', icon: '✅' },
]

export default function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1>Agent Banking</h1>
        </div>
        <nav className="sidebar-nav">
          {navItems.map(item => (
            <NavLink key={item.path} to={item.path} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-name">{user?.name}</span>
            <span className="user-role">{user?.role}</span>
          </div>
          <button onClick={handleLogout} className="btn btn-secondary btn-sm">Logout</button>
        </div>
      </aside>
      <main className="main">{children}</main>
      <style>{`
        .layout {
          display: flex;
          min-height: 100vh;
        }
        .sidebar {
          width: 240px;
          background: #1a1a2e;
          color: white;
          display: flex;
          flex-direction: column;
          position: fixed;
          height: 100vh;
        }
        .sidebar-header {
          padding: 20px;
          border-bottom: 1px solid rgba(255,255,255,0.1);
        }
        .sidebar-header h1 {
          font-size: 18px;
          font-weight: 600;
        }
        .sidebar-nav {
          flex: 1;
          padding: 12px;
        }
        .nav-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 16px;
          border-radius: 6px;
          color: rgba(255,255,255,0.7);
          transition: all 0.2s;
          margin-bottom: 4px;
        }
        .nav-item:hover {
          background: rgba(255,255,255,0.1);
          color: white;
        }
        .nav-item.active {
          background: var(--primary);
          color: white;
        }
        .nav-icon {
          font-size: 16px;
        }
        .sidebar-footer {
          padding: 16px;
          border-top: 1px solid rgba(255,255,255,0.1);
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .user-info {
          display: flex;
          flex-direction: column;
        }
        .user-name {
          font-size: 14px;
          font-weight: 500;
        }
        .user-role {
          font-size: 12px;
          color: rgba(255,255,255,0.6);
        }
        .main {
          flex: 1;
          margin-left: 240px;
          min-height: 100vh;
          background: var(--bg);
        }
      `}</style>
    </div>
  )
}
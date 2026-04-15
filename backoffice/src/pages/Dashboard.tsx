import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface DashboardStats {
  totalAgents: number
  activeAgents: number
  todayTransactions: number
  todayVolume: number
  pendingEKyc: number
  pendingSettlement: number
}

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalAgents: 0,
    activeAgents: 0,
    todayTransactions: 0,
    todayVolume: 0,
    pendingEKyc: 0,
    pendingSettlement: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<DashboardStats>('/dashboard/stats')
      .then(data => setStats(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const formatCurrency = (val: number) => new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR' }).format(val / 100)

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Dashboard</h1>
        <p>Overview of Agent Banking operations</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="label">Total Agents</div>
          <div className="value">{stats.totalAgents}</div>
        </div>
        <div className="stat-card">
          <div className="label">Active Agents</div>
          <div className="value">{stats.activeAgents}</div>
        </div>
        <div className="stat-card">
          <div className="label">Today's Transactions</div>
          <div className="value">{stats.todayTransactions}</div>
        </div>
        <div className="stat-card">
          <div className="label">Today's Volume</div>
          <div className="value">{formatCurrency(stats.todayVolume)}</div>
        </div>
        <div className="stat-card">
          <div className="label">Pending e-KYC</div>
          <div className="value">{stats.pendingEKyc}</div>
        </div>
        <div className="stat-card">
          <div className="label">Pending Settlement</div>
          <div className="value">{stats.pendingSettlement}</div>
        </div>
      </div>
    </div>
  )
}
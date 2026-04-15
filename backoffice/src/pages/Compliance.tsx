import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface ComplianceMetric {
  name: string
  value: number
  target: number
  status: 'OK' | 'WARNING' | 'FAIL'
}

export default function Compliance() {
  const [metrics, setMetrics] = useState<ComplianceMetric[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<ComplianceMetric[]>('/compliance/metrics')
      .then(data => setMetrics(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Compliance Dashboard</h1>
        <p>Monitor regulatory compliance metrics</p>
      </div>

      <div className="stats-grid">
        {metrics.map(metric => (
          <div key={metric.name} className="stat-card">
            <div className="label">{metric.name}</div>
            <div className="value" style={{ color: metric.status === 'FAIL' ? 'var(--danger)' : metric.status === 'WARNING' ? 'var(--warning)' : 'var(--success)' }}>
              {metric.value}
            </div>
            <div className="trend">
              Target: {metric.target}
              <span className={`badge ${metric.status === 'OK' ? 'badge-success' : metric.status === 'WARNING' ? 'badge-warning' : 'badge-danger'}`} style={{ marginLeft: 8 }}>
                {metric.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
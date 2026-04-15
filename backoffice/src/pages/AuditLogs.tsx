import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface AuditLog {
  id: string
  timestamp: string
  userId: string
  userName: string
  action: string
  resource: string
  details: string
}

export default function AuditLogs() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')

  useEffect(() => {
    api
      .get<AuditLog[]>('/audit')
      .then(data => setLogs(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const filteredLogs = logs.filter(log => {
    return !filter || log.action.toLowerCase().includes(filter.toLowerCase()) || log.userName.toLowerCase().includes(filter.toLowerCase())
  })

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Audit Logs</h1>
        <p>View system activity logs</p>
      </div>

      <div className="filters">
        <div className="form-group">
          <input type="text" placeholder="Search logs..." value={filter} onChange={e => setFilter(e.target.value)} />
        </div>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>User</th>
              <th>Action</th>
              <th>Resource</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {filteredLogs.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty">No audit logs found</td>
              </tr>
            ) : (
              filteredLogs.map(log => (
                <tr key={log.id}>
                  <td>{new Date(log.timestamp).toLocaleString()}</td>
                  <td>{log.userName}</td>
                  <td>{log.action}</td>
                  <td>{log.resource}</td>
                  <td>{log.details}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
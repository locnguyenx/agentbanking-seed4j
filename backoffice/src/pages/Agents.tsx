import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface Agent {
  id: string
  name: string
  mykad: string
  phone: string
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED'
  location: string
  createdAt: string
}

export default function Agents() {
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')

  useEffect(() => {
    api
      .get<Agent[]>('/agents')
      .then(data => setAgents(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const filteredAgents = agents.filter(a => {
    const matchSearch = !filter || a.name.toLowerCase().includes(filter.toLowerCase()) || a.phone.includes(filter)
    const matchStatus = !statusFilter || a.status === statusFilter
    return matchSearch && matchStatus
  })

  const handleStatusChange = async (id: string, status: string) => {
    try {
      await api.put(`/agents/${id}`, { status })
      setAgents(agents.map(a => (a.id === id ? { ...a, status: status as Agent['status'] } : a)))
    } catch (err) {
      console.error(err)
    }
  }

  const getStatusBadge = (status: string) => {
    const classes: Record<string, string> = {
      PENDING: 'badge badge-warning',
      ACTIVE: 'badge badge-success',
      SUSPENDED: 'badge badge-danger',
      TERMINATED: 'badge badge-danger',
    }
    return classes[status] || 'badge'
  }

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Agents</h1>
        <p>Manage agent accounts and profiles</p>
      </div>

      <div className="action-bar">
        <div className="filters">
          <div className="form-group">
            <input type="text" placeholder="Search by name or phone..." value={filter} onChange={e => setFilter(e.target.value)} />
          </div>
          <div className="form-group">
            <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
              <option value="">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="TERMINATED">Terminated</option>
            </select>
          </div>
        </div>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>MyKad</th>
              <th>Phone</th>
              <th>Location</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredAgents.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty">No agents found</td>
              </tr>
            ) : (
              filteredAgents.map(agent => (
                <tr key={agent.id}>
                  <td>{agent.name}</td>
                  <td>{agent.mykad}</td>
                  <td>{agent.phone}</td>
                  <td>{agent.location}</td>
                  <td>
                    <span className={getStatusBadge(agent.status)}>{agent.status}</span>
                  </td>
                  <td>{new Date(agent.createdAt).toLocaleDateString()}</td>
                  <td>
                    <select value={agent.status} onChange={e => handleStatusChange(agent.id, e.target.value)} className="btn btn-secondary btn-sm">
                      <option value="PENDING">Pending</option>
                      <option value="ACTIVE">Active</option>
                      <option value="SUSPENDED">Suspend</option>
                      <option value="TERMINATED">Terminate</option>
                    </select>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
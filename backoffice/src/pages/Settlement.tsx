import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface SettlementRecord {
  id: string
  agentId: string
  agentName: string
  date: string
  totalCredit: number
  totalDebit: number
  commission: number
  status: 'PENDING' | 'APPROVED' | 'PAID'
}

export default function Settlement() {
  const [records, setRecords] = useState<SettlementRecord[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<SettlementRecord[]>('/settlement')
      .then(data => setRecords(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const formatCurrency = (val: number) => new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR' }).format(val / 100)

  const handleApprove = async (id: string) => {
    try {
      await api.post(`/settlement/${id}/approve`, {})
      setRecords(records.map(r => (r.id === id ? { ...r, status: 'APPROVED' as const } : r)))
    } catch (err) {
      console.error(err)
    }
  }

  const handlePay = async (id: string) => {
    try {
      await api.post(`/settlement/${id}/pay`, {})
      setRecords(records.map(r => (r.id === id ? { ...r, status: 'PAID' as const } : r)))
    } catch (err) {
      console.error(err)
    }
  }

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Settlement</h1>
        <p>Manage agent settlements and commissions</p>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Agent</th>
              <th>Date</th>
              <th>Total Credit</th>
              <th>Total Debit</th>
              <th>Commission</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {records.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty">No settlements found</td>
              </tr>
            ) : (
              records.map(rec => (
                <tr key={rec.id}>
                  <td>{rec.agentName}</td>
                  <td>{rec.date}</td>
                  <td>{formatCurrency(rec.totalCredit)}</td>
                  <td>{formatCurrency(rec.totalDebit)}</td>
                  <td>{formatCurrency(rec.commission)}</td>
                  <td>
                    <span className={`badge ${rec.status === 'PAID' ? 'badge-success' : rec.status === 'APPROVED' ? 'badge-info' : 'badge-warning'}`}>
                      {rec.status}
                    </span>
                  </td>
                  <td>
                    {rec.status === 'PENDING' && (
                      <button onClick={() => handleApprove(rec.id)} className="btn btn-primary btn-sm">
                        Approve
                      </button>
                    )}
                    {rec.status === 'APPROVED' && (
                      <button onClick={() => handlePay(rec.id)} className="btn btn-primary btn-sm">
                        Mark Paid
                      </button>
                    )}
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
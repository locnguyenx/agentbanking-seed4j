import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface EKycRecord {
  id: string
  agentId: string
  agentName: string
  mykad: string
  submittedAt: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
}

export default function EKycReview() {
  const [records, setRecords] = useState<EKycRecord[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<EKycRecord[]>('/ekyc')
      .then(data => setRecords(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const handleApprove = async (id: string) => {
    try {
      await api.post(`/ekyc/${id}/approve`, {})
      setRecords(records.map(r => (r.id === id ? { ...r, status: 'APPROVED' as const } : r)))
    } catch (err) {
      console.error(err)
    }
  }

  const handleReject = async (id: string) => {
    try {
      await api.post(`/ekyc/${id}/reject`, {})
      setRecords(records.map(r => (r.id === id ? { ...r, status: 'REJECTED' as const } : r)))
    } catch (err) {
      console.error(err)
    }
  }

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>e-KYC Review</h1>
        <p>Review and approve agent digital onboarding</p>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Agent</th>
              <th>MyKad</th>
              <th>Submitted</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {records.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty">No pending e-KYC submissions</td>
              </tr>
            ) : (
              records.map(rec => (
                <tr key={rec.id}>
                  <td>{rec.agentName}</td>
                  <td>{rec.mykad}</td>
                  <td>{new Date(rec.submittedAt).toLocaleString()}</td>
                  <td>
                    <span className={`badge ${rec.status === 'APPROVED' ? 'badge-success' : rec.status === 'REJECTED' ? 'badge-danger' : 'badge-warning'}`}>
                      {rec.status}
                    </span>
                  </td>
                  <td>
                    {rec.status === 'PENDING' && (
                      <>
                        <button onClick={() => handleApprove(rec.id)} className="btn btn-primary btn-sm" style={{ marginRight: 8 }}>
                          Approve
                        </button>
                        <button onClick={() => handleReject(rec.id)} className="btn btn-danger btn-sm">
                          Reject
                        </button>
                      </>
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
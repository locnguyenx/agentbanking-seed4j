import { useState, useEffect } from 'react'
import { api } from '../services/api'

interface Transaction {
  id: string
  ref: string
  agentId: string
  agentName: string
  type: 'CASH_IN' | 'CASH_OUT' | 'PAYMENT' | 'BILL_PAYMENT' | 'BALANCE_INQUIRY'
  amount: number
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
  responseCode: string
  createdAt: string
}

export default function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')

  useEffect(() => {
    api
      .get<Transaction[]>('/transactions')
      .then(data => setTransactions(data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const filteredTransactions = transactions.filter(t => {
    const matchSearch = !filter || t.ref.includes(filter) || t.agentName.toLowerCase().includes(filter.toLowerCase())
    const matchStatus = !statusFilter || t.status === statusFilter
    const matchType = !typeFilter || t.type === typeFilter
    return matchSearch && matchStatus && matchType
  })

  const formatCurrency = (val: number) => new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR' }).format(val / 100)

  const getStatusBadge = (status: string) => {
    const classes: Record<string, string> = {
      PENDING: 'badge badge-warning',
      PROCESSING: 'badge badge-info',
      SUCCESS: 'badge badge-success',
      FAILED: 'badge badge-danger',
    }
    return classes[status] || 'badge'
  }

  if (loading) return <div className="page">Loading...</div>

  return (
    <div className="page">
      <div className="page-header">
        <h1>Transactions</h1>
        <p>Monitor transaction activity</p>
      </div>

      <div className="filters">
        <div className="form-group">
          <input type="text" placeholder="Search by ref or agent..." value={filter} onChange={e => setFilter(e.target.value)} />
        </div>
        <div className="form-group">
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="PROCESSING">Processing</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
        <div className="form-group">
          <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
            <option value="">All Types</option>
            <option value="CASH_IN">Cash In</option>
            <option value="CASH_OUT">Cash Out</option>
            <option value="PAYMENT">Payment</option>
            <option value="BILL_PAYMENT">Bill Payment</option>
            <option value="BALANCE_INQUIRY">Balance Inquiry</option>
          </select>
        </div>
      </div>

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Ref</th>
              <th>Agent</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Response</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {filteredTransactions.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty">No transactions found</td>
              </tr>
            ) : (
              filteredTransactions.map(tx => (
                <tr key={tx.id}>
                  <td>{tx.ref}</td>
                  <td>{tx.agentName}</td>
                  <td>{tx.type}</td>
                  <td>{formatCurrency(tx.amount)}</td>
                  <td>
                    <span className={getStatusBadge(tx.status)}>{tx.status}</span>
                  </td>
                  <td>{tx.responseCode}</td>
                  <td>{new Date(tx.createdAt).toLocaleString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
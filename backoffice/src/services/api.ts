const API_BASE = import.meta.env.VITE_API_URL || '/api'

class ApiClient {
  private baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }

  private getHeaders() {
    const token = localStorage.getItem('token')
    return {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
  }

  async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const res = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: { message: 'Request failed' } }))
      throw new Error(err.error?.message || 'Request failed')
    }
    const data = await res.json()
    return data.data as T
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path)
  }

  post<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>('POST', path, body)
  }

  put<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>('PUT', path, body)
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>('DELETE', path)
  }
}

export const api = new ApiClient(API_BASE)
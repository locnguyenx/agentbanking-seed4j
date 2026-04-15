import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '../services/api'

const fetchSpy = vi.fn()
vi.stubGlobal('fetch', fetchSpy)

describe('ApiClient', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should make GET requests correctly', async () => {
    fetchSpy.mockResolvedValue({
      ok: true,
      json: async () => ({ data: { id: '1', name: 'Test' } }),
    })

    const result = await api.get<{ id: string; name: string }>('/test')

    expect(fetchSpy).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      method: 'GET',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
      }),
    }))
    expect(result).toEqual({ id: '1', name: 'Test' })
  })

  it('should make POST requests correctly', async () => {
    fetchSpy.mockResolvedValue({
      ok: true,
      json: async () => ({ data: { id: '1', name: 'Created' } }),
    })

    const result = await api.post<{ id: string; name: string }>('/test', { name: 'test' })

    expect(fetchSpy).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ name: 'test' }),
    }))
    expect(result).toEqual({ id: '1', name: 'Created' })
  })

  it('should include auth token when present', async () => {
    localStorage.setItem('token', 'test-token')
    fetchSpy.mockResolvedValue({
      ok: true,
      json: async () => ({ data: {} }),
    })

    await api.get('/test')

    expect(fetchSpy).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      headers: expect.objectContaining({
        Authorization: 'Bearer test-token',
      }),
    }))
  })

  it('should throw error on failed request', async () => {
    fetchSpy.mockResolvedValue({
      ok: false,
      json: async () => ({ error: { message: 'Bad request' } }),
    })

    await expect(api.get('/test')).rejects.toThrow('Bad request')
  })
})
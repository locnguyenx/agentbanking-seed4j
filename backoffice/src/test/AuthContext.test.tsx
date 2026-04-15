import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { AuthProvider, useAuth } from '../contexts/AuthContext'

vi.mock('../services/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import { api } from '../services/api'

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should provide initial null user when no token', () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    expect(result.current.user).toBeNull()
  })

  it('should load user from token on mount', async () => {
    const mockUser = { id: '1', username: 'admin', role: 'ADMIN' as const, name: 'Admin' }
    ;(api.get as ReturnType<typeof vi.fn>).mockResolvedValue(mockUser)
    localStorage.setItem('token', 'test-token')

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.user).toEqual(mockUser)
  })

  it('should login successfully', async () => {
    const mockUser = { id: '1', username: 'admin', role: 'ADMIN' as const, name: 'Admin' }
    ;(api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ token: 'test-token', user: mockUser })

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    await act(async () => {
      await result.current.login('admin', 'password')
    })

    expect(localStorage.getItem('token')).toBe('test-token')
    expect(result.current.user).toEqual(mockUser)
  })

  it('should logout and clear token', async () => {
    localStorage.setItem('token', 'test-token')

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    act(() => {
      result.current.logout()
    })

    expect(localStorage.getItem('token')).toBeNull()
    expect(result.current.user).toBeNull()
  })

  it('should check permissions correctly', async () => {
    const mockUser = { id: '1', username: 'admin', role: 'OPERATOR' as const, name: 'Admin' }
    ;(api.get as ReturnType<typeof vi.fn>).mockResolvedValue(mockUser)
    localStorage.setItem('token', 'test-token')

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.hasPermission('view_dashboard')).toBe(true)
    expect(result.current.hasPermission('approve_ekyc')).toBe(true)
    expect(result.current.hasPermission('manage_agents')).toBe(false)
  })

  it('should return false for permissions when not logged in', () => {
    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    })

    expect(result.current.hasPermission('view_dashboard')).toBe(false)
  })
})
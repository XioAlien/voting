import http from '../api/http'
import { getEnvelopeErrorMessage, getUserFacingErrorMessage } from './errors'

const TOKEN_KEY = 'token'

interface ApiEnvelope<T> {
  success?: boolean
  message?: string
  data?: T
}

interface ConfirmTokenPayload {
  token: string
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function hasStoredToken() {
  return Boolean(getStoredToken())
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getApiErrorMessage(error: unknown, fallback: string) {
  return getUserFacingErrorMessage(error, fallback)
}

export async function probeAdminAccess() {
  if (!hasStoredToken()) {
    return false
  }

  try {
    const res = await http.get<ApiEnvelope<unknown>>('/api/admin/dashboard')
    return Boolean(res.data?.success)
  } catch (error) {
    const status = (error as { response?: { status?: number } })?.response?.status
    if (status === 401 || status === 403) {
      return false
    }
    throw error
  }
}

export async function issueAdminConfirmToken(action: string, target: string) {
  const res = await http.post<ApiEnvelope<ConfirmTokenPayload>>('/api/admin/confirm', {
    action,
    target,
  })

  const token = res.data?.data?.token
  if (!res.data?.success || !token) {
    throw new Error(getEnvelopeErrorMessage(res.data?.message, '获取确认信息失败'))
  }

  return token
}

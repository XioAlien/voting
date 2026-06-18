import axios, { AxiosHeaders } from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

const http = axios.create({
  baseURL: apiBaseUrl || undefined,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }
  return config
})

export default http

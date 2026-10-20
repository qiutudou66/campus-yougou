import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'campus_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.authorization = token
  return config
})

function redirectToLogin() {
  localStorage.removeItem(TOKEN_KEY)
  if (window.location.pathname === '/login') return
  const current = window.location.pathname + window.location.search
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'success' in body) {
      if (!body.success) {
        const message = body.errorMsg || '操作失败'
        if (!response.config.silent) ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const config = error.config || {}
    const status = error.response?.status

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      if (!config.silent) {
        ElMessage.warning('请先登录')
        redirectToLogin()
      }
      return Promise.reject(error)
    }

    if (!config.silent) {
      const message = error.code === 'ECONNABORTED'
        ? '请求超时，请稍后再试'
        : error.response
          ? '服务器开小差了，请稍后再试'
          : '网络异常，请确认后端服务已启动'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request

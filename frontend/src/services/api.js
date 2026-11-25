import axios from 'axios'
import { ElMessage } from 'element-plus'

const ADMIN_TOKEN_KEY = 'adha_admin_token'
const ADMIN_USER_KEY = 'adha_admin_user'
const CLIENT_TOKEN_KEY = 'adha_client_token'
const CLIENT_USER_KEY = 'adha_client_user'

function isAdminContext() {
  const path = window.location.pathname || ''
  return path.startsWith('/admin') || path.startsWith('/pos')
}

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000
})

apiClient.interceptors.request.use(
  (config) => {
    const tokenKey = isAdminContext() ? ADMIN_TOKEN_KEY : CLIENT_TOKEN_KEY
    const token = localStorage.getItem(tokenKey)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      const status = error.response.status
      const msg = error.response.data?.message || 'Có lỗi xảy ra từ máy chủ'
      const inAdmin = isAdminContext()

      if (status === 401) {
        if (inAdmin && !window.location.pathname.startsWith('/admin/login')) {
          localStorage.removeItem(ADMIN_TOKEN_KEY)
          localStorage.removeItem(ADMIN_USER_KEY)
          ElMessage.error(msg)
          window.location.href = '/admin/login'
        } else if (!inAdmin) {
          localStorage.removeItem(CLIENT_TOKEN_KEY)
          localStorage.removeItem(CLIENT_USER_KEY)
          ElMessage.error(msg)
        } else {
          ElMessage.error(msg)
        }
      } else {
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('Không thể kết nối đến máy chủ Backend')
    }
    return Promise.reject(error)
  }
)

export default apiClient

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '@/services/authService'

const ADMIN_TOKEN_KEY = 'adha_admin_token'
const ADMIN_USER_KEY = 'adha_admin_user'
const INTERNAL_ROLES = ['ADMIN', 'MANAGER', 'STAFF']

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(ADMIN_TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(ADMIN_USER_KEY) || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const roles = computed(() => user.value?.roles || [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isManager = computed(() => roles.value.includes('MANAGER') || roles.value.includes('ADMIN'))
  const isStaff = computed(() => roles.value.includes('STAFF'))
  const isInternalStaff = computed(() => roles.value.some((r) => INTERNAL_ROLES.includes(r)))

  async function login(username, password) {
    const res = await authService.login({ username, password })
    const userRoles = res.data?.user?.roles || []
    const allowed = userRoles.some((r) => INTERNAL_ROLES.includes(r))
    if (!allowed) {
      throw new Error('Tài khoản khách hàng không có quyền truy cập cổng Quản trị!')
    }
    token.value = res.data.accessToken
    user.value = res.data.user
    localStorage.setItem(ADMIN_TOKEN_KEY, token.value)
    localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user.value))
    return res.data
  }

  async function logout() {
    try {
      await authService.logout()
    } catch (_) {
      // ignore
    } finally {
      token.value = ''
      user.value = null
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_USER_KEY)
    }
  }

  async function updateProfile(payload) {
    const res = await authService.updateProfile(payload)
    user.value = res.data
    localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user.value))
    return res.data
  }

  async function fetchCurrentUser() {
    if (!token.value) return null
    try {
      const res = await authService.getCurrentUser()
      user.value = res.data
      localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(user.value))
      return res.data
    } catch (_) {
      return null
    }
  }

  function hasAnyRole(allowedRoles) {
    if (!allowedRoles || allowedRoles.length === 0) return true
    return allowedRoles.some((r) => roles.value.includes(r))
  }

  return {
    token,
    user,
    isAuthenticated,
    roles,
    isAdmin,
    isManager,
    isStaff,
    isInternalStaff,
    login,
    logout,
    updateProfile,
    fetchCurrentUser,
    hasAnyRole
  }
})

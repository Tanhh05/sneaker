import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '@/services/authService'

const CLIENT_TOKEN_KEY = 'adha_client_token'
const CLIENT_USER_KEY = 'adha_client_user'
const INTERNAL_ONLY_ROLES = ['ADMIN', 'MANAGER', 'STAFF']

export const useClientAuthStore = defineStore('clientAuth', () => {
  const token = ref(localStorage.getItem(CLIENT_TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(CLIENT_USER_KEY) || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const roles = computed(() => user.value?.roles || [])
  const isCustomer = computed(() => roles.value.includes('CUSTOMER'))

  async function login(username, password) {
    const res = await authService.login({ username, password })
    const userRoles = res.data?.user?.roles || []
    const isOnlyInternal =
      userRoles.some((r) => INTERNAL_ONLY_ROLES.includes(r)) && !userRoles.includes('CUSTOMER')
    if (isOnlyInternal) {
      throw new Error('Đây là tài khoản nội bộ quản trị. Vui lòng đăng nhập tại cổng /admin/login!')
    }
    token.value = res.data.accessToken
    user.value = res.data.user
    localStorage.setItem(CLIENT_TOKEN_KEY, token.value)
    localStorage.setItem(CLIENT_USER_KEY, JSON.stringify(user.value))
    return res.data
  }

  async function register(payload) {
    const res = await authService.register(payload)
    token.value = res.data.accessToken
    user.value = res.data.user
    localStorage.setItem(CLIENT_TOKEN_KEY, token.value)
    localStorage.setItem(CLIENT_USER_KEY, JSON.stringify(user.value))
    return res.data
  }

  async function loginWithGoogle(payload) {
    const res = await authService.loginWithGoogle(payload)
    token.value = res.data.accessToken
    user.value = res.data.user
    localStorage.setItem(CLIENT_TOKEN_KEY, token.value)
    localStorage.setItem(CLIENT_USER_KEY, JSON.stringify(user.value))
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
      localStorage.removeItem(CLIENT_TOKEN_KEY)
      localStorage.removeItem(CLIENT_USER_KEY)
    }
  }

  async function updateProfile(payload) {
    const res = await authService.updateProfile(payload)
    user.value = res.data
    localStorage.setItem(CLIENT_USER_KEY, JSON.stringify(user.value))
    return res.data
  }

  async function fetchCurrentUser() {
    if (!token.value) return null
    try {
      const res = await authService.getCurrentUser()
      user.value = res.data
      localStorage.setItem(CLIENT_USER_KEY, JSON.stringify(user.value))
      return res.data
    } catch (_) {
      return null
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    roles,
    isCustomer,
    login,
    register,
    loginWithGoogle,
    logout,
    updateProfile,
    fetchCurrentUser
  }
})

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getCurrentUser } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || '')

  async function doLogin(credentials) {
    const res = await login(credentials)
    token.value = res.token
    refreshToken.value = res.refreshToken
    localStorage.setItem('token', res.token)
    localStorage.setItem('refreshToken', res.refreshToken)
    user.value = { id: res.id, username: res.username, realName: res.realName, role: res.role }
    return res
  }

  async function fetchUser() {
    try {
      const res = await getCurrentUser()
      user.value = res
    } catch {
      doLogout()
    }
  }

  function doLogout() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    router.push('/login')
  }

  function hasRole(...roles) {
    return roles.includes(user.value?.role)
  }

  return { token, refreshToken, user, isLoggedIn, role, doLogin, fetchUser, doLogout, hasRole }
})

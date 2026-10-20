import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/api/request'
import { getMe, logout as logoutApi } from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(null)
  const loadingUser = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))

  function setToken(value) {
    token.value = value || ''
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function clear() {
    setToken('')
    user.value = null
  }

  async function fetchMe(force = false) {
    if (!token.value) return null
    if (user.value && !force) return user.value
    if (loadingUser.value) return user.value
    loadingUser.value = true
    try {
      user.value = await getMe({ silent: true })
      return user.value
    } catch (error) {
      if (error?.response?.status === 401) clear()
      return null
    } finally {
      loadingUser.value = false
    }
  }

  async function logout() {
    try {
      await logoutApi({ silent: true })
    } catch (error) {
      // 后端 logout 目前是占位实现，前端仍然清掉本地登录态
    }
    clear()
  }

  return { token, user, isLoggedIn, loadingUser, setToken, clear, fetchMe, logout }
})

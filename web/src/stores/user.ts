import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { clearToken, getToken, setToken } from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const loggedIn = ref(!!getToken())
  const username = ref('')

  async function login(u: string, p: string) {
    const res = await authApi.login(u, p)
    setToken(res.token)
    loggedIn.value = true
    username.value = res.username
  }

  /** 刷新页面后用 token 校验一次登录状态 */
  async function fetchMe() {
    if (!getToken()) return
    const res = await authApi.me()
    loggedIn.value = res.loggedIn
    username.value = res.username
    if (!res.loggedIn) clearToken()
  }

  async function logout() {
    await authApi.logout().catch(() => undefined)
    clearToken()
    loggedIn.value = false
    username.value = ''
  }

  return { loggedIn, username, login, fetchMe, logout }
})

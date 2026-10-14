import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { clearToken, getToken, setToken } from '@/utils/request'
import type { Me, Photographer, User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const me = ref<Me>({ loggedIn: false, user: null, photographer: null, admin: false })
  let loading: Promise<void> | null = null

  const loggedIn = computed(() => me.value.loggedIn)
  const user = computed(() => me.value.user)
  const photographer = computed(() => me.value.photographer)
  const isAdmin = computed(() => me.value.admin)
  /** 填了价格才会出现在首页 */
  const isPhotographerReady = computed(() => !!me.value.photographer?.pricePerHour)

  /** 刷新页面后用 token 取一次当前用户，路由守卫里调用，只请求一次 */
  function ensure() {
    if (!getToken()) return Promise.resolve()
    loading ??= fetchMe(true).catch(() => undefined)
    return loading
  }

  /** silent：后台静默校验登录态时不弹错误 */
  async function fetchMe(silent = false) {
    me.value = await authApi.me({ silent })
    if (!me.value.loggedIn) clearToken()
  }

  async function login(phone: string, password: string) {
    const res = await authApi.login(phone, password)
    setToken(res.token)
    me.value = res.me
  }

  async function register(data: Parameters<typeof authApi.register>[0]) {
    const res = await authApi.register(data)
    setToken(res.token)
    me.value = res.me
  }

  async function logout() {
    await authApi.logout({ silent: true }).catch(() => undefined)
    clearToken()
    loading = null
    me.value = { loggedIn: false, user: null, photographer: null, admin: false }
  }

  function setUser(u: User) {
    me.value = { ...me.value, user: u }
  }

  function setPhotographer(p: Photographer) {
    me.value = { ...me.value, photographer: p }
  }

  return { me, loggedIn, user, photographer, isAdmin, isPhotographerReady, ensure, fetchMe, login, register, logout, setUser, setPhotographer }
})

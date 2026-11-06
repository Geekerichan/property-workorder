import { defineStore } from 'pinia'
import { authApi } from '@/api'

interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  phone?: string
  companyId?: number
  personnelId?: number
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('wo_token') || '',
    user: JSON.parse(localStorage.getItem('wo_user') || 'null') as UserInfo | null,
    unread: 0
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.user?.role || ''
  },
  actions: {
    async login(username: string, password: string) {
      const data: any = await authApi.login({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('wo_token', data.token)
      localStorage.setItem('wo_user', JSON.stringify(data.user))
      return data.user
    },
    async loadMe() {
      const user: any = await authApi.me()
      this.user = user
      localStorage.setItem('wo_user', JSON.stringify(user))
      return user
    },
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 令牌可能已失效，忽略
      }
      this.token = ''
      this.user = null
      localStorage.removeItem('wo_token')
      localStorage.removeItem('wo_user')
    }
  }
})

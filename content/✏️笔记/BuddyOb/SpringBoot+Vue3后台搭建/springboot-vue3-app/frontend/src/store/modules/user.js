import { defineStore } from 'pinia'
import {
  login as loginApi,
  getInfo as getInfoApi,
  getRouters as getRoutersApi,
  logout as logoutApi
} from '@/api/login'
import { setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    userInfo: {},
    roles: [],
    permissions: [],
    routers: [],
    menus: []
  }),
  actions: {
    login(data) {
      return new Promise((resolve, reject) => {
        loginApi(data)
          .then(res => {
            setToken(res.data.token)
            this.token = res.data.token
            resolve()
          })
          .catch(e => reject(e))
      })
    },
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfoApi()
          .then(res => {
            this.userInfo = res.data.user
            this.roles = res.data.roles
            this.permissions = res.data.permissions
            resolve(res.data)
          })
          .catch(e => reject(e))
      })
    },
    getRouters() {
      return new Promise((resolve, reject) => {
        getRoutersApi()
          .then(res => {
            this.routers = res.data
            resolve(res.data)
          })
          .catch(e => reject(e))
      })
    },
    setMenus(menus) {
      this.menus = menus
    },
    logout() {
      return new Promise(resolve => {
        logoutApi().then(() => {}).catch(() => {})
        this.token = ''
        this.roles = []
        this.permissions = []
        this.routers = []
        this.menus = []
        this.userInfo = {}
        removeToken()
        resolve()
      })
    }
  }
})

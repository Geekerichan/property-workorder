// Axios 封装：统一鉴权头与错误处理
import axios from 'axios'
import { showToast } from 'vant'

const TOKEN_KEY = 'wo_h5_token'

const request = axios.create({ baseURL: '/api', timeout: 30000 })

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('wo_h5_role')
  localStorage.removeItem('wo_h5_name')
}

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') return response
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      showToast(body.message || '操作失败')
      if (body.code === 20000) {
        clearToken()
        const role = localStorage.getItem('wo_h5_role')
        location.href = role === 'OWNER' ? '/owner/login' : '/worker/login'
      }
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    showToast(error?.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export function get<T = any>(url: string, params?: any): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}

export function post<T = any>(url: string, data?: any, params?: any): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}

export function upload(url: string, file: File, params: any): Promise<any> {
  const form = new FormData()
  form.append('file', file)
  return request.post(url, form, { params }) as unknown as Promise<any>
}

/** 带 token 的图片预览地址 */
export function fileUrl(path?: string | null) {
  if (!path) return ''
  return `/api/file/view?path=${encodeURIComponent(path)}&token=${encodeURIComponent(getToken())}`
}

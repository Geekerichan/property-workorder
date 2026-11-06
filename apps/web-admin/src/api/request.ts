// Axios 封装：统一鉴权头、统一错误处理与文件下载
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('wo_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '操作失败')
      if (body.code === 20000) {
        localStorage.removeItem('wo_token')
        if (!location.hash.includes('/login')) {
          location.hash = '#/login'
        }
      }
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    const message = error?.response?.data?.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

export function get<T = any>(url: string, params?: any): Promise<T> {
  return request.get(url, { params }) as unknown as Promise<T>
}

export function post<T = any>(url: string, data?: any, params?: any): Promise<T> {
  return request.post(url, data, { params }) as unknown as Promise<T>
}

export function del<T = any>(url: string, params?: any): Promise<T> {
  return request.delete(url, { params }) as unknown as Promise<T>
}

/** 下载文件：优先读取 Content-Disposition 文件名，否则按 Content-Type 推断扩展名 */
export async function download(url: string, params?: any, method: 'get' | 'post' = 'get') {
  const response: any = await request.request({
    url,
    method,
    params,
    responseType: 'blob'
  })
  const disposition = response.headers['content-disposition'] || ''
  const contentType = response.headers['content-type'] || ''
  const starMatch = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition)
  let fileName: string
  if (starMatch) {
    fileName = decodeURIComponent(starMatch[1])
  } else if (plainMatch) {
    fileName = plainMatch[1]
  } else {
    fileName = defaultFileName(url, contentType)
  }
  const blob = new Blob([response.data])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
  return fileName
}

/** 响应头未携带文件名时，根据 Content-Type 与 URL 推断，避免 PDF 被错误命名为 xlsx */
function defaultFileName(url: string, contentType: string): string {
  if (contentType.includes('pdf')) return 'download.pdf'
  if (contentType.includes('zip')) return 'download.zip'
  if (contentType.includes('spreadsheet') || contentType.includes('excel') || contentType.includes('officedocument')) {
    return 'export.xlsx'
  }
  const segments = url.split('?')[0].split('/')
  const last = segments[segments.length - 1]
  return last || 'download'
}

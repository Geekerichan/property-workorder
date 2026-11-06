/** 文件预览地址（图片 / PDF 无法携带请求头，通过 token 参数鉴权） */
export function fileUrl(path?: string | null) {
  if (!path) return ''
  const token = localStorage.getItem('wo_token') || ''
  return `/api/file/view?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
}

/** 金额格式化 */
export function money(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return '—'
  return `¥ ${Number(value).toFixed(2)}`
}

/** 时间只取到分钟 */
export function shortTime(value?: string | null) {
  if (!value) return '—'
  return value.replace('T', ' ').substring(0, 16)
}

/** 日期 */
export function dateOnly(value?: string | null) {
  if (!value) return '—'
  return value.substring(0, 10)
}

export const facilityOptions = [
  { value: 'ELEVATOR', label: '电梯' },
  { value: 'WATER', label: '给排水' },
  { value: 'POWER', label: '供配电' },
  { value: 'HVAC', label: '暖通空调' },
  { value: 'SECURITY', label: '安防监控' },
  { value: 'GARDEN', label: '园林绿化' }
]

export const orderTypeOptions = [
  { value: 'PLAN_MAINTAIN', label: '计划性维保' },
  { value: 'DEMAND_MAINTAIN', label: '按需维保' },
  { value: 'FAULT_REPAIR', label: '故障报修' },
  { value: 'EMERGENCY', label: '应急处置' },
  { value: 'INSPECTION', label: '日常巡检' }
]

export const statusOptions = [
  { value: 'PENDING_DISPATCH', label: '待派单' },
  { value: 'DISPATCHED', label: '已派单' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'PENDING_REVIEW', label: '待回访' },
  { value: 'ARCHIVED', label: '归档' },
  { value: 'CANCELED', label: '已作废' }
]

export const urgencyOptions = [
  { value: 'LOW', label: '低' },
  { value: 'NORMAL', label: '一般' },
  { value: 'HIGH', label: '紧急' },
  { value: 'URGENT', label: '特急' }
]

import { get, post, upload } from './request'

export const workerApi = {
  login: (data: any) => post('/app/auth/login', data),
  me: () => get('/app/auth/me'),
  logout: () => post('/app/auth/logout'),
  hall: (facilityType?: string) => get('/app/workorder/hall', { facilityType }),
  myTasks: () => get('/app/workorder/my-tasks'),
  detail: (id: number) => get(`/app/workorder/${id}`),
  claim: (id: number) => post(`/app/workorder/${id}/claim`),
  start: (id: number) => post(`/app/workorder/${id}/start`),
  submit: (id: number, results: any[]) => post(`/app/workorder/${id}/submit`, { results }),
  checkin: (params: any) => post('/app/evidence/checkin', null, params),
  tasks: () => get('/app/inspection/my-tasks'),
  taskDetail: (id: number) => get(`/app/inspection/task/${id}`),
  submitPoint: (data: any) => post('/app/inspection/point/submit', data),
  completeTask: (id: number) => post(`/app/inspection/task/${id}/complete`),
  toOrder: (data: any) => post('/app/inspection/to-order', data),
  inspectionPhoto: (file: File, params: any) => upload('/app/inspection/photo', file, params),
  history: () => get('/app/inspection/history'),
  performance: () => get('/app/workorder/performance'),
  messages: (params: any) => get('/app/message/page', params),
  unread: () => get('/app/message/unread'),
  read: (id: number) => post(`/app/message/read/${id}`),
  contractByOrder: (orderId: number) => get('/app/contract/by-order', { orderId }),
  confirmContract: (id: number, party: string) => post(`/app/contract/${id}/confirm`, null, { party }),
  checklist: (orderId: number) => get('/app/checklist', { orderId }),
  saveChecklist: (orderId: number, results: any[]) => post('/app/checklist/save', { orderId, results }),
  uploadPhoto: (file: File, params: any) => upload('/app/evidence/photo', file, params)
}

export const ownerApi = {
  smsCode: (phone: string) => post('/h5/auth/sms-code', { phone }),
  login: (data: any) => post('/h5/auth/login', data),
  me: () => get('/h5/owner/me'),
  logout: () => post('/h5/owner/logout'),
  equip: (code: string, sign: string) => get(`/h5/equip/${encodeURIComponent(code)}`, { sign }),
  equipments: (communityId?: number) => get('/h5/public/equipments', { communityId }),
  report: (data: any) => post('/h5/owner/report', data),
  reports: (params: any) => get('/h5/owner/reports', params),
  reportDetail: (id: number) => get(`/h5/owner/reports/${id}`),
  evaluate: (id: number, data: any) => post(`/h5/owner/reports/${id}/evaluate`, data),
  complaint: (data: any) => post('/h5/owner/complaint', data),
  notices: (params: any) => get('/h5/notice/page', params),
  noticeDetail: (id: number) => get(`/h5/notice/${id}`),
  uploadPhoto: (file: File, params: any) => upload('/h5/owner/report/photo', file, params)
}

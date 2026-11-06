import request, { get, post, del, download } from './request'

/** 认证 */
export const authApi = {
  login: (data: any) => post('/admin/auth/login', data),
  me: () => get('/admin/auth/me'),
  logout: () => post('/admin/auth/logout'),
  changePassword: (data: any) => post('/admin/auth/password', data)
}

/** 看板 */
export const workbenchApi = {
  dashboard: () => get('/admin/workbench/dashboard')
}

/** 工单 */
export const workOrderApi = {
  page: (params: any) => get('/admin/workorder/page', params),
  detail: (id: number) => get(`/admin/workorder/${id}`),
  create: (data: any) => post('/admin/workorder/create', data),
  dispatch: (id: number, data: any) => post(`/admin/workorder/${id}/dispatch`, data),
  batchDispatch: (data: any) => post('/admin/workorder/batch-dispatch', data),
  transfer: (id: number, data: any) => post(`/admin/workorder/${id}/transfer`, data),
  urge: (id: number) => post(`/admin/workorder/${id}/urge`),
  accept: (id: number, data: any) => post(`/admin/workorder/${id}/accept`, data),
  archive: (id: number) => post(`/admin/workorder/${id}/archive`),
  review: (id: number, data: any) => post(`/admin/workorder/${id}/review`, data),
  returnToDispatch: (id: number, data: any) => post(`/admin/workorder/${id}/return`, data),
  cancel: (id: number, data: any) => post(`/admin/workorder/${id}/cancel`, data),
  publish: (id: number, data: any) => post(`/admin/workorder/${id}/publish`, data),
  dispatchDesk: () => get('/admin/workorder/dispatch-desk'),
  settlements: (params: any) => get('/admin/workorder/settlements', params)
}

/** 设备台账 */
export const equipmentApi = {
  page: (params: any) => get('/admin/equipment/page', params),
  detail: (id: number) => get(`/admin/equipment/${id}`),
  save: (data: any) => post('/admin/equipment/save', data),
  remove: (id: number) => del(`/admin/equipment/${id}`),
  qrInfo: (id: number) => get(`/admin/equipment/${id}/qr-info`),
  recordPrint: (id: number) => post(`/admin/equipment/${id}/print`),
  annualCheck: (data: any) => post('/admin/equipment/annual-check', data),
  annualList: (id: number) => get('/admin/equipment/annual-check/list', { equipmentId: id }),
  changeStatus: (id: number, status: string, reason?: string) =>
    post(`/admin/equipment/${id}/status`, null, { status, reason }),
  options: (facilityType?: string) => get('/admin/equipment/options', { facilityType }),
  exportExcel: (params: any) => download('/admin/equipment/export', params)
}

/** 维保计划 */
export const planApi = {
  list: (params?: any) => get('/admin/plan/list', params),
  save: (data: any) => post('/admin/plan/save', data),
  remove: (id: number) => del(`/admin/plan/${id}`),
  generate: (id: number) => post(`/admin/plan/${id}/generate`),
  logs: (planId?: number) => get('/admin/plan/generate-logs', { planId }),
  seasonalList: () => get('/admin/plan/seasonal/list'),
  saveSeasonal: (data: any) => post('/admin/plan/seasonal/save', data),
  deleteSeasonal: (id: number) => del(`/admin/plan/seasonal/${id}`),
  overdue: () => get('/admin/plan/overdue')
}

/** 巡检 */
export const inspectionApi = {
  routes: (facilityType?: string) => get('/admin/inspection/route/list', { facilityType }),
  routeDetail: (id: number) => get(`/admin/inspection/route/${id}`),
  saveRoute: (data: any) => post('/admin/inspection/route/save', data),
  deleteRoute: (id: number) => del(`/admin/inspection/route/${id}`),
  generateTask: (routeId: number) => post('/admin/inspection/task/generate', null, { routeId }),
  taskPage: (params: any) => get('/admin/inspection/task/page', params),
  stats: (days = 30) => get('/admin/inspection/stats', { days }),
  exportExcel: (params: any) => download('/admin/inspection/export', params)
}

/** 合同 */
export const contractApi = {
  page: (params: any) => get('/admin/contract/page', params),
  detail: (id: number) => get(`/admin/contract/${id}`),
  chain: (equipmentId: number) => get('/admin/contract/chain', { equipmentId }),
  confirm: (id: number, party: string) => post(`/admin/contract/${id}/confirm`, null, { party }),
  generatePdf: (id: number) => post(`/admin/contract/${id}/pdf`),
  pdf: (id: number) => download(`/admin/contract/${id}/pdf`)
}

/** 稽核与信用 */
export const auditApi = {
  pending: (params: any) => get('/admin/audit/pending', params),
  acceptance: (params: any) => get('/admin/audit/acceptance/page', params),
  credit: (params: any) => get('/admin/audit/credit/page', params),
  creditSummary: (targetType?: string) => get('/admin/audit/credit/summary', { targetType })
}

/** 基础档案 */
export const baseDataApi = {
  tree: () => get('/admin/basedata/tree'),
  communities: () => get('/admin/basedata/community/list'),
  buildings: (communityId?: number) => get('/admin/basedata/building/list', { communityId }),
  units: (buildingId?: number) => get('/admin/basedata/unit/list', { buildingId }),
  saveCommunity: (data: any) => post('/admin/basedata/community/save', data),
  saveBuilding: (data: any) => post('/admin/basedata/building/save', data),
  saveUnit: (data: any) => post('/admin/basedata/unit/save', data),
  deleteCommunity: (id: number) => del(`/admin/basedata/community/${id}`),
  deleteBuilding: (id: number) => del(`/admin/basedata/building/${id}`),
  deleteUnit: (id: number) => del(`/admin/basedata/unit/${id}`),
  housePage: (params: any) => get('/admin/basedata/house/page', params),
  houseOwners: (houseId: number) => get('/admin/basedata/house/owners', { houseId }),
  saveHouse: (data: any) => post('/admin/basedata/house/save', data),
  deleteHouse: (id: number) => del(`/admin/basedata/house/${id}`),
  saveOwner: (data: any) => post('/admin/basedata/house/owner/save', data),
  bindOwner: (data: any) => post('/admin/basedata/house/bind-owner', data),
  unbindOwner: (relationId: number) => post('/admin/basedata/house/unbind-owner', null, { relationId }),
  lookup: (roomNo: string) => get('/admin/basedata/house/lookup', { roomNo }),
  houseTemplate: () => download('/admin/basedata/house/template'),
  houseExport: () => download('/admin/basedata/house/export'),
  houseImport: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/admin/basedata/house/import', form) as any
  },
  teams: () => get('/admin/basedata/team/list'),
  saveTeam: (data: any) => post('/admin/basedata/team/save', data),
  deleteTeam: (id: number) => del(`/admin/basedata/team/${id}`),
  personnelPage: (params: any) => get('/admin/basedata/personnel/page', params),
  availablePersonnel: (teamId?: number) => get('/admin/basedata/personnel/available', { teamId }),
  savePersonnel: (data: any) => post('/admin/basedata/personnel/save', data),
  dutyStatus: (id: number, dutyStatus: string) =>
    post('/admin/basedata/personnel/duty-status', null, { id, dutyStatus }),
  deletePersonnel: (id: number) => del(`/admin/basedata/personnel/${id}`),
  certExpiring: () => get('/admin/basedata/personnel/cert-expiring'),
  companies: (keyword?: string) => get('/admin/company/list', { keyword }),
  saveCompany: (data: any) => post('/admin/company/save', data),
  deleteCompany: (id: number) => del(`/admin/company/${id}`),
  qualifications: (companyId: number) => get('/admin/company/qualification/list', { companyId }),
  saveQualification: (data: any) => post('/admin/company/qualification/save', data),
  deleteQualification: (id: number) => del(`/admin/company/qualification/${id}`),
  expiringQualifications: () => get('/admin/company/qualification/expiring')
}

/** 系统管理 */
export const systemApi = {
  userPage: (params: any) => get('/admin/system/user/page', params),
  usersByRole: (roleCode: string) => get('/admin/system/user/list', { roleCode }),
  saveUser: (data: any) => post('/admin/system/user/save', data),
  resetPassword: (id: number, password: string) =>
    post('/admin/system/user/reset-password', null, { id, password }),
  deleteUser: (id: number) => del(`/admin/system/user/${id}`),
  dictList: () => get('/admin/system/dict/list'),
  dictItems: (dictType: string) => get('/admin/system/dict/items', { dictType }),
  allItems: () => get('/admin/system/dict/all-items'),
  saveDict: (data: any) => post('/admin/system/dict/save', data),
  saveDictItem: (data: any) => post('/admin/system/dict/item/save', data),
  deleteDictItem: (id: number) => del(`/admin/system/dict/item/${id}`),
  configList: () => get('/admin/system/config/list'),
  saveConfig: (data: any) => post('/admin/system/config/save', data),
  loginLogs: (params: any) => get('/admin/system/log/login', params),
  operLogs: (params: any) => get('/admin/system/log/oper', params),
  messagePage: (params: any) => get('/admin/system/message/page', params),
  unread: () => get('/admin/system/message/unread'),
  read: (id: number) => post(`/admin/system/message/read/${id}`),
  readAll: () => post('/admin/system/message/read-all'),
  templates: (facilityType?: string) => get('/admin/checklist/template/list', { facilityType }),
  templateItems: (templateId: number) => get('/admin/checklist/template/items', { templateId }),
  saveTemplate: (data: any) => post('/admin/checklist/template/save', data)
}

/** 导出中心 */
export const exportApi = {
  regulatory: () => download('/admin/export/regulatory'),
  responsibility: (equipmentId: number) => post('/admin/export/responsibility', null, { equipmentId }),
  logs: (params: any) => get('/admin/export/logs', params)
}

// 路由：静态路由表，附登录态校验与角色守卫
import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', code: 'M00' }
      },
      {
        path: 'workorder',
        name: 'workorder',
        component: () => import('@/views/workorder/WorkOrderList.vue'),
        meta: { title: '工单管理', code: 'M01' }
      },
      {
        path: 'workorder/create',
        name: 'workorder-create',
        component: () => import('@/views/workorder/WorkOrderCreate.vue'),
        meta: { title: '创建工单', code: 'M01' }
      },
      {
        path: 'workorder/:id',
        name: 'workorder-detail',
        component: () => import('@/views/workorder/WorkOrderDetail.vue'),
        meta: { title: '工单详情', code: 'M01' }
      },
      {
        path: 'dispatch',
        name: 'dispatch',
        component: () => import('@/views/workorder/DispatchDesk.vue'),
        meta: { title: '派单工作台', code: 'M01' }
      },
      {
        path: 'equipment',
        name: 'equipment',
        component: () => import('@/views/equipment/EquipmentList.vue'),
        meta: { title: '设备台账', code: 'M02' }
      },
      {
        path: 'equipment/:id',
        name: 'equipment-detail',
        component: () => import('@/views/equipment/EquipmentDetail.vue'),
        meta: { title: '设备档案', code: 'M02' }
      },
      {
        path: 'plan',
        name: 'plan',
        component: () => import('@/views/plan/PlanView.vue'),
        meta: { title: '维保计划', code: 'M03' }
      },
      {
        path: 'inspection',
        name: 'inspection',
        component: () => import('@/views/inspection/InspectionView.vue'),
        meta: { title: '巡检管理', code: 'M03' }
      },
      {
        path: 'contract',
        name: 'contract',
        component: () => import('@/views/contract/ContractView.vue'),
        meta: { title: '电子合同', code: 'M04', roles: ['ADMIN', 'COMPANY'] }
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/audit/AuditView.vue'),
        meta: { title: '质量稽核', code: 'M04' }
      },
      {
        path: 'basedata',
        name: 'basedata',
        component: () => import('@/views/basedata/BaseDataView.vue'),
        meta: { title: '基础档案', code: 'M05' }
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/system/SystemView.vue'),
        meta: { title: '系统管理', code: 'M06' }
      },
      {
        path: 'export',
        name: 'export',
        component: () => import('@/views/export/ExportView.vue'),
        meta: { title: '导出中心', code: 'M06' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || '工作台'} · 物业运维工单平台`
  if (to.path === '/login') {
    return true
  }
  if (!auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await auth.loadMe()
    } catch (e) {
      await auth.logout()
      return { path: '/login' }
    }
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && roles.length && auth.role !== 'ADMIN' && !roles.includes(auth.role)) {
    return { path: '/dashboard' }
  }
  return true
})

export default router

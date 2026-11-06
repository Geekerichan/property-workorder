// 路由：作业端 / 业主端双端静态路由
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/request'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => {
      const role = localStorage.getItem('wo_h5_role')
      if (role === 'OWNER') return '/owner/reports'
      if (role === 'WORKER' || role === 'COMPANY') return '/worker/hall'
      return '/owner/login'
    }
  },
  { path: '/worker/login', component: () => import('@/views/worker/LoginView.vue'), meta: { title: '作业端登录' } },
  { path: '/worker/hall', component: () => import('@/views/worker/HallView.vue'), meta: { title: '工单大厅', auth: 'WORKER' } },
  { path: '/worker/tasks', component: () => import('@/views/worker/TasksView.vue'), meta: { title: '我的任务', auth: 'WORKER' } },
  { path: '/worker/order/:id', component: () => import('@/views/worker/OrderDetailView.vue'), meta: { title: '工单执行', auth: 'WORKER' } },
  { path: '/worker/inspection', component: () => import('@/views/worker/InspectionListView.vue'), meta: { title: '巡检任务', auth: 'WORKER' } },
  { path: '/worker/inspection/:id', component: () => import('@/views/worker/InspectionDetailView.vue'), meta: { title: '巡检执行', auth: 'WORKER' } },
  { path: '/worker/performance', component: () => import('@/views/worker/PerformanceView.vue'), meta: { title: '我的业绩', auth: 'WORKER' } },
  { path: '/worker/messages', component: () => import('@/views/worker/MessagesView.vue'), meta: { title: '消息', auth: 'WORKER' } },

  { path: '/owner/login', component: () => import('@/views/owner/LoginView.vue'), meta: { title: '业主登录' } },
  { path: '/owner/reports', component: () => import('@/views/owner/MyReportsView.vue'), meta: { title: '我的上报', auth: 'OWNER' } },
  { path: '/owner/reports/:id', component: () => import('@/views/owner/ReportDetailView.vue'), meta: { title: '进度详情', auth: 'OWNER' } },
  { path: '/owner/report', component: () => import('@/views/owner/ReportView.vue'), meta: { title: '我要报修', auth: 'OWNER' } },
  { path: '/owner/notice', component: () => import('@/views/owner/NoticeView.vue'), meta: { title: '公示栏', auth: 'OWNER' } },
  { path: '/h5/equip/:code', component: () => import('@/views/owner/EquipPublicView.vue'), meta: { title: '设备公开信息' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = `${to.meta.title || '移动端'} · 物业运维`
  const needAuth = to.meta.auth as string | undefined
  if (!needAuth) return true
  if (!getToken()) {
    return needAuth === 'OWNER' ? '/owner/login' : '/worker/login'
  }
  const role = localStorage.getItem('wo_h5_role')
  if (needAuth === 'OWNER' && role !== 'OWNER') return '/owner/login'
  if (needAuth === 'WORKER' && !['WORKER', 'COMPANY'].includes(role || '')) return '/worker/login'
  return true
})

export default router

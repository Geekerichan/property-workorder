<!-- 工程档案式布局：216px 侧栏 + 48px 信息条 + 1280 内容区 -->
<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <div class="brand">
        <div class="brand-mark">工务</div>
        <div class="brand-text">
          <div class="brand-title">物业运维工单</div>
          <div class="brand-sub mono">PROPERTY / WO v1.0</div>
        </div>
      </div>
      <nav class="nav">
        <template v-for="group in visibleNav" :key="group.label">
          <div class="nav-group">{{ group.label }}</div>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="nav-item"
            :class="{ active: isActive(item.path) }">
            <span class="nav-code mono">{{ item.code }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </router-link>
        </template>
      </nav>
      <div class="aside-foot mono">试运行</div>
    </aside>

    <section class="admin-main">
      <header class="admin-header">
        <div class="crumb">
          <span class="mono crumb-code">{{ route.meta.code || 'M00' }}</span>
          <span class="crumb-sep">/</span>
          <span>{{ route.meta.title }}</span>
        </div>
        <div class="spacer" />
        <button class="bell" @click="openMessages">
          <Bell :size="17" />
          <span v-if="unread > 0" class="badge mono">{{ unread > 99 ? '99+' : unread }}</span>
        </button>
        <div class="account">
          <span class="account-name">{{ auth.user?.realName }}</span>
          <span class="tag-box role-tag">{{ roleLabel }}</span>
          <el-dropdown trigger="click">
            <span class="account-more">···</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="passwordVisible = true">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </section>

    <el-dialog v-model="passwordVisible" title="修改密码" width="380px">
      <el-form label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="messageVisible" title="站内信" size="420px">
      <div class="row" style="margin-bottom: 12px">
        <span class="muted">未读 {{ unread }} 条</span>
        <div class="spacer" />
        <el-button link type="primary" @click="readAll">全部已读</el-button>
      </div>
      <div v-if="messages.length === 0" class="empty-line">暂无消息</div>
      <div v-for="msg in messages" :key="msg.id" class="msg-item" :class="{ unread: msg.readFlag === 0 }"
        @click="openMessage(msg)">
        <div class="row">
          <span class="msg-title">{{ msg.title }}</span>
          <div class="spacer" />
          <span class="timeline-time">{{ msg.createTime }}</span>
        </div>
        <div class="msg-content">{{ msg.content }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { systemApi } from '@/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const unread = ref(0)
const messages = ref<any[]>([])
const messageVisible = ref(false)
const passwordVisible = ref(false)
const pwd = ref({ oldPassword: '', newPassword: '' })

const navGroups = [
  {
    label: '总览',
    items: [{ code: 'M00', label: '工作台', path: '/dashboard' }]
  },
  {
    label: '工单业务',
    items: [
      { code: 'M01', label: '工单管理', path: '/workorder' },
      { code: 'M01', label: '派单工作台', path: '/dispatch' }
    ]
  },
  {
    label: '设施与计划',
    items: [
      { code: 'M02', label: '设备台账', path: '/equipment' },
      { code: 'M03', label: '维保计划', path: '/plan' },
      { code: 'M03', label: '巡检管理', path: '/inspection' }
    ]
  },
  {
    label: '合同与质量',
    items: [
      { code: 'M04', label: '电子合同', path: '/contract' },
      { code: 'M04', label: '质量稽核', path: '/audit' }
    ]
  },
  {
    label: '档案与系统',
    items: [
      { code: 'M05', label: '基础档案', path: '/basedata' },
      { code: 'M06', label: '系统管理', path: '/system' },
      { code: 'M06', label: '导出中心', path: '/export' }
    ]
  }
]

const visibleNav = computed(() => {
  if (auth.role === 'ADMIN') return navGroups
  // 维保公司：仅开放工单查询与电子合同，工作台/台账等管理功能由管理员使用
  return navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => ['/workorder', '/contract'].includes(item.path))
    }))
    .filter((group) => group.items.length > 0)
})

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    COMPANY: '维保公司',
    WORKER: '作业人员',
    OWNER: '业主'
  }
  return map[auth.role] || auth.role
})

function isActive(path: string) {
  if (path === '/workorder') {
    return route.path === '/workorder' || route.path.startsWith('/workorder/')
  }
  if (path === '/equipment') {
    return route.path === '/equipment' || route.path.startsWith('/equipment/')
  }
  return route.path === path
}

async function refreshUnread() {
  try {
    unread.value = (await systemApi.unread()) as unknown as number
  } catch (e) {
    unread.value = 0
  }
}

async function openMessages() {
  messageVisible.value = true
  const page: any = await systemApi.messagePage({ pageNum: 1, pageSize: 30 })
  messages.value = page.list || []
}

async function readAll() {
  await systemApi.readAll()
  await refreshUnread()
  messages.value = messages.value.map((m) => ({ ...m, readFlag: 1 }))
}

async function openMessage(msg: any) {
  if (msg.readFlag === 0) {
    await systemApi.read(msg.id)
    msg.readFlag = 1
    await refreshUnread()
  }
  if (msg.bizType === 'WORK_ORDER' && msg.bizId) {
    messageVisible.value = false
    router.push(`/workorder/${msg.bizId}`)
  }
}

async function changePassword() {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  await (await import('@/api')).authApi.changePassword(pwd.value)
  ElMessage.success('密码已修改，请重新登录')
  passwordVisible.value = false
  await auth.logout()
  router.push('/login')
}

async function logout() {
  await auth.logout()
  router.push('/login')
}

onMounted(refreshUnread)
</script>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
}

.admin-aside {
  width: 216px;
  flex: 0 0 216px;
  background: #fbfaf6;
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 18px;
  border-bottom: 1px solid var(--line);
}

.brand-mark {
  width: 34px;
  height: 34px;
  background: var(--brand);
  color: #fff;
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 1px;
}

.brand-title {
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.3;
}

.brand-sub {
  font-size: 9px;
  color: var(--ink-3);
  letter-spacing: 0.6px;
  margin-top: 2px;
}

.nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.nav-group {
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 1px;
  padding: 12px 16px 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  color: var(--ink-2);
  text-decoration: none;
  font-size: 13px;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: var(--row-hover);
  color: var(--ink);
}

.nav-item.active {
  border-left-color: var(--brand);
  background: #f4efe6;
  color: var(--ink);
  font-weight: 600;
}

.nav-code {
  font-size: 10px;
  color: var(--ink-3);
}

.nav-item.active .nav-code {
  color: var(--brand);
}

.aside-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--line);
  font-size: 10px;
  color: var(--ink-3);
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: 48px;
  flex: 0 0 48px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 20;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.crumb-code {
  font-size: 10px;
  color: var(--brand);
  border: 1px solid #e8c9b8;
  padding: 1px 5px;
}

.crumb-sep {
  color: var(--ink-3);
}

.bell {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ink-2);
  padding: 4px;
}

.badge {
  position: absolute;
  top: -2px;
  right: -6px;
  background: var(--brand);
  color: #fff;
  font-size: 9px;
  padding: 0 4px;
  line-height: 14px;
  min-width: 14px;
  text-align: center;
}

.account {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.account-name {
  font-weight: 500;
}

.role-tag {
  background: rgba(217, 72, 15, 0.08);
  color: var(--brand);
  border-color: rgba(217, 72, 15, 0.2);
}

.account-more {
  cursor: pointer;
  color: var(--ink-3);
  letter-spacing: 2px;
}

.admin-content {
  flex: 1;
}

.msg-item {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-left: 3px solid var(--line-strong);
  margin-bottom: 8px;
  background: #fff;
  cursor: pointer;
}

.msg-item.unread {
  border-left-color: var(--brand);
  background: #fdf9f5;
}

.msg-title {
  font-weight: 600;
  font-size: 13px;
}

.msg-content {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.6;
}
</style>

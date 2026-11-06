<template>
  <div class="login-shell">
    <div class="login-aside">
      <div class="aside-top mono">M00 / ACCESS</div>
      <div class="aside-title">
        <div class="t1">物业运维</div>
        <div class="t1">工单管理平台</div>
        <div class="t2">PROPERTY WORK ORDER / 2026</div>
      </div>
      <div class="aside-notes">
        <div class="note"><span class="mono idx">01</span> 计划触发 · 派单抢单 · 全流程留痕</div>
        <div class="note"><span class="mono idx">02</span> 电子合同 · 责任周期 · 责任链追溯</div>
        <div class="note"><span class="mono idx">03</span> 验收稽核 · 业主监督 · 监管报送</div>
      </div>
      <div class="aside-foot mono">内部试运行环境</div>
    </div>

    <div class="login-main">
      <div class="login-box">
        <div class="box-head">
          <h1>账号登录</h1>
          <p class="muted">管理端仅供物业与维保公司使用，业主与作业人员请使用移动端。</p>
        </div>
        <el-form :model="form" label-position="top" @submit.prevent>
          <el-form-item label="登录账号">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码"
              autocomplete="current-password" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%; margin-top: 8px" :loading="loading"
            @click="submit">
            登录
          </el-button>
        </el-form>
        <div class="demo-tip">
          <div class="tip-title">演示账号（管理端）</div>
          <div class="tip-row mono">admin / admin123　管理员</div>
          <div class="tip-row mono">company / cp123　维保公司管理员</div>
          <div class="tip-row mono">owner / ow123　业主（PC 演示）</div>
          <div class="tip-row muted" style="margin-top: 4px">作业人员请使用移动 H5 端（worker / wk123 等），详见首页演示账号。</div>
        </div>
      </div>
      <div class="login-foot mono">物业公司运维工单管理平台 v1.0.0</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = ref({ username: '', password: '' })

async function submit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const user: any = await auth.login(form.value.username, form.value.password)
    ElMessage.success(`欢迎，${user.realName}`)
    const redirect = (route.query.redirect as string) || (user.role === 'COMPANY' ? '/contract' : '/dashboard')
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
}

.login-aside {
  width: 46%;
  max-width: 620px;
  background: #f1eee7;
  border-right: 1px solid var(--line);
  padding: 40px 44px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}

.login-aside::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(to right, rgba(201, 196, 184, 0.35) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(201, 196, 184, 0.35) 1px, transparent 1px);
  background-size: 44px 44px;
  pointer-events: none;
}

.aside-top {
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 2px;
  position: relative;
}

.aside-title {
  position: relative;
}

.t1 {
  font-family: 'Noto Serif SC', serif;
  font-size: 40px;
  font-weight: 600;
  line-height: 1.28;
  letter-spacing: 3px;
}

.t2 {
  margin-top: 14px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--ink-2);
  letter-spacing: 2px;
}

.aside-notes {
  position: relative;
  border-top: 1px solid var(--line-strong);
  padding-top: 18px;
}

.note {
  font-size: 12px;
  color: var(--ink-2);
  padding: 6px 0;
}

.idx {
  color: var(--brand);
  margin-right: 10px;
  font-size: 10px;
}

.aside-foot {
  font-size: 10px;
  color: var(--ink-3);
  position: relative;
}

.login-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.login-box {
  width: 360px;
}

.box-head h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.box-head p {
  font-size: 12px;
  line-height: 1.7;
  margin: 0 0 26px;
}

.demo-tip {
  margin-top: 26px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}

.tip-title {
  font-size: 11px;
  color: var(--ink-3);
  margin-bottom: 6px;
}

.tip-row {
  font-size: 11px;
  color: var(--ink-2);
  line-height: 1.9;
}

.login-foot {
  margin-top: 48px;
  font-size: 10px;
  color: var(--ink-3);
}

@media (max-width: 900px) {
  .login-aside {
    display: none;
  }
}
</style>

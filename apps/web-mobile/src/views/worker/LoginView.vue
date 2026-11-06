<template>
  <div class="login-m">
    <div class="login-head">
      <div class="mono code">M07 / FIELD</div>
      <h1 class="login-title">作业端</h1>
      <p class="login-sub">维保、维修与巡检人员登录<br />管理员与业主请使用对应入口</p>
    </div>
    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="账号" placeholder="请输入账号" required />
        <van-field v-model="form.password" type="password" name="password" label="密码" placeholder="请输入密码" required />
      </van-cell-group>
      <div class="login-actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        <van-button plain block class="link-btn" @click="router.push('/owner/login')">我是业主，进入业主端</van-button>
      </div>
    </van-form>
    <div class="login-foot mono">演示账号：worker / wk123　worker2 / wk123</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { workerApi } from '@/api'
import { setToken } from '@/api/request'

const router = useRouter()
const loading = ref(false)
const form = ref({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    const data: any = await workerApi.login(form.value)
    setToken(data.token)
    localStorage.setItem('wo_h5_role', data.user.role)
    localStorage.setItem('wo_h5_name', data.user.realName)
    showToast('登录成功')
    router.replace('/worker/hall')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-m {
  min-height: 100vh;
  padding: 56px 20px 40px;
}

.login-head {
  margin-bottom: 28px;
}

.code {
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 2px;
}

.login-title {
  font-size: 34px;
  letter-spacing: 4px;
  margin: 12px 0 10px;
}

.login-sub {
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.8;
}

.login-actions {
  padding: 22px 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.link-btn {
  border-color: var(--line);
  color: var(--ink-2);
}

.login-foot {
  text-align: center;
  font-size: 10px;
  color: var(--ink-3);
  margin-top: 32px;
}
</style>

<template>
  <div class="login-m">
    <div class="login-head">
      <div class="mono code">OWNER / 免安装</div>
      <h1 class="login-title">业主服务</h1>
      <p class="login-sub">扫码查设备、随手报修、进度可查<br />手机号验证码登录，绑定房屋后自动带出房号</p>
    </div>

    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.phone" label="手机号" type="tel" maxlength="11" placeholder="请输入业主手机号" required />
        <van-field v-model="form.code" label="验证码" placeholder="请输入 6 位验证码" required>
          <template #button>
            <van-button size="small" type="primary" plain :disabled="counting > 0" @click="sendCode">
              {{ counting > 0 ? counting + 's' : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
        <van-field v-model="form.roomNo" label="房号" placeholder="如 1-1-101（可选，用于校验）" />
      </van-cell-group>
      <div class="login-actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        <van-button plain block class="link-btn" @click="router.push('/worker/login')">我是作业人员</van-button>
      </div>
    </van-form>
    <div class="login-foot">
      <span class="mono">开发环境验证码固定 123456</span><br />
      演示手机号 13800000001 ~ 13800000008
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ownerApi } from '@/api'
import { setToken } from '@/api/request'

const router = useRouter()
const loading = ref(false)
const counting = ref(0)
const form = ref({ phone: '', code: '', roomNo: '' })

async function sendCode() {
  if (!/^1\d{10}$/.test(form.value.phone)) {
    showToast('请输入 11 位手机号')
    return
  }
  await ownerApi.smsCode(form.value.phone)
  showToast('验证码已发送（固定 123456）')
  counting.value = 60
  const timer = setInterval(() => {
    counting.value -= 1
    if (counting.value <= 0) clearInterval(timer)
  }, 1000)
}

async function submit() {
  loading.value = true
  try {
    const data: any = await ownerApi.login(form.value)
    setToken(data.token)
    localStorage.setItem('wo_h5_role', 'OWNER')
    localStorage.setItem('wo_h5_name', data.name)
    showToast(`欢迎，${data.name}`)
    router.replace('/owner/reports')
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
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 30px;
  line-height: 1.9;
}
</style>

<template>
  <div class="page-m">
    <div class="m-header">
      <h1 class="m-title">我的业绩</h1>
      <div class="m-sub mono">{{ name }} · 接单与结算</div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="m-label">接单量</div>
        <div class="stat-value mono">{{ stats.total || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="m-label">完成率</div>
        <div class="stat-value mono">{{ finishRate }}%</div>
      </div>
      <div class="stat-card">
        <div class="m-label">已完成</div>
        <div class="stat-value mono">{{ stats.finished || 0 }}</div>
      </div>
    </div>

    <div class="m-card" v-if="latestCredit">
      <div class="section-inline">最近信用记录</div>
      <div class="m-row">
        <span :style="{ color: latestCredit.creditType === 'DEDUCT' ? '#b23a2f' : '#3f7a56' }">
          {{ latestCredit.creditType === 'DEDUCT' ? '扣分' : '评价' }}
        </span>
        <span class="mono">{{ latestCredit.score }} 分</span>
        <div class="m-spacer" />
        <span class="mono muted" style="font-size: 11px">{{ latestCredit.createTime }}</span>
      </div>
      <div class="muted" style="font-size: 12px; margin-top: 6px">{{ latestCredit.reason }}</div>
    </div>

    <div class="m-card">
      <div class="section-inline">结算记录</div>
      <div v-if="!settlements.length" class="empty-m">暂无结算记录</div>
      <div v-for="s in settlements" :key="s.id" class="settle-row">
        <div>
          <div class="mono" style="font-size: 11px">{{ s.orderNo }}</div>
          <div style="font-size: 13px">{{ s.title }}</div>
        </div>
        <div class="m-spacer" />
        <div style="text-align: right">
          <div class="mono amount">¥ {{ s.amount }}</div>
          <div class="tag-m" :style="s.settleStatus === 'SETTLED'
            ? 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)'
            : 'color:#c98a12;border-color:rgba(201,138,18,.3);background:rgba(201,138,18,.08)'">
            {{ s.settleStatus === 'SETTLED' ? '已结算' : '待结算' }}
          </div>
        </div>
      </div>
    </div>

    <div class="logout-row">
      <van-button plain block @click="logout">退出登录</van-button>
    </div>

    <WorkerTabbar />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkerTabbar from '@/components/WorkerTabbar.vue'
import { workerApi } from '@/api'
import { clearToken } from '@/api/request'

const router = useRouter()
const stats = ref<any>({})
const settlements = ref<any[]>([])
const latestCredit = ref<any>(null)
const finishRate = ref(0)
const name = localStorage.getItem('wo_h5_name') || ''

async function logout() {
  clearToken()
  router.replace('/worker/login')
}

onMounted(async () => {
  const data: any = await workerApi.performance()
  stats.value = data.stats || {}
  settlements.value = data.settlements || []
  latestCredit.value = data.latestCredit
  finishRate.value = data.finishRate || 0
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 12px 0;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--line);
  padding: 12px 10px;
}

.stat-value {
  font-size: 22px;
  margin-top: 6px;
}

.section-inline {
  font-family: 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.settle-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}

.amount {
  font-size: 14px;
  color: var(--brand);
}

.logout-row {
  padding: 18px 12px 0;
}
</style>

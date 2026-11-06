<template>
  <div class="page-m">
    <div class="m-header">
      <div class="m-row">
        <div>
          <h1 class="m-title">工单大厅</h1>
          <div class="m-sub mono">{{ name }} · 开放抢单池</div>
        </div>
        <div class="m-spacer" />
        <van-icon name="bell" size="20" :badge="unread || ''" @click="router.push('/worker/messages')" />
      </div>
    </div>

    <van-dropdown-menu>
      <van-dropdown-item v-model="facilityType" :options="facilityOptions" @change="load" />
    </van-dropdown-menu>

    <div v-if="list.length === 0" class="empty-m">暂无待抢工单</div>
    <div v-for="item in list" :key="item.id" class="m-card order-card">
      <div class="m-row">
        <span class="mono order-no">{{ item.orderNo }}</span>
        <div class="m-spacer" />
        <span class="tag-m" :style="urgencyStyle(item.urgency)">{{ item.urgencyLabel }}</span>
      </div>
      <div class="order-title">{{ item.title }}</div>
      <div class="order-meta">
        <span>{{ item.facilityLabel }}</span>
        <span>{{ item.equipmentName || item.communityName }}</span>
        <span class="mono">时限 {{ shortTime(item.deadline) }}</span>
      </div>
      <div class="m-row order-foot">
        <span class="mono reward">{{ item.rewardAmount ? '报酬 ¥' + item.rewardAmount : '报酬面议' }}</span>
        <div class="m-spacer" />
        <van-button size="small" type="primary" :loading="claiming === item.id" @click="claim(item)">抢单</van-button>
      </div>
    </div>

    <WorkerTabbar />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import WorkerTabbar from '@/components/WorkerTabbar.vue'
import { workerApi } from '@/api'

const router = useRouter()
const list = ref<any[]>([])
const unread = ref(0)
const claiming = ref(0)
const facilityType = ref('')
const name = localStorage.getItem('wo_h5_name') || ''
const facilityOptions = [
  { text: '全部设施', value: '' },
  { text: '电梯', value: 'ELEVATOR' },
  { text: '给排水', value: 'WATER' },
  { text: '供配电', value: 'POWER' },
  { text: '暖通空调', value: 'HVAC' },
  { text: '安防监控', value: 'SECURITY' },
  { text: '园林绿化', value: 'GARDEN' }
]

async function load() {
  list.value = (await workerApi.hall(facilityType.value || undefined)) as unknown as any[]
  unread.value = (await workerApi.unread()) as unknown as number
}

async function claim(item: any) {
  claiming.value = item.id
  try {
    await workerApi.claim(item.id)
    showToast('抢单成功')
    router.push(`/worker/order/${item.id}`)
  } catch (e) {
    load()
  } finally {
    claiming.value = 0
  }
}

function shortTime(value: string) {
  return value ? value.substring(5, 16) : '—'
}

function urgencyStyle(urgency: string) {
  const color = urgency === 'URGENT' ? '#b23a2f' : urgency === 'HIGH' ? '#c98a12' : '#3b6ea5'
  return `color:${color};border-color:${color}44;background:${color}12`
}

onMounted(load)
</script>

<style scoped>
.order-no {
  font-size: 11px;
  color: var(--brand);
}

.order-title {
  margin: 8px 0 6px;
  font-size: 15px;
  line-height: 1.5;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 11px;
  color: var(--ink-3);
}

.order-foot {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.reward {
  font-size: 13px;
  color: var(--brand);
}
</style>

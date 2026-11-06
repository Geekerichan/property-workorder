<template>
  <div class="page-m">
    <div class="m-header">
      <h1 class="m-title">我的任务</h1>
      <div class="m-sub">按状态分组 · 点击进入现场作业</div>
    </div>

    <van-tabs v-model:active="active" sticky offset-top="72">
      <van-tab v-for="group in groups" :key="group.key" :title="`${group.label} ${group.items.length}`">
        <div v-if="group.items.length === 0" class="empty-m">该状态下暂无工单</div>
        <div v-for="item in group.items" :key="item.id" class="m-card" @click="router.push(`/worker/order/${item.id}`)">
          <div class="m-row">
            <span class="mono order-no">{{ item.orderNo }}</span>
            <div class="m-spacer" />
            <span class="mono time">{{ shortTime(item.deadline) }}</span>
          </div>
          <div class="order-title">{{ item.title }}</div>
          <div class="order-meta">
            <span>{{ item.facilityLabel }}</span>
            <span>{{ item.equipmentName || item.communityName }}</span>
            <span v-if="item.overdue" style="color: #b23a2f">已超时</span>
          </div>
        </div>
      </van-tab>
    </van-tabs>

    <WorkerTabbar />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkerTabbar from '@/components/WorkerTabbar.vue'
import { workerApi } from '@/api'

const router = useRouter()
const active = ref(0)
const data = ref<any>({})

const groups = computed(() => [
  { key: 'DISPATCHED', label: '待开始', items: data.value.DISPATCHED || [] },
  { key: 'PROCESSING', label: '处理中', items: data.value.PROCESSING || [] },
  { key: 'PENDING_REVIEW', label: '待验收', items: data.value.PENDING_REVIEW || [] },
  { key: 'ARCHIVED', label: '已归档', items: data.value.ARCHIVED || [] }
])

function shortTime(value: string) {
  return value ? value.substring(5, 16) : ''
}

onMounted(async () => {
  data.value = await workerApi.myTasks()
})
</script>

<style scoped>
.order-no {
  font-size: 11px;
  color: var(--brand);
}

.time {
  font-size: 11px;
  color: var(--ink-3);
}

.order-title {
  margin: 8px 0 6px;
  font-size: 15px;
}

.order-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--ink-3);
}
</style>

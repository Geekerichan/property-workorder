<template>
  <div class="page-m">
    <div class="m-header">
      <h1 class="m-title">巡检任务</h1>
      <div class="m-sub">按频次自动生成 · 逐点打卡 · 异常一键转单</div>
    </div>

    <div v-if="tasks.length === 0" class="empty-m">今日暂无巡检任务</div>
    <div v-for="task in tasks" :key="task.id" class="m-card" @click="router.push(`/worker/inspection/${task.id}`)">
      <div class="m-row">
        <span class="mono task-no">{{ task.taskNo }}</span>
        <div class="m-spacer" />
        <span class="tag-m" :style="task.status === 'DONE'
          ? 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)'
          : 'color:#3b6ea5;border-color:rgba(59,110,165,.3);background:rgba(59,110,165,.08)'">
          {{ task.status === 'DONE' ? '已完成' : task.status === 'DOING' ? '执行中' : '待执行' }}
        </span>
      </div>
      <div class="task-title">{{ task.routeName }}</div>
      <div class="task-meta">
        <span>{{ task.facilityLabel }}</span>
        <span class="mono">{{ task.planDate }}</span>
        <span>{{ task.points?.length || 0 }} 个点位</span>
        <span v-if="task.abnormalCount > 0" style="color: #b23a2f">异常 {{ task.abnormalCount }}</span>
      </div>
    </div>

    <WorkerTabbar />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkerTabbar from '@/components/WorkerTabbar.vue'
import { workerApi } from '@/api'

const router = useRouter()
const tasks = ref<any[]>([])

onMounted(async () => {
  tasks.value = (await workerApi.tasks()) as unknown as any[]
})
</script>

<style scoped>
.task-no {
  font-size: 11px;
  color: var(--brand);
}

.task-title {
  font-size: 15px;
  margin: 8px 0 6px;
}

.task-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--ink-3);
}
</style>

<template>
  <div class="page-m">
    <van-nav-bar title="公示栏" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="notice-head">
      <div class="mono muted" style="font-size: 10px">PUBLIC NOTICE / 分级过滤</div>
      <div class="muted" style="font-size: 11px; margin-top: 6px">
        消防类、监控画面与住户个人信息永久不公开
      </div>
    </div>

    <div v-if="list.length === 0" class="empty-m">暂无公示</div>
    <div v-for="item in list" :key="item.id" class="m-card notice">
      <div class="m-row">
        <span class="tag-m" :style="typeStyle(item.noticeType)">{{ typeLabel(item.noticeType) }}</span>
        <div class="m-spacer" />
        <span class="mono muted" style="font-size: 10px">{{ (item.publishTime || '').substring(0, 16) }}</span>
      </div>
      <div class="notice-title">{{ item.title }}</div>
      <div class="notice-content">{{ item.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ownerApi } from '@/api'

const router = useRouter()
const list = ref<any[]>([])

function typeLabel(type: string) {
  return { REPAIR: '设施维修', WATER: '停水通知', ELECTRIC: '停电通知', GREEN: '绿化施药', OTHER: '其他' }[type] || type
}

function typeStyle(type: string) {
  if (type === 'WATER') return 'color:#3b6ea5;border-color:rgba(59,110,165,.3);background:rgba(59,110,165,.08)'
  if (type === 'GREEN') return 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)'
  if (type === 'ELECTRIC') return 'color:#c98a12;border-color:rgba(201,138,18,.3);background:rgba(201,138,18,.08)'
  return 'color:#b23a2f;border-color:rgba(178,58,47,.22);background:rgba(178,58,47,.08)'
}

onMounted(async () => {
  const page: any = await ownerApi.notices({ pageNum: 1, pageSize: 30 })
  list.value = page.list
})
</script>

<style scoped>
.notice-head {
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--line);
  background: #fbfaf6;
}

.notice-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  margin: 10px 0 8px;
  line-height: 1.5;
}

.notice-content {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.8;
}
</style>

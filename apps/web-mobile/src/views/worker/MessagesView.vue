<template>
  <div class="page-m">
    <div class="m-header">
      <div class="m-row">
        <div>
          <h1 class="m-title">消息</h1>
          <div class="m-sub">新工单、催办、驳回与合同提醒</div>
        </div>
        <div class="m-spacer" />
        <van-button size="small" plain @click="readAll">全部已读</van-button>
      </div>
    </div>

    <div v-if="list.length === 0" class="empty-m">暂无消息</div>
    <div v-for="msg in list" :key="msg.id" class="m-card msg" :class="{ unread: msg.readFlag === 0 }" @click="open(msg)">
      <div class="m-row">
        <span class="msg-title">{{ msg.title }}</span>
        <div class="m-spacer" />
        <span class="mono muted" style="font-size: 10px">{{ msg.createTime }}</span>
      </div>
      <div class="msg-content">{{ msg.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { workerApi } from '@/api'

const router = useRouter()
const list = ref<any[]>([])

async function load() {
  const page: any = await workerApi.messages({ pageNum: 1, pageSize: 50 })
  list.value = page.list
}

async function open(msg: any) {
  if (msg.readFlag === 0) {
    await workerApi.read(msg.id)
    msg.readFlag = 1
  }
  if (msg.bizType === 'WORK_ORDER' && msg.bizId) {
    router.push(`/worker/order/${msg.bizId}`)
  }
}

async function readAll() {
  await workerApi.messages({ pageNum: 1, pageSize: 1 })
  for (const msg of list.value) {
    if (msg.readFlag === 0) {
      await workerApi.read(msg.id)
      msg.readFlag = 1
    }
  }
}

onMounted(load)
</script>

<style scoped>
.msg {
  border-left: 3px solid var(--line-strong);
}

.msg.unread {
  border-left-color: var(--brand);
  background: #fdf9f5;
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
}

.msg-content {
  font-size: 12px;
  color: var(--ink-2);
  margin-top: 6px;
  line-height: 1.6;
}
</style>

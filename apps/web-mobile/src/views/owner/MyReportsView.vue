<template>
  <div class="page-m">
    <div class="m-header">
      <div class="m-row">
        <div>
          <h1 class="m-title">我的上报</h1>
          <div class="m-sub mono">{{ name }} · 进度可查</div>
        </div>
        <div class="m-spacer" />
        <van-icon name="setting-o" size="20" @click="logout" />
      </div>
    </div>

    <div class="quick-row">
      <div class="quick" @click="router.push('/owner/report')">
        <van-icon name="edit" size="18" />
        <span>我要报修</span>
      </div>
      <div class="quick" @click="router.push('/owner/notice')">
        <van-icon name="volume-o" size="18" />
        <span>公示栏</span>
      </div>
    </div>

    <div v-if="list.length === 0" class="empty-m">暂无上报记录</div>
    <div v-for="item in list" :key="item.id" class="m-card" @click="router.push(`/owner/reports/${item.id}`)">
      <div class="m-row">
        <span class="mono order-no">{{ item.orderNo }}</span>
        <div class="m-spacer" />
        <span class="tag-m" :style="statusStyle(item.status)">{{ item.statusLabel }}</span>
      </div>
      <div class="order-title">{{ item.title }}</div>
      <div class="order-meta">
        <span>{{ item.facilityLabel }}</span>
        <span class="mono">{{ (item.createTime || '').substring(5, 16) }}</span>
        <span v-if="item.satisfactionScore">已评价 {{ item.satisfactionScore }} 星</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ownerApi } from '@/api'
import { clearToken } from '@/api/request'

const router = useRouter()
const list = ref<any[]>([])
const name = localStorage.getItem('wo_h5_name') || ''

function statusStyle(status: string) {
  if (status === 'ARCHIVED' || status === 'PENDING_REVIEW') {
    return 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)'
  }
  if (status === 'CANCELED') return 'color:#8a8578;border-color:#e4e0d6;background:#f6f4ef'
  if (status === 'PROCESSING' || status === 'DISPATCHED') {
    return 'color:#3b6ea5;border-color:rgba(59,110,165,.3);background:rgba(59,110,165,.08)'
  }
  return 'color:#c98a12;border-color:rgba(201,138,18,.3);background:rgba(201,138,18,.08)'
}

async function logout() {
  clearToken()
  router.replace('/owner/login')
}

onMounted(async () => {
  const page: any = await ownerApi.reports({ pageNum: 1, pageSize: 30 })
  list.value = page.list
})
</script>

<style scoped>
.quick-row {
  display: flex;
  gap: 10px;
  padding: 12px 12px 0;
}

.quick {
  flex: 1;
  background: #fff;
  border: 1px solid var(--line);
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}

.order-no {
  font-size: 11px;
  color: var(--brand);
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

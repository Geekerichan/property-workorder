<template>
  <div class="public-page">
    <div class="public-head">
      <div class="mono code-line">设备公开信息 / {{ code }}</div>
      <h1 class="equip-name">{{ info.name || '设备信息查询' }}</h1>
      <div class="equip-loc">{{ info.location || '' }}</div>
    </div>

    <div v-if="hidden" class="hidden-card">
      <div class="status-huge warn">不对外公开</div>
      <p class="hidden-tip">{{ info.message }}</p>
    </div>

    <template v-else-if="info.publicVisible">
      <div class="status-card">
        <div class="m-label">运行状态</div>
        <div class="status-huge" :class="statusClass">{{ info.statusText }}</div>
        <div class="status-sub mono">{{ info.facilityLabel }} · 年检有效期 {{ info.nextAnnualDate || '未登记' }}</div>
      </div>

      <div class="info-table">
        <div class="row-line"><span>最近维保</span><span class="mono">{{ info.lastMaintainTime || '暂无记录' }}</span></div>
        <div class="row-line"><span>维保单位</span><span>{{ info.lastMaintainCompany || '—' }}</span></div>
        <div class="row-line"><span>下次维保到期</span><span class="mono">{{ info.nextMaintainDate || '—' }}</span></div>
      </div>

      <div class="records">
        <div class="records-title">近 N 次维保记录</div>
        <div v-if="!info.records?.length" class="empty-m">暂无公开维保记录</div>
        <div v-for="(r, i) in info.records" :key="i" class="record-line">
          <span class="mono">{{ (r.finishTime || '').substring(0, 10) }}</span>
          <span class="company">{{ r.maintainCompany || '内部班组' }}</span>
          <span class="conclusion">{{ r.conclusion }}</span>
        </div>
      </div>

      <div class="public-actions">
        <van-button type="primary" block @click="goReport">我要报修</van-button>
        <van-button plain block @click="goNotice">查看公示</van-button>
      </div>
    </template>

    <div v-else class="hidden-card">
      <div class="status-huge warn">查询失败</div>
      <p class="hidden-tip">{{ info.message || '二维码无效，请联系物业服务中心' }}</p>
    </div>

    <div class="foot-note">
      本页仅展示设备公开维保信息，不含任何人员个人信息与内部数据。<br />
      消防类设备信息不对外公开。
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ownerApi } from '@/api'

const route = useRoute()
const router = useRouter()
const code = String(route.params.code || '')
const sign = String(route.query.sign || '')
const info = ref<any>({})

const hidden = computed(() => info.value.publicVisible === false)
const statusClass = computed(() => {
  const level = info.value.statusLevel
  if (level === 'DANGER' || level === 'OVERDUE') return 'danger'
  if (level === 'WARN') return 'warn'
  return 'normal'
})

function goReport() {
  router.push({ path: '/owner/report', query: { equipmentId: info.value.id } })
}

function goNotice() {
  router.push('/owner/notice')
}

onMounted(async () => {
  try {
    info.value = await ownerApi.equip(code, sign)
  } catch (e) {
    info.value = { publicVisible: false, message: '二维码校验失败，请联系物业服务中心' }
  }
})
</script>

<style scoped>
.public-page {
  min-height: 100vh;
  padding: 28px 18px 40px;
  background: var(--paper);
}

.public-head {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line-strong);
}

.code-line {
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 1.5px;
}

.equip-name {
  font-size: 22px;
  line-height: 1.5;
  margin: 10px 0 6px;
}

.equip-loc {
  font-size: 12px;
  color: var(--ink-2);
}

.status-card {
  margin-top: 18px;
  background: #fff;
  border: 1px solid var(--line);
  padding: 22px 18px;
}

.status-sub {
  margin-top: 10px;
  font-size: 11px;
  color: var(--ink-3);
}

.info-table {
  margin-top: 14px;
  background: #fff;
  border: 1px solid var(--line);
}

.row-line {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 11px 14px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}

.row-line:last-child {
  border-bottom: none;
}

.row-line > span:first-child {
  color: var(--ink-3);
  font-size: 12px;
}

.records {
  margin-top: 14px;
  background: #fff;
  border: 1px solid var(--line);
}

.records-title {
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
  font-family: 'Noto Serif SC', serif;
  font-size: 14px;
}

.record-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}

.record-line .company {
  flex: 1;
  color: var(--ink-2);
}

.conclusion {
  color: var(--done);
}

.public-actions {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hidden-card {
  margin-top: 24px;
  background: #fff;
  border: 1px solid var(--line);
  border-left: 3px solid var(--warn);
  padding: 24px 18px;
}

.hidden-tip {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.7;
}

.foot-note {
  margin-top: 28px;
  font-size: 10px;
  color: var(--ink-3);
  line-height: 1.8;
}
</style>

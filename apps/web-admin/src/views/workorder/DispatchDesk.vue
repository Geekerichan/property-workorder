<template>
  <div class="page">
    <PageHeader title="派单工作台" description="待派池按紧急等级排序，右侧人员状态实时可见" />

    <div class="desk-grid">
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">待派池</span>
          <span class="muted" style="font-size: 12px">{{ pool.length }} 单待派</span>
        </div>
        <div class="panel-body" style="padding: 0">
          <div v-if="pool.length === 0" class="empty-line">暂无待派工单</div>
          <div v-for="row in pool" :key="row.id" class="pool-row"
            :class="{ urgent: row.urgency === 'URGENT' || row.urgency === 'HIGH' }" @click="select(row)">
            <div class="pool-main">
              <div class="row">
                <span class="mono pool-no">{{ row.orderNo }}</span>
                <span class="pool-title">{{ row.title }}</span>
              </div>
              <div class="pool-meta">
                <span>{{ row.facilityLabel }} / {{ row.orderTypeLabel }}</span>
                <span class="mono">{{ shortTime(row.deadline) }}</span>
                <span :style="{ color: row.urgency === 'URGENT' ? '#b23a2f' : row.urgency === 'HIGH' ? '#c98a12' : '#6b675e' }">
                  {{ row.urgencyLabel }}
                </span>
              </div>
            </div>
            <el-button size="small" type="primary" plain @click.stop="openDispatch(row)">派单</el-button>
          </div>
        </div>
      </div>

      <div class="side-col">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">人员状态</span>
            <span class="muted" style="font-size: 11px">在岗 / 请假 / 离职</span>
          </div>
          <div class="panel-body" style="padding: 0; max-height: 380px; overflow: auto">
            <div v-for="p in personnel" :key="p.id" class="person-row">
              <div class="person-dot" :style="{ background: dutyColor(p.dutyStatus) }" />
              <div class="person-main">
                <div class="person-name">{{ p.name }}
                  <span v-if="p.certExpired" class="cert-flag">资质过期</span>
                  <span v-else-if="p.certMissing" class="cert-flag">无资质</span>
                </div>
                <div class="person-sub">{{ p.teamName || '未分组' }} · {{ p.skills || '—' }}</div>
              </div>
              <span class="mono person-duty">{{ dutyLabel(p.dutyStatus) }}</span>
            </div>
          </div>
        </div>

        <div class="panel" style="margin-top: 16px">
          <div class="panel-head"><span class="panel-title">抢单监控</span></div>
          <div class="panel-body" style="padding: 0">
            <div v-if="claims.length === 0" class="empty-line">暂无抢单记录</div>
            <div v-for="c in claims" :key="c.id" class="claim-row">
              <span class="mono timeline-time">{{ shortTime(c.claimTime) }}</span>
              <span>{{ c.personnelName }}</span>
              <span class="tag-box" :style="c.success
                ? 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
                : 'color:#8a8578;background:rgba(138,133,120,.08);border-color:rgba(138,133,120,.2)'">
                {{ c.success ? '抢单成功' : '失败' }}
              </span>
              <span class="muted" style="font-size: 11px">{{ c.failReason || '' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="visible" title="派单" width="440px">
      <div class="dialog-tip">{{ current?.orderNo }} · {{ current?.title }}</div>
      <el-form label-width="80px">
        <el-form-item label="作业人员">
          <el-select v-model="form.personnelId" filterable style="width: 100%">
            <el-option v-for="p in available" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id"
              :disabled="!p.certValid" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { workOrderApi, baseDataApi } from '@/api'
import { shortTime } from '@/utils/format'

const pool = ref<any[]>([])
const personnel = ref<any[]>([])
const claims = ref<any[]>([])
const available = ref<any[]>([])
const visible = ref(false)
const current = ref<any>(null)
const form = ref<any>({ personnelId: undefined, remark: '' })

async function load() {
  const data: any = await workOrderApi.dispatchDesk()
  pool.value = data.pool || []
  personnel.value = data.personnel || []
  claims.value = data.claims || []
}

function select(row: any) {
  current.value = row
}

async function openDispatch(row: any) {
  current.value = row
  available.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
  form.value = { personnelId: undefined, remark: '' }
  visible.value = true
}

async function submit() {
  if (!form.value.personnelId) {
    ElMessage.warning('请选择作业人员')
    return
  }
  try {
    await workOrderApi.dispatch(current.value.id, form.value)
    ElMessage.success('派单成功')
    visible.value = false
    load()
  } catch (e) {
    // 离岗拦截等业务异常由请求层提示
  }
}

function dutyColor(status: string) {
  return status === 'ON_DUTY' ? '#3f7a56' : status === 'LEAVE' ? '#c98a12' : '#8a8578'
}

function dutyLabel(status: string) {
  return { ON_DUTY: '在岗', LEAVE: '请假', RESIGNED: '离职' }[status] || status
}

onMounted(load)
</script>

<style scoped>
.desk-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
  align-items: start;
}

.pool-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-left: 3px solid var(--line-strong);
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}

.pool-row.urgent {
  border-left-color: var(--state-danger);
  background: #fbf7f6;
}

.pool-main {
  flex: 1;
  min-width: 0;
}

.pool-no {
  font-size: 11px;
  color: var(--brand);
}

.pool-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pool-meta {
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 3px;
}

.person-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--line);
}

.person-dot {
  width: 6px;
  height: 6px;
}

.person-main {
  flex: 1;
}

.person-name {
  font-size: 13px;
}

.person-sub {
  font-size: 11px;
  color: var(--ink-3);
}

.person-duty {
  font-size: 11px;
  color: var(--ink-2);
}

.cert-flag {
  font-size: 10px;
  color: var(--state-danger);
  border: 1px solid rgba(178, 58, 47, 0.3);
  padding: 0 4px;
  margin-left: 6px;
}

.claim-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
}

.dialog-tip {
  font-size: 12px;
  background: #f6f4ef;
  padding: 8px 10px;
  margin-bottom: 12px;
}

@media (max-width: 1100px) {
  .desk-grid {
    grid-template-columns: 1fr;
  }
}
</style>

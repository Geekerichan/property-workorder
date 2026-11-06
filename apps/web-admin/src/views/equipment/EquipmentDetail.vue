<template>
  <div class="page" v-loading="loading">
    <PageHeader :title="equipment.name || '设备档案'" :description="`${equipment.code || ''} · ${equipment.location || ''}`">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <el-button @click="router.push('/export')">导出责任记录包</el-button>
        <el-button v-if="equipment.status !== 'LOCKED'" type="danger" plain @click="changeStatus('LOCKED')">锁停设备</el-button>
        <el-button v-else type="primary" @click="changeStatus('NORMAL')">恢复运行</el-button>
        <el-button @click="annualVisible = true">登记年检</el-button>
      </template>
    </PageHeader>

    <div class="top-grid">
      <div class="panel">
        <div class="panel-head"><span class="panel-title">基础信息</span></div>
        <div class="panel-body info-grid">
          <div class="info-item"><span class="info-label">设备类型</span><span class="info-value">{{ equipment.facilityLabel }}</span></div>
          <div class="info-item"><span class="info-label">运行状态</span>
            <span class="info-value"><StatusTag :status="equipment.status" /></span>
          </div>
          <div class="info-item"><span class="info-label">投用日期</span><span class="mono info-value">{{ dateOnly(equipment.commissionDate) }}</span></div>
          <div class="info-item"><span class="info-label">下次年检</span><span class="mono info-value">{{ dateOnly(equipment.nextAnnualDate) }}</span></div>
          <div class="info-item"><span class="info-label">安全管理员</span><span class="info-value">{{ equipment.safetyManager || '—' }} {{ equipment.safetyPhone || '' }}</span></div>
          <div class="info-item"><span class="info-label">维保逾期</span><span class="info-value">{{ equipment.maintainOverdue === 1 ? '是（红牌）' : '否' }}</span></div>
          <div class="info-item"><span class="info-label">坐标</span>
            <span class="mono info-value">{{ equipment.longitude || '—' }}, {{ equipment.latitude || '—' }}</span>
          </div>
          <div class="info-item"><span class="info-label">备注</span><span class="info-value">{{ equipment.remark || '—' }}</span></div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head"><span class="panel-title">设备二维码</span></div>
        <div class="panel-body qr-panel">
          <img :src="`/api/admin/equipment/${id}/qr.png?token=${token}`" class="qr-img" alt="设备二维码" />
          <div class="mono qr-content">{{ equipment.qrContent }}</div>
          <div class="muted" style="font-size: 11px">累计打印 {{ equipment.printCount || 0 }} 次 · 扫码签名防伪</div>
        </div>
      </div>
    </div>

    <div class="panel" style="margin-top: 16px">
      <div class="panel-head"><span class="panel-title">分类型扩展参数</span></div>
      <div class="panel-body">
        <div v-if="!extEntries.length" class="empty-line">暂无扩展参数</div>
        <div v-else class="ext-grid">
          <div v-for="([key, value]) in extEntries" :key="key" class="ext-item">
            <span class="info-label">{{ key }}</span>
            <span class="mono info-value">{{ value }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="panel">
        <div class="panel-head"><span class="panel-title">年检记录</span></div>
        <el-table :data="equipment.annualChecks || []" size="small">
          <el-table-column label="检验日期" width="110">
            <template #default="{ row }"><span class="mono">{{ dateOnly(row.checkDate) }}</span></template>
          </el-table-column>
          <el-table-column label="有效期至" width="110">
            <template #default="{ row }"><span class="mono">{{ dateOnly(row.expireDate) }}</span></template>
          </el-table-column>
          <el-table-column label="结论" width="80">
            <template #default="{ row }">{{ row.result === 'PASS' ? '合格' : '不合格' }}</template>
          </el-table-column>
          <el-table-column prop="agency" label="检验机构" min-width="140" />
          <el-table-column prop="reportNo" label="报告编号" width="140" />
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-head"><span class="panel-title">近期维保履历</span></div>
        <div class="panel-body" style="padding: 0">
          <div v-if="!equipment.recentMaintain?.length" class="empty-line">暂无维保记录</div>
          <div v-for="(item, idx) in equipment.recentMaintain" :key="idx" class="maintain-row">
            <span class="mono maintain-time">{{ item.finishTime }}</span>
            <span class="maintain-company">{{ item.maintainCompany || '内部班组' }}</span>
            <span class="spacer" />
            <span class="tag-box" style="color: #3f7a56; background: rgba(63,122,86,.08); border-color: rgba(63,122,86,.2)">
              {{ item.acceptResult === 'PASS' ? '合格' : '已完成' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel" style="margin-top: 16px">
      <div class="panel-head"><span class="panel-title">状态变更日志</span></div>
      <div class="panel-body" style="padding: 0">
        <div v-for="log in equipment.statusLogs || []" :key="log.id" class="record-row">
          <span class="mono timeline-time">{{ log.createTime }}</span>
          <span>{{ log.fromStatus || '—' }} → {{ log.toStatus }}</span>
          <span class="muted">{{ log.reason }}</span>
          <span class="spacer" />
          <span class="muted" style="font-size: 11px">{{ log.operatorName }}</span>
        </div>
        <div v-if="!equipment.statusLogs?.length" class="empty-line">暂无状态变更</div>
      </div>
    </div>

    <el-dialog v-model="annualVisible" title="登记年检记录" width="460px">
      <el-form label-width="90px">
        <el-form-item label="检验日期">
          <el-date-picker v-model="annualForm.checkDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="annualForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="检验结论">
          <el-radio-group v-model="annualForm.result">
            <el-radio value="PASS">合格</el-radio>
            <el-radio value="FAIL">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="检验机构"><el-input v-model="annualForm.agency" /></el-form-item>
        <el-form-item label="报告编号"><el-input v-model="annualForm.reportNo" /></el-form-item>
      </el-form>
      <div class="muted" style="font-size: 12px">不合格或有效期已过将自动锁停设备，并在业主端显示"禁止使用"。</div>
      <template #footer>
        <el-button @click="annualVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnual">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { equipmentApi } from '@/api'
import { dateOnly } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const loading = ref(false)
const equipment = ref<any>({})
const annualVisible = ref(false)
const annualForm = ref<any>({ result: 'PASS' })
const token = localStorage.getItem('wo_token') || ''

const extEntries = computed(() => {
  if (!equipment.value.extJson) return []
  try {
    return Object.entries(JSON.parse(equipment.value.extJson))
  } catch (e) {
    return []
  }
})

async function load() {
  loading.value = true
  try {
    equipment.value = await equipmentApi.detail(id)
  } finally {
    loading.value = false
  }
}

async function saveAnnual() {
  if (!annualForm.value.checkDate || !annualForm.value.expireDate) {
    ElMessage.warning('请填写检验日期与有效期')
    return
  }
  await equipmentApi.annualCheck({ ...annualForm.value, equipmentId: id })
  ElMessage.success('年检记录已保存')
  annualVisible.value = false
  load()
}

async function changeStatus(status: string) {
  const { value } = await ElMessageBox.prompt(
    status === 'LOCKED' ? '填写锁停原因（将通知管理员并影响业主端显示）' : '填写恢复原因',
    status === 'LOCKED' ? '锁停设备' : '恢复运行',
    { inputPlaceholder: '原因' }
  )
  await equipmentApi.changeStatus(id, status, value)
  ElMessage.success('设备状态已更新')
  load()
}

onMounted(load)
</script>

<style scoped>
.top-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-label {
  font-size: 11px;
  color: var(--ink-3);
}

.info-value {
  font-size: 13px;
}

.qr-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.qr-img {
  width: 180px;
  height: 180px;
  border: 1px solid var(--line);
  padding: 6px;
}

.qr-content {
  font-size: 9px;
  color: var(--ink-3);
  word-break: break-all;
  text-align: center;
}

.ext-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 20px;
}

.ext-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.maintain-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
}

.maintain-time {
  font-size: 11px;
  color: var(--ink-3);
}

.maintain-company {
  color: var(--ink);
}

.record-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .top-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>

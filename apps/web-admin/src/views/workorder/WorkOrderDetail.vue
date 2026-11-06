<template>
  <div class="page" v-loading="loading">
    <PageHeader :title="order.orderNo || '工单详情'" :description="order.title">
      <template #actions>
        <el-button @click="router.back()">返回列表</el-button>
        <template v-if="isAdmin">
          <el-button v-if="canDispatch" type="primary" @click="openDispatch">派单</el-button>
          <el-button v-if="canTransfer" @click="openTransfer">转派</el-button>
          <el-button v-if="canUrge" @click="urge">催办</el-button>
          <el-button v-if="canAccept" type="primary" @click="acceptVisible = true">验收</el-button>
          <el-button v-if="canArchive" type="primary" @click="archive">归档</el-button>
          <el-button v-if="canReview" @click="reviewVisible = true">回访评价</el-button>
          <el-button v-if="canPublish" @click="publishVisible = true">公示</el-button>
        </template>
      </template>
    </PageHeader>

    <div class="status-strip">
      <StatusTag :status="order.status" />
      <span class="tag-box" :style="urgencyStyle">{{ order.urgencyLabel }}</span>
      <span class="mono strip-no">{{ order.orderNo }}</span>
      <span class="muted">{{ order.orderTypeLabel }} / {{ order.facilityLabel }}</span>
      <div class="spacer" />
      <span v-if="order.overdue" class="overdue-flag">已超时</span>
      <span class="muted mono" style="font-size: 11px">创建 {{ order.createTime }}</span>
    </div>

    <div class="detail-grid">
      <div class="detail-main">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">基本信息</span>
            <div v-if="isAdmin" class="row">
              <el-button v-if="canReturn" link @click="returnDispatch">退回重新派单</el-button>
              <el-button v-if="canCancel" link style="color: #b23a2f" @click="cancelOrder">作废</el-button>
            </div>
          </div>
          <div class="panel-body info-grid">
            <div class="info-item">
              <span class="info-label">设备档案</span>
              <span class="info-value">
                <router-link v-if="order.equipmentId" :to="`/equipment/${order.equipmentId}`" class="link">
                  {{ order.equipmentName }}
                </router-link>
                <span v-else>—</span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">安装位置</span>
              <span class="info-value">{{ order.equipment?.location || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属小区 / 楼栋</span>
              <span class="info-value">{{ order.communityName }} {{ order.buildingName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">主责人</span>
              <span class="info-value">{{ order.mainWorkerName || '待派单' }}
                <span v-if="order.workerPhone" class="mono muted"> {{ order.workerPhone }}</span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">响应时限</span>
              <span class="mono info-value" :style="order.overdue ? 'color:#b23a2f' : ''">{{ order.deadline || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">触发源</span>
              <span class="info-value">{{ order.sourceLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">责任周期</span>
              <span class="mono info-value">{{ shortTime(order.responsibilityStart) }} ~ {{ shortTime(order.responsibilityEnd) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">单次报酬</span>
              <span class="mono info-value">{{ money(order.rewardAmount) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">上报人</span>
              <span class="info-value">{{ order.reporterName || '—' }}
                <span v-if="order.reporterPhone" class="mono muted"> {{ order.reporterPhone }}</span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">满意度</span>
              <span class="info-value">{{ order.satisfactionScore ? order.satisfactionScore + ' 星' : '待评价' }}</span>
            </div>
            <div class="info-item full">
              <span class="info-label">问题描述</span>
              <span class="info-value desc">{{ order.description || '—' }}</span>
            </div>
            <div v-if="order.cancelReason" class="info-item full">
              <span class="info-label">作废原因</span>
              <span class="info-value">{{ order.cancelReason }}</span>
            </div>
          </div>
        </div>

        <div v-if="order.contract" class="panel" style="margin-top: 16px">
          <div class="panel-head">
            <span class="panel-title">电子合同与责任周期</span>
            <el-button link type="primary" @click="router.push('/contract')">前往合同列表</el-button>
          </div>
          <div class="panel-body info-grid">
            <div class="info-item"><span class="info-label">合同编号</span><span class="mono info-value">{{ order.contract.contractNo }}</span></div>
            <div class="info-item"><span class="info-label">服务方</span><span class="info-value">{{ order.contract.partyB }}</span></div>
            <div class="info-item"><span class="info-label">责任周期</span><span class="mono info-value">{{ order.contract.responsibilityDays }} 天</span></div>
            <div class="info-item"><span class="info-label">合同状态</span>
              <span class="info-value">{{ order.contract.status === 'ACTIVE' ? '已生效' : order.contract.status === 'EXPIRED' ? '已到期' : '待双方确认' }}</span>
            </div>
          </div>
        </div>

        <div class="panel" style="margin-top: 16px">
          <div class="panel-head">
            <span class="panel-title">作业清单</span>
            <span class="muted" style="font-size: 12px">{{ mandatoryHint }}</span>
          </div>
          <div class="panel-body">
            <div v-if="!order.checklistGroups || order.checklistGroups.length === 0" class="empty-line">尚未生成作业清单（派单后自动生成）</div>
            <div v-for="group in order.checklistGroups" :key="group.groupName" class="check-group">
              <div class="check-group-title">{{ group.groupName }}</div>
              <div v-for="item in group.items" :key="item.id" class="check-row">
                <span class="check-name">
                  <span v-if="item.requirePhoto === 1" class="req">*</span>{{ item.itemName }}
                </span>
                <span v-if="item.itemValue" class="mono check-value">{{ item.itemValue }}</span>
                <span class="spacer" />
                <span class="tag-box" :style="item.result === 'ABNORMAL'
                  ? 'color:#b23a2f;background:rgba(178,58,47,.08);border-color:rgba(178,58,47,.22)'
                  : item.result === 'NORMAL'
                    ? 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
                    : 'color:#8a8578;background:rgba(138,133,120,.08);border-color:rgba(138,133,120,.2)'">
                  {{ item.result === 'ABNORMAL' ? '异常' : item.result === 'NORMAL' ? '正常' : '未勾选' }}
                </span>
              </div>
            </div>
            <div class="completeness">
              完成度 {{ order.checklistCompleteness?.percent || 0 }}%（{{ order.checklistCompleteness?.done || 0 }}/{{ order.checklistCompleteness?.total || 0 }}），
              强制拍照项 {{ order.checklistCompleteness?.photoDone || 0 }}/{{ order.checklistCompleteness?.photoRequired || 0 }}
            </div>
          </div>
        </div>

        <div class="panel" style="margin-top: 16px">
          <div class="panel-head">
            <span class="panel-title">留痕照片</span>
            <span class="muted" style="font-size: 12px">服务端水印 · 含 MD5 校验码</span>
          </div>
          <div class="panel-body">
            <div v-if="!order.photos || order.photos.length === 0" class="empty-line">暂无留痕照片</div>
            <div v-else class="photo-grid">
              <div v-for="photo in order.photos" :key="photo.id" class="photo-card">
                <el-image :src="fileUrl(photo.watermarkPath)" :preview-src-list="[fileUrl(photo.watermarkPath)]"
                  fit="cover" class="photo-img" preview-teleported />
                <div class="photo-meta">
                  <span>{{ photoTypeLabel(photo.photoType) }}</span>
                  <span class="mono">{{ shortTime(photo.shootTime) }}</span>
                </div>
                <div class="photo-md5 mono">MD5 {{ photo.md5 }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel" style="margin-top: 16px">
          <div class="panel-head"><span class="panel-title">打卡与现场记录</span></div>
          <div class="panel-body">
            <div v-if="order.checkins?.length" class="checkin-line">
              <span v-for="c in order.checkins" :key="c.id" class="mono">
                {{ shortTime(c.checkinTime) }} · {{ c.checkinType === 'SCAN' ? '扫码' : '定位' }}
                · 距设备 {{ c.distance ?? '—' }} 米
              </span>
            </div>
            <div v-if="!order.records || order.records.length === 0" class="empty-line">暂无现场记录</div>
            <div v-for="record in order.records" :key="record.id" class="record-row">
              <span class="mono timeline-time">{{ record.createTime }}</span>
              <span class="record-content">{{ record.content }}</span>
            </div>
          </div>
        </div>

        <div v-if="order.acceptances && order.acceptances.length" class="panel" style="margin-top: 16px">
          <div class="panel-head"><span class="panel-title">验收记录</span></div>
          <div class="panel-body">
            <div v-for="item in order.acceptances" :key="item.id" class="record-row">
              <span class="tag-box" :style="item.result === 'PASS'
                ? 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
                : 'color:#b23a2f;background:rgba(178,58,47,.08);border-color:rgba(178,58,47,.22)'">
                {{ item.result === 'PASS' ? '验收通过' : '驳回重做' }}
              </span>
              <span class="record-content">{{ item.checkDetail }}　{{ item.reason || '' }}</span>
              <span class="mono timeline-time">{{ item.createTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-side">
        <div class="panel">
          <div class="panel-head"><span class="panel-title">留痕时间轴</span></div>
          <div class="panel-body">
            <RecordTimeline :logs="order.logs || []" />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dispatchVisible" title="指定派单" width="440px">
      <el-form label-width="80px">
        <el-form-item label="作业人员">
          <el-select v-model="dispatchForm.personnelId" filterable style="width: 100%">
            <el-option v-for="p in personnel" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id"
              :disabled="!p.certValid" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dispatchForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispatch">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转派工单" width="440px">
      <el-form label-width="80px">
        <el-form-item label="新主责人">
          <el-select v-model="transferForm.personnelId" filterable style="width: 100%">
            <el-option v-for="p in personnel" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id"
              :disabled="!p.certValid" />
          </el-select>
        </el-form-item>
        <el-form-item label="转派原因"><el-input v-model="transferForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转派</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="acceptVisible" title="验收稽核" width="520px">
      <div class="audit-summary">
        作业清单完成度 {{ order.checklistCompleteness?.percent || 0 }}%，
        强制拍照项 {{ order.checklistCompleteness?.photoDone || 0 }}/{{ order.checklistCompleteness?.photoRequired || 0 }}，
        留痕照片 {{ order.photos?.length || 0 }} 张
      </div>
      <el-radio-group v-model="acceptForm.result" style="margin-bottom: 12px">
        <el-radio value="PASS">验收通过</el-radio>
        <el-radio value="REJECT">驳回重做</el-radio>
      </el-radio-group>
      <el-input v-model="acceptForm.reason" type="textarea" :rows="3"
        :placeholder="acceptForm.result === 'REJECT' ? '驳回必须填写原因（写入信用扣分记录）' : '可填写验收意见'" />
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAccept">提交验收</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="回访与满意度" width="460px">
      <el-form label-width="80px">
        <el-form-item label="满意度">
          <el-rate v-model="reviewForm.score" :max="5" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
        </el-form-item>
        <el-form-item label="回访记录">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="业主反馈与回访结论" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">保存回访</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="对外公示" width="480px">
      <el-form label-width="80px">
        <el-form-item label="公示标题"><el-input v-model="publishForm.title" /></el-form-item>
        <el-form-item label="公示内容">
          <el-input v-model="publishForm.content" type="textarea" :rows="4" placeholder="留空则使用工单摘要" />
        </el-form-item>
      </el-form>
      <div class="muted" style="font-size: 12px">消防类工单永久禁止公示，监控画面与住户信息不会公开。</div>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPublish">发布公示</el-button>
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
import RecordTimeline from '@/components/RecordTimeline.vue'
import { workOrderApi, baseDataApi } from '@/api'
import { fileUrl, money, shortTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isAdmin = computed(() => auth.role === 'ADMIN')
const id = Number(route.params.id)
const loading = ref(false)
const order = ref<any>({})
const personnel = ref<any[]>([])
const dispatchVisible = ref(false)
const transferVisible = ref(false)
const transferForm = ref<any>({ personnelId: undefined, reason: '' })
const acceptVisible = ref(false)
const reviewVisible = ref(false)
const publishVisible = ref(false)
const dispatchForm = ref<any>({ personnelId: undefined, remark: '' })
const acceptForm = ref<any>({ result: 'PASS', reason: '' })
const reviewForm = ref<any>({ score: 5, content: '' })
const publishForm = ref<any>({ publish: true, title: '', content: '', noticeType: 'REPAIR' })

const canDispatch = computed(() => order.value.status === 'PENDING_DISPATCH')
const canTransfer = computed(() => ['DISPATCHED', 'PROCESSING'].includes(order.value.status))
const canUrge = computed(() => ['PENDING_DISPATCH', 'DISPATCHED', 'PROCESSING'].includes(order.value.status))
const canAccept = computed(() => order.value.status === 'PENDING_REVIEW' && order.value.acceptResult !== 'PASS')
const canArchive = computed(() => order.value.status === 'PENDING_REVIEW' && order.value.acceptResult === 'PASS')
const canReview = computed(() => ['PENDING_REVIEW', 'ARCHIVED'].includes(order.value.status))
const canPublish = computed(() => ['PENDING_REVIEW', 'ARCHIVED'].includes(order.value.status))
const canReturn = computed(() => ['DISPATCHED', 'PROCESSING'].includes(order.value.status))
const canCancel = computed(() => ['PENDING_DISPATCH', 'DISPATCHED', 'PROCESSING', 'PENDING_REVIEW'].includes(order.value.status))

const urgencyStyle = computed(() => {
  const color = order.value.urgency === 'URGENT' ? '#b23a2f' : order.value.urgency === 'HIGH' ? '#c98a12' : '#3b6ea5'
  return `color:${color};background:rgba(0,0,0,.03);border-color:${color}33`
})

function photoTypeLabel(type: string) {
  return { BEFORE: '作业前', AFTER: '作业后', KEY: '关键操作', OTHER: '其他' }[type] || type
}

async function load() {
  loading.value = true
  try {
    order.value = await workOrderApi.detail(id)
  } finally {
    loading.value = false
  }
}

async function openDispatch() {
  personnel.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
  dispatchForm.value = { personnelId: undefined, remark: '' }
  dispatchVisible.value = true
}

async function submitDispatch() {
  if (!dispatchForm.value.personnelId) {
    ElMessage.warning('请选择作业人员')
    return
  }
  await workOrderApi.dispatch(id, dispatchForm.value)
  ElMessage.success('派单成功，电子合同已自动生成')
  dispatchVisible.value = false
  load()
}

async function openTransfer() {
  personnel.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
  transferForm.value = { personnelId: undefined, reason: '' }
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferForm.value.personnelId) {
    ElMessage.warning('请选择新的主责人')
    return
  }
  await workOrderApi.transfer(id, transferForm.value)
  ElMessage.success('转派完成')
  transferVisible.value = false
  load()
}

async function urge() {
  await workOrderApi.urge(id)
  ElMessage.success('已发送催办')
  load()
}

async function submitAccept() {
  if (acceptForm.value.result === 'REJECT' && !acceptForm.value.reason) {
    ElMessage.warning('驳回必须填写原因')
    return
  }
  await workOrderApi.accept(id, acceptForm.value)
  ElMessage.success(acceptForm.value.result === 'PASS' ? '验收通过' : '已驳回重做')
  acceptVisible.value = false
  load()
}

async function archive() {
  await ElMessageBox.confirm('确认回访完成并归档该工单？归档后结算记录将标记为已结算。', '归档确认')
  await workOrderApi.archive(id)
  ElMessage.success('工单已归档')
  load()
}

async function submitReview() {
  await workOrderApi.review(id, reviewForm.value)
  ElMessage.success('回访记录已保存')
  reviewVisible.value = false
  load()
}

async function submitPublish() {
  await workOrderApi.publish(id, publishForm.value)
  ElMessage.success('公示已发布')
  publishVisible.value = false
  load()
}

async function returnDispatch() {
  const { value } = await ElMessageBox.prompt('退回重新派单须填写原因', '退回重新派单', {
    inputPlaceholder: '如：错报 / 非业务范畴'
  })
  await workOrderApi.returnToDispatch(id, { reason: value })
  ElMessage.success('已退回待派池')
  load()
}

async function cancelOrder() {
  const { value } = await ElMessageBox.prompt('作废须填写原因（如业主不在家）', '作废工单', {
    inputPlaceholder: '作废原因'
  })
  await workOrderApi.cancel(id, { reason: value })
  ElMessage.success('工单已作废')
  load()
}

onMounted(load)
</script>

<style scoped>
.status-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid var(--line);
  margin-bottom: 16px;
  font-size: 12px;
}

.strip-no {
  font-size: 12px;
  color: var(--ink);
}

.overdue-flag {
  color: var(--state-danger);
  border: 1px solid rgba(178, 58, 47, 0.3);
  padding: 1px 6px;
  font-size: 11px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 16px;
  align-items: start;
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

.info-item.full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 11px;
  color: var(--ink-3);
}

.info-value {
  font-size: 13px;
  color: var(--ink);
}

.info-value.desc {
  line-height: 1.7;
  background: #f8f6f1;
  border-left: 2px solid var(--line-strong);
  padding: 8px 10px;
}

.link {
  color: var(--brand);
  text-decoration: none;
  border-bottom: 1px solid rgba(217, 72, 15, 0.3);
}

.check-group {
  margin-bottom: 14px;
}

.check-group-title {
  font-size: 12px;
  color: var(--ink-2);
  background: #f1eee7;
  padding: 5px 8px;
  margin-bottom: 6px;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.req {
  color: var(--brand);
  margin-right: 3px;
}

.check-value {
  font-size: 12px;
  color: var(--ink-2);
}

.completeness {
  margin-top: 10px;
  font-size: 12px;
  color: var(--ink-2);
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 12px;
}

.photo-card {
  border: 1px solid var(--line);
  padding: 8px;
  background: #fbfaf6;
}

.photo-img {
  width: 100%;
  height: 120px;
  display: block;
}

.photo-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--ink-2);
  margin-top: 6px;
}

.photo-md5 {
  font-size: 9px;
  color: var(--ink-3);
  margin-top: 3px;
  word-break: break-all;
}

.checkin-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-2);
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
  margin-bottom: 8px;
}

.record-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}

.record-content {
  flex: 1;
  color: var(--ink);
  line-height: 1.6;
}

.audit-summary {
  font-size: 12px;
  background: #f6f4ef;
  padding: 8px 10px;
  margin-bottom: 12px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

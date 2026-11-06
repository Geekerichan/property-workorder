<template>
  <div class="page-m" v-if="detail.task">
    <van-nav-bar :title="detail.task.taskNo" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="m-card">
      <div class="m-row">
        <span class="route-name">{{ detail.route?.name }}</span>
        <div class="m-spacer" />
        <span class="mono muted" style="font-size: 11px">{{ detail.task.planDate }}</span>
      </div>
      <div class="progress-line">
        已完成 {{ doneCount }} / {{ detail.points.length }} 个点位
        <span v-if="detail.task.abnormalCount" style="color: #b23a2f"> · 异常 {{ detail.task.abnormalCount }}</span>
      </div>
    </div>

    <div class="m-card">
      <div class="section-inline">点位序列</div>
      <div v-for="(p, index) in detail.points" :key="p.point.id" class="point-row">
        <div class="point-index mono">{{ index + 1 }}</div>
        <div class="point-main">
          <div class="point-name">{{ p.point.pointName }}</div>
          <div class="point-sub">{{ p.equipmentName || '未绑定设备' }} · {{ p.items.length }} 项检查</div>
          <div v-if="p.point.remark" class="point-remark">{{ p.point.remark }}</div>
        </div>
        <div class="point-actions">
          <span class="tag-m" :style="p.point.status === 'DONE'
            ? (p.point.result === 'ABNORMAL'
              ? 'color:#b23a2f;border-color:rgba(178,58,47,.3);background:rgba(178,58,47,.08)'
              : 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)')
            : 'color:#8a8578;border-color:#e4e0d6;background:#f6f4ef'">
            {{ p.point.status === 'DONE' ? (p.point.result === 'ABNORMAL' ? '异常' : '正常') : '待检' }}
          </span>
          <van-button v-if="p.point.status !== 'DONE'" size="mini" type="primary" plain
            @click="openPoint(p)">检查</van-button>
          <van-button v-else-if="p.point.result === 'ABNORMAL'" size="mini" plain type="danger"
            @click="toOrder(p)">转工单</van-button>
          <van-button v-else size="mini" plain @click="openPoint(p)">重检</van-button>
        </div>
      </div>
    </div>

    <div class="action-bar">
      <van-button block type="primary" :disabled="doneCount < detail.points.length" @click="complete">结束巡检</van-button>
    </div>

    <van-popup v-model:show="popupVisible" position="bottom" round :style="{ maxHeight: '82%' }">
      <div class="popup-body">
        <div class="popup-title">{{ current?.point?.pointName }}</div>
        <div class="seg-line">
          <span :class="{ on: pointForm.result === 'NORMAL' }" @click="pointForm.result = 'NORMAL'">正常</span>
          <span :class="{ on: pointForm.result === 'ABNORMAL', danger: true }"
            @click="pointForm.result = 'ABNORMAL'">异常</span>
        </div>

        <div v-for="item in current?.items || []" :key="item.id" class="check-line">
          <span class="check-label">{{ item.itemName }}</span>
          <van-field v-if="item.requireValue === 1" v-model="item.value" :placeholder="`读数 ${item.unit || ''}`"
            class="value-field" />
          <span v-else class="muted" style="font-size: 11px">目视检查</span>
        </div>

        <van-field v-model="pointForm.remark" type="textarea" rows="2" placeholder="异常描述 / 备注"
          class="remark-field" />
        <div class="m-row" style="margin: 12px 0">
          <van-uploader :after-read="upload" :max-count="2" capture="camera" :preview-image="false">
            <div class="photo-btn"><van-icon name="photograph" /> 现场拍照</div>
          </van-uploader>
          <span v-if="photoPath" class="muted" style="font-size: 11px">已上传 {{ photoPath ? 1 : 0 }} 张</span>
        </div>

        <van-button type="primary" block :loading="saving" @click="submitPoint">提交点位</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { workerApi } from '@/api'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const detail = ref<any>({ task: null, points: [] })
const popupVisible = ref(false)
const current = ref<any>(null)
const saving = ref(false)
const photoPath = ref('')
const pointForm = ref<any>({ result: 'NORMAL', remark: '' })

const doneCount = computed(() => (detail.value.points || []).filter((p: any) => p.point.status === 'DONE').length)

async function load() {
  detail.value = await workerApi.taskDetail(id)
}

function openPoint(p: any) {
  current.value = p
  pointForm.value = { result: p.point.result || 'NORMAL', remark: p.point.remark || '' }
  photoPath.value = ''
  p.items.forEach((item: any) => {
    if (item.value === undefined) item.value = ''
  })
  popupVisible.value = true
}

async function upload(file: any) {
  const photo: any = await workerApi.inspectionPhoto(file.file, { taskId: id, photoType: 'OTHER' })
  photoPath.value = photo.watermarkPath
  showToast('照片已上传')
}

async function submitPoint() {
  if (pointForm.value.result === 'ABNORMAL' && !pointForm.value.remark) {
    showToast('异常点位请填写描述')
    return
  }
  saving.value = true
  try {
    await workerApi.submitPoint({
      taskPointId: current.value.point.id,
      result: pointForm.value.result,
      remark: pointForm.value.remark,
      items: (current.value.items || []).map((item: any) => ({
        itemId: item.id,
        itemName: item.itemName,
        result: pointForm.value.result,
        value: item.value,
        photoPath: photoPath.value
      }))
    })
    showToast('点位已提交')
    popupVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toOrder(p: any) {
  await showConfirmDialog({
    title: '异常转报修',
    message: `将点位「${p.point.pointName}」转为故障报修工单，自动带出设备、位置、描述与巡检照片。`
  })
  const orderId: any = await workerApi.toOrder({
    taskPointId: p.point.id,
    title: `${p.equipmentName || p.point.pointName} 巡检异常`,
    description: p.point.remark || '巡检发现设备异常',
    urgency: 'HIGH'
  })
  showToast('已生成工单')
  router.push(`/worker/order/${orderId}`)
}

async function complete() {
  await workerApi.completeTask(id)
  showToast('巡检已完成')
  router.replace('/worker/inspection')
}

onMounted(load)
</script>

<style scoped>
.route-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
}

.progress-line {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-2);
}

.point-row {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}

.point-index {
  width: 18px;
  font-size: 11px;
  color: var(--brand);
  padding-top: 2px;
}

.point-main {
  flex: 1;
  min-width: 0;
}

.point-name {
  font-size: 13px;
}

.point-sub {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 2px;
}

.point-remark {
  font-size: 11px;
  color: var(--danger);
  margin-top: 3px;
  line-height: 1.5;
}

.point-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}

.popup-body {
  padding: 18px 16px calc(20px + env(safe-area-inset-bottom));
}

.popup-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 16px;
  margin-bottom: 12px;
}

.seg-line {
  display: flex;
  border: 1px solid var(--line);
  margin-bottom: 12px;
}

.seg-line span {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  color: var(--ink-2);
}

.seg-line span.on {
  background: var(--done);
  color: #fff;
}

.seg-line span.on.danger {
  background: var(--danger);
}

.check-line {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.check-label {
  flex: 0 0 130px;
}

.value-field {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid var(--line);
  font-size: 12px;
}

.remark-field {
  margin-top: 10px;
  border: 1px solid var(--line);
  font-size: 12px;
}

.photo-btn {
  border: 1px dashed var(--line-strong);
  padding: 8px 12px;
  font-size: 12px;
  color: var(--ink-2);
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>

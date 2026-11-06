<template>
  <div class="page-m" v-if="order.id">
    <van-nav-bar :title="order.orderNo" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="m-card">
      <div class="m-row">
        <span class="tag-m" :style="statusStyle">{{ order.statusLabel }}</span>
        <span class="tag-m" :style="urgencyStyle">{{ order.urgencyLabel }}</span>
        <div class="m-spacer" />
        <span class="mono time">限 {{ shortTime(order.deadline) }}</span>
      </div>
      <div class="order-title">{{ order.title }}</div>
      <div class="kv"><span>设备</span><span>{{ order.equipmentName || '—' }}</span></div>
      <div class="kv"><span>位置</span><span>{{ order.equipment?.location || order.communityName }}</span></div>
      <div class="kv"><span>类型</span><span>{{ order.orderTypeLabel }} / {{ order.facilityLabel }}</span></div>
      <div class="kv"><span>业主联系</span><span class="mono">{{ order.reporterName || '—' }} {{ order.reporterPhone || '' }}</span></div>
      <div class="kv"><span>描述</span><span class="desc">{{ order.description || '—' }}</span></div>
    </div>

    <div v-if="order.contract" class="m-card">
      <div class="section-inline">电子合同</div>
      <div class="kv"><span>合同编号</span><span class="mono">{{ order.contract.contractNo }}</span></div>
      <div class="kv"><span>责任周期</span><span class="mono">{{ order.contract.responsibilityDays }} 天 ·
        {{ shortTime(order.contract.respStart) }} 起</span></div>
      <div class="kv"><span>乙方确认</span>
        <span>{{ order.contract.signTimeB ? '已确认' : '待确认' }}</span>
      </div>
      <van-button v-if="!order.contract.signTimeB" size="small" type="primary" plain style="margin-top: 8px"
        @click="confirmContract">确认合同条款</van-button>
    </div>

    <div class="m-card" v-if="order.status === 'DISPATCHED'">
      <div class="section-inline">到场打卡</div>
      <p class="hint">定位打卡需在设备 200 米范围内；浏览器需授权位置权限。</p>
      <van-button type="primary" block :loading="checking" @click="doCheckin">定位打卡并开始作业</van-button>
    </div>

    <div v-if="order.status === 'PROCESSING'" class="m-card">
      <div class="section-inline">
        作业清单
        <span class="mono hint-inline">{{ completeness.done }}/{{ completeness.total }}</span>
      </div>
      <p class="hint">{{ order.mandatoryHint }}</p>
      <div v-for="group in groups" :key="group.groupName" class="check-group">
        <div class="group-name">{{ group.groupName }}</div>
        <div v-for="item in group.items" :key="item.id" class="check-item">
          <div class="m-row">
            <span class="item-name"><span v-if="item.requirePhoto === 1" class="req">*</span>{{ item.itemName }}</span>
            <div class="m-spacer" />
            <div class="seg">
              <span :class="{ on: item.result === 'NORMAL' }" @click="item.result = 'NORMAL'">正常</span>
              <span :class="{ on: item.result === 'ABNORMAL', danger: true }" @click="item.result = 'ABNORMAL'">异常</span>
            </div>
          </div>
          <div class="m-row" style="margin-top: 8px; gap: 10px">
            <van-field v-if="item.requireValue === 1" v-model="item.itemValue" :placeholder="`读数 ${item.unit || ''}`"
              class="value-field" />
            <van-uploader :after-read="(file: any) => uploadItemPhoto(item, file)" :max-count="1"
              :preview-image="false" capture="camera">
              <div class="photo-btn" :class="{ done: !!item.photoIds }">
                <van-icon name="photograph" size="16" />
                <span>{{ item.photoIds ? '已拍照' : '拍照' }}</span>
              </div>
            </van-uploader>
          </div>
        </div>
      </div>
    </div>

    <div v-if="order.status === 'PROCESSING'" class="m-card">
      <div class="section-inline">现场留痕照片</div>
      <p class="hint">照片由服务端加水印（时间、作业人、设备、坐标），并生成 MD5 校验码。</p>
      <div class="m-row" style="gap: 10px">
        <van-uploader :after-read="(file: any) => uploadScene(file, 'BEFORE')" :max-count="1" capture="camera">
          <div class="photo-btn">作业前</div>
        </van-uploader>
        <van-uploader :after-read="(file: any) => uploadScene(file, 'AFTER')" :max-count="1" capture="camera">
          <div class="photo-btn">作业后</div>
        </van-uploader>
        <van-uploader :after-read="(file: any) => uploadScene(file, 'KEY')" :max-count="1" capture="camera">
          <div class="photo-btn">关键操作</div>
        </van-uploader>
      </div>
      <div class="scene-photos">
        <img v-for="p in order.photos || []" :key="p.id" :src="fileUrl(p.watermarkPath)" class="scene-img" />
      </div>
    </div>

    <div class="m-card" v-if="order.records?.length">
      <div class="section-inline">现场记录</div>
      <div v-for="r in order.records" :key="r.id" class="record-line">
        <span class="mono">{{ shortTime(r.createTime) }}</span>
        <span>{{ r.content }}</span>
      </div>
    </div>

    <div class="action-bar" v-if="order.status === 'PROCESSING'">
      <van-button type="primary" block :loading="submitting" @click="submitOrder">完工提交</van-button>
    </div>
    <div class="action-bar" v-else-if="order.status === 'DISPATCHED'">
      <van-button type="primary" block :loading="checking" @click="doCheckin">定位打卡</van-button>
    </div>
    <div class="action-bar" v-else>
      <van-button block plain @click="router.push('/worker/tasks')">返回我的任务</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { workerApi } from '@/api'
import { fileUrl } from '@/api/request'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const order = ref<any>({})
const groups = ref<any[]>([])
const checking = ref(false)
const submitting = ref(false)
const coords = ref<{ longitude?: number; latitude?: number }>({})

const completeness = computed(() => order.value.checklistCompleteness || {})

const statusStyle = computed(() => {
  const color = order.value.status === 'PROCESSING' ? '#3b6ea5'
    : order.value.status === 'PENDING_REVIEW' ? '#3f7a56'
      : order.value.status === 'DISPATCHED' ? '#c98a12' : '#8a8578'
  return `color:${color};border-color:${color}44;background:${color}12`
})

const urgencyStyle = computed(() => {
  const color = order.value.urgency === 'URGENT' ? '#b23a2f' : order.value.urgency === 'HIGH' ? '#c98a12' : '#3b6ea5'
  return `color:${color};border-color:${color}44;background:${color}12`
})

function shortTime(value: string) {
  return value ? value.substring(5, 16) : '—'
}

async function load() {
  order.value = await workerApi.detail(id)
  groups.value = order.value.checklistGroups || []
  if (groups.value.length === 0) {
    const data: any = await workerApi.checklist(id)
    groups.value = data.groups || []
  }
  groups.value.forEach((group: any) => group.items.forEach((item: any) => {
    if (!item.result) item.result = 'NORMAL'
  }))
}

function locate(): Promise<{ longitude?: number; latitude?: number }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({})
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ longitude: pos.coords.longitude, latitude: pos.coords.latitude }),
      () => resolve({}),
      { timeout: 8000 }
    )
  })
}

async function doCheckin() {
  checking.value = true
  try {
    const position = await locate()
    if (!position.longitude) {
      showToast('未获取定位，可在浏览器设置中授权位置权限')
    }
    await workerApi.checkin({
      orderId: id,
      type: position.longitude ? 'LOCATION' : 'SCAN',
      longitude: position.longitude,
      latitude: position.latitude,
      address: '现场'
    })
    showToast('打卡成功，开始计时')
    await load()
  } finally {
    checking.value = false
  }
}

async function uploadItemPhoto(item: any, file: any) {
  const position = coords.value.longitude ? coords.value : await locate()
  const photo: any = await workerApi.uploadPhoto(file.file, {
    orderId: id,
    photoType: 'KEY',
    itemId: item.id,
    longitude: position.longitude,
    latitude: position.latitude
  })
  item.photoIds = String(photo.id)
  showToast('照片已加水印上传')
}

async function uploadScene(file: any, photoType: string) {
  const position = await locate()
  await workerApi.uploadPhoto(file.file, {
    orderId: id,
    photoType,
    longitude: position.longitude,
    latitude: position.latitude
  })
  showToast('照片已上传')
  await load()
}

async function submitOrder() {
  const results = groups.value.flatMap((group: any) =>
    group.items.map((item: any) => ({
      itemId: item.id,
      result: item.result,
      value: item.itemValue,
      remark: item.remark,
      photoIds: item.photoIds
    }))
  )
  submitting.value = true
  try {
    await workerApi.submit(id, results)
    await showDialog({ title: '提交成功', message: '工单已进入待验收，物业将尽快完成验收与回访。' })
    await load()
  } finally {
    submitting.value = false
  }
}

async function confirmContract() {
  await workerApi.confirmContract(order.value.contract.id, 'B')
  showToast('合同条款已确认')
  await load()
}

onMounted(load)
</script>

<style scoped>
.order-title {
  font-size: 16px;
  margin: 10px 0 12px;
  line-height: 1.5;
}

.time {
  font-size: 11px;
  color: var(--ink-3);
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
  font-size: 12px;
  color: var(--ink);
}

.kv > span:first-child {
  color: var(--ink-3);
  flex: 0 0 70px;
}

.kv .desc {
  text-align: right;
  line-height: 1.6;
}

.section-inline {
  font-family: 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
}

.hint {
  font-size: 11px;
  color: var(--ink-3);
  line-height: 1.7;
  margin: 0 0 10px;
}

.hint-inline {
  font-size: 12px;
  color: var(--brand);
}

.check-group {
  margin-bottom: 12px;
}

.group-name {
  font-size: 11px;
  color: var(--ink-2);
  background: #f1eee7;
  padding: 4px 8px;
  margin-bottom: 4px;
}

.check-item {
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}

.item-name {
  font-size: 13px;
}

.req {
  color: var(--brand);
}

.seg {
  display: flex;
  border: 1px solid var(--line);
}

.seg span {
  padding: 3px 10px;
  font-size: 12px;
  color: var(--ink-2);
}

.seg span.on {
  background: var(--done);
  color: #fff;
}

.seg span.on.danger {
  background: var(--danger);
}

.value-field {
  padding: 4px 8px;
  border: 1px solid var(--line);
  font-size: 12px;
  flex: 1;
}

.photo-btn {
  border: 1px dashed var(--line-strong);
  padding: 8px 10px;
  font-size: 11px;
  color: var(--ink-2);
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 38px;
}

.photo-btn.done {
  border-color: var(--done);
  color: var(--done);
}

.scene-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.scene-img {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border: 1px solid var(--line);
}

.record-line {
  display: flex;
  gap: 10px;
  font-size: 12px;
  padding: 5px 0;
  color: var(--ink-2);
}
</style>

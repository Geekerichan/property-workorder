<template>
  <div class="page-m" v-if="detail.id">
    <van-nav-bar title="上报进度" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="m-card">
      <div class="m-row">
        <span class="mono order-no">{{ detail.orderNo }}</span>
        <div class="m-spacer" />
        <span class="tag-m" :style="statusStyle">{{ detail.statusLabel }}</span>
      </div>
      <div class="order-title">{{ detail.title }}</div>
      <div class="desc">{{ detail.description }}</div>
      <div class="kv"><span>设施类型</span><span>{{ detail.facilityLabel }}</span></div>
      <div class="kv"><span>紧急等级</span><span>{{ detail.urgencyLabel }}</span></div>
      <div class="kv"><span>提交时间</span><span class="mono">{{ detail.createTime }}</span></div>
      <div class="kv"><span>完成时间</span><span class="mono">{{ detail.finishTime || '处理中' }}</span></div>
    </div>

    <div class="m-card">
      <div class="section-inline">处理进度</div>
      <div class="timeline-m">
        <div v-for="(log, index) in detail.timeline || []" :key="index" class="tl-node">
          <div class="tl-dot" />
          <div>
            <div class="tl-action">{{ actionLabel(log.action) }}</div>
            <div class="mono tl-time">{{ log.createTime }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="detail.photos?.length" class="m-card">
      <div class="section-inline">现场照片</div>
      <div class="photo-row">
        <img v-for="p in detail.photos" :key="p.id" :src="fileUrl(p.watermarkPath)" class="photo" />
      </div>
    </div>

    <div class="m-card" v-if="canEvaluate">
      <div class="section-inline">满意度评价</div>
      <div class="rate-row">
        <van-rate v-model="score" :size="26" color="#d9480f" void-color="#e4e0d6" />
        <span class="muted" style="font-size: 12px">{{ score }} 星</span>
      </div>
      <van-field v-model="content" type="textarea" rows="2" placeholder="可填写评价与建议" />
      <van-button type="primary" block style="margin-top: 12px" :loading="submitting" @click="evaluate">
        提交评价
      </van-button>
    </div>

    <div class="m-card" v-else-if="detail.satisfactionScore">
      <div class="section-inline">我的评价</div>
      <van-rate :model-value="detail.satisfactionScore" readonly :size="20" color="#d9480f" void-color="#e4e0d6" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ownerApi } from '@/api'
import { fileUrl } from '@/api/request'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const detail = ref<any>({})
const score = ref(5)
const content = ref('')
const submitting = ref(false)

const canEvaluate = computed(() =>
  ['PENDING_REVIEW', 'ARCHIVED'].includes(detail.value.status) && !detail.value.satisfactionScore
)

const statusStyle = computed(() => {
  const status = detail.value.status
  if (status === 'ARCHIVED' || status === 'PENDING_REVIEW') {
    return 'color:#3f7a56;border-color:rgba(63,122,86,.3);background:rgba(63,122,86,.08)'
  }
  if (status === 'CANCELED') return 'color:#8a8578;border-color:#e4e0d6;background:#f6f4ef'
  return 'color:#3b6ea5;border-color:rgba(59,110,165,.3);background:rgba(59,110,165,.08)'
})

const actionMap: Record<string, string> = {
  CREATE: '已提交上报',
  DISPATCH: '物业已派单',
  CLAIM: '维保人员已接单',
  START: '已到场处理',
  SUBMIT: '处置完成',
  ACCEPT: '物业验收通过',
  REJECT: '整改中',
  ARCHIVE: '工单已归档',
  URGE: '物业已催办',
  RETURN: '重新派单',
  CANCEL: '工单已作废',
  REVIEW: '回访记录'
}

function actionLabel(action: string) {
  return actionMap[action] || action
}

async function load() {
  detail.value = await ownerApi.reportDetail(id)
}

async function evaluate() {
  submitting.value = true
  try {
    await ownerApi.evaluate(id, { score: score.value, content: content.value })
    showToast('感谢您的评价')
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-no {
  font-size: 11px;
  color: var(--brand);
}

.order-title {
  font-size: 16px;
  margin: 8px 0 8px;
}

.desc {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.7;
  background: #f8f6f1;
  padding: 8px 10px;
  margin-bottom: 10px;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 12px;
}

.kv > span:first-child {
  color: var(--ink-3);
}

.section-inline {
  font-family: 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 8px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--line);
}

.timeline-m {
  position: relative;
  padding-left: 14px;
}

.timeline-m::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 4px;
  bottom: 4px;
  width: 1px;
  background: var(--line-strong);
}

.tl-node {
  position: relative;
  display: flex;
  gap: 10px;
  padding-bottom: 14px;
}

.tl-dot {
  position: absolute;
  left: -14px;
  top: 5px;
  width: 7px;
  height: 7px;
  background: #fff;
  border: 1px solid var(--ink-2);
}

.tl-action {
  font-size: 13px;
}

.tl-time {
  font-size: 10px;
  color: var(--ink-3);
  margin-top: 2px;
}

.photo-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.photo {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border: 1px solid var(--line);
}

.rate-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
</style>

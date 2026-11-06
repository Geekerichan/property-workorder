<template>
  <div class="page-m">
    <van-nav-bar title="我要报修" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="step-head">
      <div v-for="(step, index) in steps" :key="index" class="step">
        <div class="step-dot mono" :class="{ on: stepIndex >= index }">{{ index + 1 }}</div>
        <div class="step-label" :class="{ on: stepIndex === index }">{{ step }}</div>
      </div>
    </div>

    <!-- 步骤一 -->
    <div class="m-card">
      <div class="section-inline">① 拍照并描述问题</div>
      <van-field v-model="form.description" type="textarea" rows="3" maxlength="300" show-word-limit
        placeholder="请描述具体问题，如：2 单元 6 层水管接口渗漏，墙面已出现水渍" />
      <div class="m-label" style="margin: 12px 0 6px">紧急等级</div>
      <div class="urgency-row">
        <span v-for="u in urgencyOptions" :key="u.value" class="urgency-item"
          :class="{ on: form.urgency === u.value }" @click="form.urgency = u.value">{{ u.label }}</span>
      </div>
      <div class="m-label" style="margin: 12px 0 6px">现场照片（选填，最多 3 张）</div>
      <van-uploader v-model="fileList" :max-count="3" :after-read="() => {}" capture="camera" />
    </div>

    <!-- 步骤二 -->
    <div class="m-card">
      <div class="section-inline">② 选择位置 / 设备</div>
      <van-field v-model="houseName" label="房号" readonly placeholder="按登录业主自动带出" />
      <van-field v-model="equipmentName" label="设备" readonly is-link placeholder="请选择故障设备"
        @click="showEquipment = true" />
      <van-popup v-model:show="showEquipment" position="bottom" round>
        <div class="picker-head">选择设备</div>
        <div class="picker-list">
          <div v-for="e in equipments" :key="e.id" class="picker-item"
            @click="selectEquip(e)">
            <div>{{ e.name }}</div>
            <div class="mono muted" style="font-size: 11px">{{ e.code }} · {{ e.facilityLabel }} · {{ e.location }}</div>
          </div>
          <div v-if="equipments.length === 0" class="empty-m">暂无可选设备</div>
        </div>
      </van-popup>
    </div>

    <!-- 步骤三 -->
    <div class="m-card">
      <div class="section-inline">③ 提交报修</div>
      <div class="summary">
        <div class="sum-line"><span>报修人</span><span>{{ owner.name }} {{ owner.phone }}</span></div>
        <div class="sum-line"><span>房号</span><span class="mono">{{ houseName || '未绑定' }}</span></div>
        <div class="sum-line"><span>设备</span><span>{{ equipmentName || '未选择' }}</span></div>
        <div class="sum-line"><span>紧急等级</span><span>{{ urgencyLabel }}</span></div>
      </div>
      <p class="hint">提交后生成上报单号，可在“我的上报”查看进度；处置完成后可评价。</p>
    </div>

    <div class="action-bar">
      <van-button type="primary" block :loading="submitting" @click="submit">提交报修</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ownerApi } from '@/api'

const route = useRoute()
const router = useRouter()
const steps = ['拍照描述', '选位置设备', '提交']
const stepIndex = ref(2)
const form = ref({ description: '', urgency: 'NORMAL', equipmentId: undefined as number | undefined, houseId: undefined as number | undefined })
const owner = ref<any>({ name: localStorage.getItem('wo_h5_name'), phone: '', houses: [] })
const equipments = ref<any[]>([])
const showEquipment = ref(false)
const equipmentName = ref('')
const fileList = ref<any[]>([])
const submitting = ref(false)

const urgencyOptions = [
  { value: 'LOW', label: '低' },
  { value: 'NORMAL', label: '一般' },
  { value: 'HIGH', label: '紧急' },
  { value: 'URGENT', label: '特急' }
]

const urgencyLabel = computed(() => urgencyOptions.find((u) => u.value === form.value.urgency)?.label || '一般')
const houseName = computed(() => owner.value.houses?.[0]?.fullName || '')

function selectEquip(e: any) {
  form.value.equipmentId = e.id
  equipmentName.value = e.name
  showEquipment.value = false
}

async function submit() {
  if (!form.value.description) {
    showToast('请描述需要处理的问题')
    return
  }
  if (!form.value.equipmentId) {
    showToast('请选择故障设备')
    return
  }
  submitting.value = true
  try {
    form.value.houseId = owner.value.houses?.[0]?.houseId
    const order: any = await ownerApi.report({
      equipmentId: form.value.equipmentId,
      houseId: form.value.houseId,
      description: form.value.description,
      urgency: form.value.urgency
    })
    for (const file of fileList.value) {
      if (file.file) {
        await ownerApi.uploadPhoto(file.file, { orderId: order.id, photoType: 'OTHER' })
      }
    }
    showToast('报修已提交')
    router.replace(`/owner/reports/${order.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  owner.value = await ownerApi.me()
  const communityId = owner.value.houses?.[0]?.communityId
  equipments.value = (await ownerApi.equipments(communityId)) as unknown as any[]
  const preset = Number(route.query.equipmentId)
  if (preset) {
    const match = equipments.value.find((e) => e.id === preset)
    if (match) selectEquip(match)
  }
})
</script>

<style scoped>
.step-head {
  display: flex;
  gap: 6px;
  padding: 14px 16px 4px;
}

.step {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.step-dot {
  width: 18px;
  height: 18px;
  border: 1px solid var(--line-strong);
  color: var(--ink-3);
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-dot.on {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

.step-label {
  font-size: 11px;
  color: var(--ink-3);
}

.step-label.on {
  color: var(--ink);
}

.section-inline {
  font-family: 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 8px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--line);
}

.urgency-row {
  display: flex;
  gap: 8px;
}

.urgency-item {
  flex: 1;
  text-align: center;
  border: 1px solid var(--line);
  padding: 7px 0;
  font-size: 12px;
  color: var(--ink-2);
}

.urgency-item.on {
  border-color: var(--brand);
  color: var(--brand);
  background: #fdf6f1;
}

.picker-head {
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  font-family: 'Noto Serif SC', serif;
}

.picker-list {
  max-height: 55vh;
  overflow: auto;
}

.picker-item {
  padding: 11px 16px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}

.summary {
  border: 1px dashed var(--line);
  padding: 8px 12px;
}

.sum-line {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 6px 0;
  font-size: 13px;
}

.sum-line > span:first-child {
  color: var(--ink-3);
  font-size: 12px;
}

.hint {
  font-size: 11px;
  color: var(--ink-3);
  line-height: 1.7;
  margin: 10px 0 0;
}
</style>

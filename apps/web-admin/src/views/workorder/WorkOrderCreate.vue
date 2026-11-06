<template>
  <div class="page">
    <PageHeader title="创建工单" description="选择设备自动带出档案与设施工艺，房号可带出业主信息">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
      </template>
    </PageHeader>

    <div class="panel" style="max-width: 880px">
      <div class="panel-body">
        <el-form :model="form" label-width="96px">
          <el-form-item label="设备档案">
            <el-select v-model="form.equipmentId" filterable placeholder="选择设备（必选）" style="width: 100%"
              @change="onEquipment">
              <el-option-group v-for="group in equipmentGroups" :key="group.label" :label="group.label">
                <el-option v-for="item in group.items" :key="item.id"
                  :label="`${item.code} · ${item.name}`" :value="item.id" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-if="equipment" label="设备信息">
            <div class="equipment-brief">
              <span class="mono">{{ equipment.code }}</span>
              <span>{{ equipment.location }}</span>
              <span class="muted">{{ equipment.status === 'LOCKED' ? '禁止使用' : equipment.status === 'REPAIR' ? '停用检修' : '正常运行' }}</span>
            </div>
          </el-form-item>
          <el-form-item label="业务类型">
            <el-select v-model="form.orderType" style="width: 100%">
              <el-option v-for="item in orderTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="紧急等级">
            <el-radio-group v-model="form.urgency">
              <el-radio-button v-for="item in urgencyOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="派单方式">
            <el-radio-group v-model="form.dispatchMode" :disabled="form.orderType === 'EMERGENCY'">
              <el-radio value="GRAB">开放抢单</el-radio>
              <el-radio value="ASSIGN">指定派单</el-radio>
            </el-radio-group>
            <span v-if="form.orderType === 'EMERGENCY'" class="muted" style="font-size: 12px">
              应急工单不进抢单池，需直接指派
            </span>
          </el-form-item>
          <el-form-item label="工单标题">
            <el-input v-model="form.title" placeholder="如：1号楼1单元电梯门机异响" maxlength="120" show-word-limit />
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit
              placeholder="描述现场情况、业主反馈与处置要求" />
          </el-form-item>
          <el-form-item label="业主房号">
            <div class="row">
              <el-input v-model="roomNo" placeholder="如 1-1-101，用于带出业主信息" style="width: 260px" />
              <el-button @click="lookupRoom">带出业主</el-button>
            </div>
          </el-form-item>
          <el-form-item v-if="reporter.ownerName" label="上报人">
            <div class="equipment-brief">
              <span>{{ reporter.ownerName }}</span>
              <span class="mono">{{ reporter.ownerPhone }}</span>
              <span class="muted">已与房屋档案匹配</span>
            </div>
          </el-form-item>
          <el-form-item label="响应时限">
            <el-date-picker v-model="form.deadline" type="datetime" placeholder="默认按紧急等级自动计算"
              format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width: 260px" />
          </el-form-item>
          <el-form-item label="单次报酬">
            <el-input-number v-model="form.rewardAmount" :min="0" :step="50" :precision="2" style="width: 200px" />
            <span class="muted" style="font-size: 12px; margin-left: 8px">用于维保结算记录</span>
          </el-form-item>
          <el-form-item label="直接指派">
            <el-select v-model="form.mainWorkerId" clearable placeholder="可留空，先进入待派池" style="width: 100%">
              <el-option v-for="p in personnel" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id"
                :disabled="!p.certValid" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">创建工单</el-button>
            <el-button @click="router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { workOrderApi, equipmentApi, baseDataApi } from '@/api'
import { orderTypeOptions, urgencyOptions, facilityOptions } from '@/utils/format'

const router = useRouter()
const submitting = ref(false)
const equipmentList = ref<any[]>([])
const personnel = ref<any[]>([])
const roomNo = ref('')
const reporter = ref<any>({})
const form = ref<any>({
  equipmentId: undefined,
  orderType: 'FAULT_REPAIR',
  urgency: 'NORMAL',
  dispatchMode: 'ASSIGN',
  title: '',
  description: '',
  deadline: '',
  rewardAmount: undefined,
  mainWorkerId: undefined,
  houseId: undefined
})

const equipment = computed(() => equipmentList.value.find((e) => e.id === form.value.equipmentId))

const equipmentGroups = computed(() => {
  return facilityOptions
    .map((f) => ({
      label: f.label,
      items: equipmentList.value.filter((e) => e.facilityType === f.value)
    }))
    .filter((g) => g.items.length > 0)
})

function onEquipment() {
  if (equipment.value && !form.value.title) {
    form.value.title = `${equipment.value.name} 故障报修`
  }
}

async function lookupRoom() {
  if (!roomNo.value) {
    ElMessage.warning('请输入房号')
    return
  }
  try {
    reporter.value = await baseDataApi.lookup(roomNo.value)
    form.value.houseId = reporter.value.houseId
    ElMessage.success(`已带出业主：${reporter.value.ownerName || '未登记'}`)
  } catch (e) {
    reporter.value = {}
  }
}

async function submit() {
  if (!form.value.equipmentId) {
    ElMessage.warning('请选择设备档案')
    return
  }
  if (!form.value.title) {
    ElMessage.warning('请填写工单标题')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form.value, deadline: form.value.deadline || null }
    const order: any = await workOrderApi.create(payload)
    ElMessage.success('工单已创建：' + order.orderNo)
    router.push(`/workorder/${order.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  equipmentList.value = (await equipmentApi.options()) as unknown as any[]
  personnel.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
})
</script>

<style scoped>
.equipment-brief {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--ink);
  background: #f6f4ef;
  border: 1px dashed var(--line);
  padding: 8px 12px;
  width: 100%;
}
</style>

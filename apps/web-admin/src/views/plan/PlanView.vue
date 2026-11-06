<template>
  <div class="page">
    <PageHeader title="维保计划" description="定期与按需计划自动生成工单，季节节点到期触发保养">
      <template #actions>
        <el-button v-if="tab === 'plan'" type="primary" @click="openForm()">新建计划</el-button>
        <el-button v-else-if="tab === 'seasonal'" type="primary" @click="openSeasonal()">新增季节节点</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="plain-tabs">
      <el-tab-pane label="维保计划" name="plan">
        <div class="panel">
          <el-table :data="plans" v-loading="loading">
            <el-table-column prop="name" label="计划名称" min-width="200" />
            <el-table-column prop="facilityLabel" label="设施" width="80" />
            <el-table-column label="模式" width="80">
              <template #default="{ row }">{{ row.mode === 'PERIODIC' ? '定期' : '按需' }}</template>
            </el-table-column>
            <el-table-column label="周期 / 间隔" width="110">
              <template #default="{ row }">
                <span class="mono">{{ row.mode === 'PERIODIC' ? row.periodDays + ' 天' : row.intervalDays + ' 天' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="advanceDays" label="提前生成" width="90">
              <template #default="{ row }"><span class="mono">{{ row.advanceDays }} 天</span></template>
            </el-table-column>
            <el-table-column label="下次生成" width="120">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ dateOnly(row.nextGenerateDate) }}</span></template>
            </el-table-column>
            <el-table-column label="绑定设备" width="150">
              <template #default="{ row }">
                <span class="muted" style="font-size: 12px">{{ row.devices?.length || 0 }} 台</span>
                <el-tooltip :content="row.devices.map((d: any) => d.equipmentName).join('、')" placement="top">
                  <el-button link type="primary" style="margin-left: 6px">查看</el-button>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="派单方式" width="90">
              <template #default="{ row }">{{ row.dispatchMode === 'GRAB' ? '抢单' : '指派' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="generate(row)">立即生成</el-button>
                <el-button link @click="openForm(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="季节节点" name="seasonal">
        <div class="panel">
          <el-table :data="seasonals">
            <el-table-column prop="name" label="节点名称" min-width="200" />
            <el-table-column prop="facilityType" label="设施" width="100">
              <template #default="{ row }">{{ facilityLabel(row.facilityType) }}</template>
            </el-table-column>
            <el-table-column label="触发日期" width="110">
              <template #default="{ row }"><span class="mono">{{ row.monthNo }} 月 {{ row.dayNo }} 日</span></template>
            </el-table-column>
            <el-table-column prop="description" label="保养内容" min-width="260" show-overflow-tooltip />
            <el-table-column label="启用" width="80">
              <template #default="{ row }">{{ row.enabled === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link @click="openSeasonal(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="removeSeasonal(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="生成记录" name="logs">
        <div class="panel">
          <el-table :data="logs">
            <el-table-column prop="planId" label="计划 ID" width="90" />
            <el-table-column label="周期起始" width="120">
              <template #default="{ row }"><span class="mono">{{ dateOnly(row.periodStart) }}</span></template>
            </el-table-column>
            <el-table-column prop="orderCount" label="生成单数" width="90" />
            <el-table-column label="触发方式" width="110">
              <template #default="{ row }">
                {{ row.triggerType === 'MANUAL' ? '手动' : row.triggerType === 'SEASONAL' ? '季节节点' : '定时' }}
              </template>
            </el-table-column>
            <el-table-column label="生成时间" min-width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.createTime }}</span></template>
            </el-table-column>
            <el-table-column label="工单" width="90">
              <template #default="{ row }">
                <router-link v-if="row.orderId" :to="`/workorder/${row.orderId}`" class="link">查看</router-link>
                <span v-else>—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑计划' : '新建计划'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="计划名称"><el-input v-model="form.name" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="设施类型">
            <el-select v-model="form.facilityType" style="width: 100%">
              <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模式">
            <el-radio-group v-model="form.mode">
              <el-radio value="PERIODIC">定期</el-radio>
              <el-radio value="ON_DEMAND">按需</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item v-if="form.mode === 'PERIODIC'" label="周期天数">
            <el-input-number v-model="form.periodDays" :min="1" :max="365" style="width: 100%" />
          </el-form-item>
          <el-form-item v-else label="间隔天数">
            <el-input-number v-model="form.intervalDays" :min="1" :max="90" style="width: 100%" />
          </el-form-item>
          <el-form-item label="提前生成">
            <el-input-number v-model="form.advanceDays" :min="0" :max="30" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="派单方式">
            <el-select v-model="form.dispatchMode" style="width: 100%">
              <el-option label="开放抢单" value="GRAB" />
              <el-option label="指定派单" value="ASSIGN" />
            </el-select>
          </el-form-item>
          <el-form-item label="清单模板">
            <el-select v-model="form.templateId" clearable style="width: 100%">
              <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="设备范围">
          <el-select v-model="equipmentIds" multiple filterable style="width: 100%"
            placeholder="选择计划覆盖的设备">
            <el-option v-for="e in equipments" :key="e.id" :label="`${e.code} · ${e.name}`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="seasonalVisible" title="季节保养节点" width="520px">
      <el-form :model="seasonalForm" label-width="90px">
        <el-form-item label="节点名称"><el-input v-model="seasonalForm.name" /></el-form-item>
        <el-form-item label="设施类型">
          <el-select v-model="seasonalForm.facilityType" style="width: 100%">
            <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="月份"><el-input-number v-model="seasonalForm.monthNo" :min="1" :max="12" /></el-form-item>
          <el-form-item label="日期"><el-input-number v-model="seasonalForm.dayNo" :min="1" :max="28" /></el-form-item>
        </div>
        <el-form-item label="保养内容"><el-input v-model="seasonalForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="seasonalForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seasonalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSeasonal">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { planApi, equipmentApi, systemApi } from '@/api'
import { dateOnly, facilityOptions } from '@/utils/format'

const tab = ref('plan')
const plans = ref<any[]>([])
const seasonals = ref<any[]>([])
const logs = ref<any[]>([])
const templates = ref<any[]>([])
const equipments = ref<any[]>([])
const loading = ref(false)
const formVisible = ref(false)
const form = ref<any>({ mode: 'PERIODIC', dispatchMode: 'GRAB', advanceDays: 3, periodDays: 15 })
const equipmentIds = ref<number[]>([])
const seasonalVisible = ref(false)
const seasonalForm = ref<any>({ monthNo: 4, dayNo: 15, enabled: 1 })

async function load() {
  loading.value = true
  try {
    plans.value = (await planApi.list()) as unknown as any[]
    seasonals.value = (await planApi.seasonalList()) as unknown as any[]
    logs.value = (await planApi.logs()) as unknown as any[]
  } finally {
    loading.value = false
  }
}

async function openForm(row?: any) {
  if (!templates.value.length) templates.value = (await systemApi.templates()) as unknown as any[]
  if (!equipments.value.length) equipments.value = (await equipmentApi.options()) as unknown as any[]
  if (row) {
    const full = plans.value.find((p) => p.id === row.id) || row
    form.value = { ...full }
    equipmentIds.value = (full.devices || []).map((d: any) => d.equipmentId)
  } else {
    form.value = { mode: 'PERIODIC', dispatchMode: 'GRAB', advanceDays: 3, periodDays: 15, facilityType: 'ELEVATOR' }
    equipmentIds.value = []
  }
  formVisible.value = true
}

async function save() {
  if (!form.value.name || equipmentIds.value.length === 0) {
    ElMessage.warning('请填写计划名称并选择设备')
    return
  }
  await planApi.save({ plan: form.value, equipmentIds: equipmentIds.value })
  ElMessage.success('计划已保存')
  formVisible.value = false
  load()
}

async function generate(row: any) {
  await ElMessageBox.confirm(`立即为计划「${row.name}」生成待派工单？`, '手动触发生成')
  const result: any = await planApi.generate(row.id)
  ElMessage.success(`已生成 ${result.count} 张工单，下次生成日 ${result.nextGenerateDate}`)
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除计划「${row.name}」？`, '删除确认')
  await planApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

function openSeasonal(row?: any) {
  seasonalForm.value = row ? { ...row } : { monthNo: 4, dayNo: 15, enabled: 1 }
  seasonalVisible.value = true
}

async function saveSeasonal() {
  await planApi.saveSeasonal(seasonalForm.value)
  ElMessage.success('季节节点已保存')
  seasonalVisible.value = false
  load()
}

async function removeSeasonal(row: any) {
  await ElMessageBox.confirm(`确认删除季节节点「${row.name}」？`, '删除确认')
  await planApi.deleteSeasonal(row.id)
  ElMessage.success('已删除')
  load()
}

function facilityLabel(value: string) {
  return facilityOptions.find((f) => f.value === value)?.label || value
}

watch(tab, () => load())
onMounted(load)
</script>

<style scoped>
.plain-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.link {
  color: var(--brand);
  text-decoration: none;
}
</style>

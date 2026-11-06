<template>
  <div class="page">
    <PageHeader title="巡检管理" description="路线点位配置、任务生成与异常转单，完成率与隐患可统计">
      <template #actions>
        <el-button @click="exportRecords">导出巡检记录</el-button>
        <el-button type="primary" @click="openRoute()">新建路线</el-button>
      </template>
    </PageHeader>

    <div class="stat-strip">
      <div class="stat"><span class="muted">近 30 天任务</span><span class="mono stat-v">{{ stats.taskCount || 0 }}</span></div>
      <div class="stat"><span class="muted">已完成</span><span class="mono stat-v">{{ stats.doneCount || 0 }}</span></div>
      <div class="stat"><span class="muted">完成率</span><span class="mono stat-v">{{ stats.completionRate || 0 }}%</span></div>
      <div class="stat"><span class="muted">隐患发现</span><span class="mono stat-v" style="color: #b23a2f">{{ stats.abnormalCount || 0 }}</span></div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="巡检路线" name="route">
        <div class="panel">
          <el-table :data="routes" v-loading="loading">
            <el-table-column prop="name" label="路线名称" min-width="200" />
            <el-table-column prop="facilityLabel" label="设施" width="90" />
            <el-table-column label="频次" width="80">
              <template #default="{ row }">{{ row.frequencyLabel }}</template>
            </el-table-column>
            <el-table-column prop="leaderName" label="负责人" width="90" />
            <el-table-column prop="pointCount" label="点位数" width="80" />
            <el-table-column prop="remark" label="点位顺序" min-width="240" show-overflow-tooltip />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="generateTask(row)">生成任务</el-button>
                <el-button link @click="openRoute(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="removeRoute(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="巡检任务" name="task">
        <div class="panel">
          <el-table :data="tasks">
            <el-table-column label="任务编号" width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.taskNo }}</span></template>
            </el-table-column>
            <el-table-column prop="routeName" label="路线" min-width="180" />
            <el-table-column label="计划日期" width="110">
              <template #default="{ row }"><span class="mono">{{ dateOnly(row.planDate) }}</span></template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="90" />
            <el-table-column prop="totalPoints" label="点位" width="70" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="tag-box" :style="row.status === 'DONE'
                  ? 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
                  : 'color:#3b6ea5;background:rgba(59,110,165,.08);border-color:rgba(59,110,165,.2)'">
                  {{ row.status === 'DONE' ? '已完成' : row.status === 'DOING' ? '执行中' : '待执行' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="abnormalCount" label="异常项" width="80" />
            <el-table-column label="完成时间" width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.finishTime || '—' }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="routeVisible" :title="routeForm.route.id ? '编辑路线' : '新建路线'" width="760px">
      <el-form label-width="90px">
        <div class="form-grid">
          <el-form-item label="路线名称"><el-input v-model="routeForm.route.name" /></el-form-item>
          <el-form-item label="设施类型">
            <el-select v-model="routeForm.route.facilityType" style="width: 100%">
              <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="巡检频次">
            <el-select v-model="routeForm.route.frequency" style="width: 100%">
              <el-option label="每日" value="DAILY" />
              <el-option label="每周" value="WEEKLY" />
              <el-option label="每月" value="MONTHLY" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人">
            <el-select v-model="routeForm.route.leaderId" filterable style="width: 100%">
              <el-option v-for="p in personnel" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="routeForm.route.remark" /></el-form-item>

        <div class="points-head">
          <span class="panel-title">点位序列</span>
          <el-button size="small" @click="addPoint">添加点位</el-button>
        </div>
        <div v-for="(point, index) in routeForm.points" :key="index" class="point-block">
          <div class="row" style="margin-bottom: 6px">
            <span class="mono point-idx">{{ index + 1 }}</span>
            <el-input v-model="point.point.pointName" placeholder="点位名称" style="width: 220px" />
            <el-select v-model="point.point.equipmentId" filterable clearable placeholder="绑定设备" style="width: 260px">
              <el-option v-for="e in equipments" :key="e.id" :label="`${e.code} · ${e.name}`" :value="e.id" />
            </el-select>
            <div class="spacer" />
            <el-button link style="color: #b23a2f" @click="removePoint(index)">移除</el-button>
          </div>
          <div class="items-box">
            <div v-for="(item, i2) in point.items" :key="i2" class="row" style="margin-bottom: 4px">
              <el-input v-model="item.itemName" placeholder="检查项名称" style="width: 240px" size="small" />
              <el-checkbox v-model="item.requireValue" :true-value="1" :false-value="0" size="small">需填读数</el-checkbox>
              <el-input v-model="item.unit" placeholder="单位" style="width: 70px" size="small" />
              <el-button link size="small" style="color: #b23a2f" @click="point.items.splice(i2, 1)">删除</el-button>
            </div>
            <el-button link size="small" type="primary" @click="point.items.push({ itemName: '', requireValue: 0, unit: '' })">
              添加检查项
            </el-button>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="routeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoute">保存路线</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { inspectionApi, equipmentApi, baseDataApi } from '@/api'
import { dateOnly, facilityOptions } from '@/utils/format'

const tab = ref('route')
const routes = ref<any[]>([])
const tasks = ref<any[]>([])
const stats = ref<any>({})
const equipments = ref<any[]>([])
const personnel = ref<any[]>([])
const loading = ref(false)
const routeVisible = ref(false)
const routeForm = ref<any>({ route: {}, points: [] })

async function load() {
  loading.value = true
  try {
    routes.value = (await inspectionApi.routes()) as unknown as any[]
    const taskPage: any = await inspectionApi.taskPage({ pageNum: 1, pageSize: 50 })
    tasks.value = taskPage.list
    stats.value = await inspectionApi.stats(30)
  } finally {
    loading.value = false
  }
}

async function openRoute(row?: any) {
  if (!equipments.value.length) equipments.value = (await equipmentApi.options()) as unknown as any[]
  if (!personnel.value.length) {
    const page: any = await baseDataApi.personnelPage({ pageNum: 1, pageSize: 100 })
    personnel.value = page.list
  }
  if (row) {
    const detail: any = await inspectionApi.routeDetail(row.id)
    routeForm.value = {
      route: { ...detail.route },
      points: detail.points.map((p: any) => ({ point: { ...p.point }, items: p.items.map((i: any) => ({ ...i })) }))
    }
  } else {
    routeForm.value = {
      route: { frequency: 'WEEKLY', status: 'ENABLED', facilityType: 'POWER' },
      points: [{ point: { pointName: '', equipmentId: undefined }, items: [{ itemName: '', requireValue: 0, unit: '' }] }]
    }
  }
  routeVisible.value = true
}

function addPoint() {
  routeForm.value.points.push({ point: { pointName: '', equipmentId: undefined }, items: [] })
}

function removePoint(index: number) {
  routeForm.value.points.splice(index, 1)
}

async function saveRoute() {
  if (!routeForm.value.route.name || routeForm.value.points.length === 0) {
    ElMessage.warning('请填写路线名称并至少添加一个点位')
    return
  }
  await inspectionApi.saveRoute(routeForm.value)
  ElMessage.success('路线已保存')
  routeVisible.value = false
  load()
}

async function removeRoute(row: any) {
  await ElMessageBox.confirm(`确认删除路线「${row.name}」？`, '删除确认')
  await inspectionApi.deleteRoute(row.id)
  ElMessage.success('已删除')
  load()
}

async function generateTask(row: any) {
  await inspectionApi.generateTask(row.id)
  ElMessage.success('已生成今日巡检任务')
  load()
}

async function exportRecords() {
  await inspectionApi.exportExcel({})
}

onMounted(load)
</script>

<style scoped>
.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat {
  background: #fff;
  border: 1px solid var(--line);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
}

.stat-v {
  font-size: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.points-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--line);
  padding-top: 12px;
  margin: 4px 0 10px;
}

.point-block {
  border: 1px solid var(--line);
  padding: 10px;
  margin-bottom: 10px;
  background: #fbfaf6;
}

.point-idx {
  color: var(--brand);
  font-size: 11px;
  width: 18px;
}

.items-box {
  padding-left: 26px;
}
</style>

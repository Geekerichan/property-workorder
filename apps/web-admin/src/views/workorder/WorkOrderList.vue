<template>
  <div class="page">
    <PageHeader title="工单管理" description="覆盖计划维保、按需维保、故障报修、应急处置与巡检转单">
      <template #actions>
        <el-button v-if="isAdmin" @click="exportExcel">导出工单</el-button>
        <el-button v-if="isAdmin" type="primary" @click="router.push('/workorder/create')">创建工单</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.orderType" placeholder="全部类型" clearable style="width: 130px">
          <el-option v-for="item in orderTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.facilityType" placeholder="全部设施" clearable style="width: 120px">
          <el-option v-for="item in facilityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.urgency" placeholder="紧急等级" clearable style="width: 110px">
          <el-option v-for="item in urgencyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="工单号 / 标题" clearable style="width: 180px"
          @keyup.enter="load" />
        <el-checkbox v-model="query.overdueOnly">只看超时</el-checkbox>
        <el-button @click="load">筛选</el-button>
        <el-button link @click="reset">重置</el-button>
        <div class="spacer" />
        <span class="muted" style="font-size: 12px">共 {{ total }} 单</span>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id" @selection-change="onSelect">
        <el-table-column type="selection" width="40" :selectable="(row: any) => row.status === 'PENDING_DISPATCH'" />
        <el-table-column label="工单号" width="168">
          <template #default="{ row }">
            <span class="mono order-no" @click="router.push(`/workorder/${row.id}`)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="orderTypeLabel" label="类型" width="100" />
        <el-table-column prop="facilityLabel" label="设施" width="80" />
        <el-table-column label="紧急" width="70">
          <template #default="{ row }">
            <span :style="{ color: row.urgency === 'URGENT' ? '#b23a2f' : row.urgency === 'HIGH' ? '#c98a12' : '#6b675e' }">
              {{ row.urgencyLabel }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="mainWorkerName" label="主责人" width="80">
          <template #default="{ row }">{{ row.mainWorkerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="时限" width="132">
          <template #default="{ row }">
            <span class="mono" :style="{ color: row.overdue ? '#b23a2f' : '#6b675e', fontSize: '11px' }">
              {{ shortTime(row.deadline) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/workorder/${row.id}`)">详情</el-button>
            <el-button v-if="isAdmin && row.status === 'PENDING_DISPATCH'" link type="primary"
              @click="openDispatch(row)">派单</el-button>
            <el-button v-if="isAdmin && ['PENDING_DISPATCH', 'DISPATCHED', 'PROCESSING'].includes(row.status)" link
              @click="urge(row)">催办</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-button v-if="isAdmin" :disabled="selected.length === 0" @click="openBatch">
          批量派单（{{ selected.length }}）
        </el-button>
        <div class="spacer" />
        <el-pagination layout="prev, pager, next, sizes, total" :total="total" background
          v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50]"
          @change="load" />
      </div>
    </div>

    <el-dialog v-model="dispatchVisible" :title="dispatchMode === 'batch' ? '批量派单' : '指定派单'" width="440px">
      <div class="dialog-tip">
        {{ dispatchMode === 'batch' ? `将 ${selected.length} 张待派工单指派给同一名在岗人员` : current?.orderNo }}
      </div>
      <el-form label-width="80px">
        <el-form-item label="作业人员">
          <el-select v-model="dispatchForm.personnelId" filterable placeholder="仅显示在岗且资质有效人员"
            style="width: 100%">
            <el-option v-for="p in personnel" :key="p.id" :label="`${p.name}（${p.skills || '—'}）`" :value="p.id"
              :disabled="!p.certValid" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dispatchForm.remark" placeholder="可填写派单说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispatch">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { workOrderApi, baseDataApi } from '@/api'
import { shortTime, statusOptions, orderTypeOptions, facilityOptions, urgencyOptions } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const isAdmin = computed(() => auth.role === 'ADMIN')
const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const selected = ref<any[]>([])
const personnel = ref<any[]>([])
const dispatchVisible = ref(false)
const dispatchMode = ref<'single' | 'batch'>('single')
const current = ref<any>(null)
const submitting = ref(false)
const dispatchForm = ref({ personnelId: undefined as number | undefined, remark: '' })
const query = ref<any>({ pageNum: 1, pageSize: 10, status: '', orderType: '', facilityType: '', urgency: '', keyword: '', overdueOnly: false })

async function load() {
  loading.value = true
  try {
    const data: any = await workOrderApi.page(query.value)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.value = { pageNum: 1, pageSize: 10, status: '', orderType: '', facilityType: '', urgency: '', keyword: '', overdueOnly: false }
  load()
}

function onSelect(rowsSelected: any[]) {
  selected.value = rowsSelected
}

async function openDispatch(row: any) {
  current.value = row
  dispatchMode.value = 'single'
  dispatchForm.value = { personnelId: undefined, remark: '' }
  personnel.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
  dispatchVisible.value = true
}

async function openBatch() {
  dispatchMode.value = 'batch'
  dispatchForm.value = { personnelId: undefined, remark: '批量派单' }
  personnel.value = (await baseDataApi.availablePersonnel()) as unknown as any[]
  dispatchVisible.value = true
}

async function submitDispatch() {
  if (!dispatchForm.value.personnelId) {
    ElMessage.warning('请选择作业人员')
    return
  }
  submitting.value = true
  try {
    if (dispatchMode.value === 'batch') {
      await workOrderApi.batchDispatch({
        orderIds: selected.value.map((r) => r.id),
        personnelId: dispatchForm.value.personnelId
      })
      ElMessage.success('批量派单完成')
    } else {
      await workOrderApi.dispatch(current.value.id, {
        personnelId: dispatchForm.value.personnelId,
        remark: dispatchForm.value.remark
      })
      ElMessage.success('派单成功，电子合同已生成')
    }
    dispatchVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function urge(row: any) {
  await workOrderApi.urge(row.id)
  ElMessage.success('已发送催办通知')
  load()
}

async function exportExcel() {
  const params = new URLSearchParams()
  Object.entries(query.value).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && v !== false) params.append(k, String(v))
  })
  const { download } = await import('@/api/request')
  await download('/admin/workorder/export', Object.fromEntries(params))
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  flex-wrap: wrap;
}

.order-no {
  color: var(--brand);
  font-size: 11px;
  cursor: pointer;
}

.pager {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.dialog-tip {
  font-size: 12px;
  color: var(--ink-2);
  background: #f6f4ef;
  padding: 8px 10px;
  margin-bottom: 14px;
}
</style>

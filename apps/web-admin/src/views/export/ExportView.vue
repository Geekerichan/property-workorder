<template>
  <div class="page">
    <PageHeader title="导出中心" description="监管报表、责任记录包、巡检记录、合同 PDF 统一留痕" />

    <div class="export-grid">
      <div class="panel export-card">
        <div class="panel-head"><span class="panel-title">监管报送</span><span class="mono" style="font-size: 10px; color: #8a8578">REGULATORY</span></div>
        <div class="panel-body">
          <p class="card-desc">按监管字段导出电梯维保记录（设备注册代码、使用单位、维保单位、维保日期、维保人员、项目结论、下次维保日期、签章位），用于人工报送。</p>
          <el-button type="primary" @click="exportApi.regulatory()">导出监管报表</el-button>
        </div>
      </div>

      <div class="panel export-card">
        <div class="panel-head"><span class="panel-title">责任记录包</span><span class="mono" style="font-size: 10px; color: #8a8578">RESPONSIBILITY</span></div>
        <div class="panel-body">
          <p class="card-desc">导出设备近 6 个月维保/维修记录、责任周期标注、合同 PDF、留痕照片索引，打包 zip 用于责任认定。</p>
          <el-select v-model="equipmentId" filterable placeholder="选择设备" style="width: 100%; margin-bottom: 10px">
            <el-option v-for="e in equipments" :key="e.id" :label="`${e.code} · ${e.name}`" :value="e.id" />
          </el-select>
          <el-button type="primary" :disabled="!equipmentId" :loading="packing" @click="buildPackage">生成责任记录包</el-button>
          <div v-if="packageResult" class="package-result">
            <div class="mono">{{ packageResult.fileName }}</div>
            <div class="muted" style="font-size: 12px">
              工单 {{ packageResult.orderCount }} 条 · 合同 {{ packageResult.contractCount }} 份 · 照片 {{ packageResult.photoCount }} 张
            </div>
            <el-button link type="primary" @click="downloadPackage">下载 zip</el-button>
          </div>
        </div>
      </div>

      <div class="panel export-card">
        <div class="panel-head"><span class="panel-title">台账与档案</span><span class="mono" style="font-size: 10px; color: #8a8578">LEDGER</span></div>
        <div class="panel-body">
          <p class="card-desc">设备台账（迎检导出）、房屋档案（含业主与工单必填信息）、巡检记录，均带格式校验与错误行提示。</p>
          <div class="row" style="flex-wrap: wrap; gap: 8px">
            <el-button @click="equipmentApi.exportExcel({})">设备台账</el-button>
            <el-button @click="baseDataApi.houseExport()">房屋档案</el-button>
            <el-button @click="inspectionApi.exportExcel({})">巡检记录</el-button>
            <el-button @click="baseDataApi.houseTemplate()">房屋导入模板</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel" style="margin-top: 16px">
      <div class="panel-head"><span class="panel-title">导出记录</span></div>
      <el-table :data="logs" v-loading="loading">
        <el-table-column label="导出类型" width="150">
          <template #default="{ row }">
            {{ typeLabel(row.exportType) }}
          </template>
        </el-table-column>
        <el-table-column prop="params" label="导出范围" min-width="240" show-overflow-tooltip />
        <el-table-column prop="recordCount" label="记录数" width="80" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.createTime }}</span></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { exportApi, equipmentApi, baseDataApi, inspectionApi } from '@/api'
import { download } from '@/api/request'

const equipments = ref<any[]>([])
const equipmentId = ref<number>()
const packing = ref(false)
const packageResult = ref<any>(null)
const logs = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page: any = await exportApi.logs({ pageNum: 1, pageSize: 20 })
    logs.value = page.list
  } finally {
    loading.value = false
  }
}

async function buildPackage() {
  packing.value = true
  try {
    packageResult.value = await exportApi.responsibility(equipmentId.value!)
    ElMessage.success('责任记录包已生成')
    load()
  } finally {
    packing.value = false
  }
}

async function downloadPackage() {
  if (packageResult.value?.path) {
    await download('/file/download', { path: packageResult.value.path, fileName: packageResult.value.fileName })
  }
}

function typeLabel(type: string) {
  return {
    REGULATORY: '监管报表',
    RESPONSIBILITY: '责任记录包',
    INSPECTION: '巡检记录',
    HOUSE: '房屋档案',
    WORKORDER: '工单导出',
    CONTRACT_PDF: '合同 PDF'
  }[type] || type
}

onMounted(async () => {
  equipments.value = (await equipmentApi.options()) as unknown as any[]
  load()
})
</script>

<style scoped>
.export-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.export-card {
  display: flex;
  flex-direction: column;
}

.card-desc {
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.8;
  margin: 0 0 14px;
  min-height: 66px;
}

.package-result {
  margin-top: 12px;
  border-top: 1px dashed var(--line);
  padding-top: 10px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

@media (max-width: 1200px) {
  .export-grid {
    grid-template-columns: 1fr;
  }
}
</style>

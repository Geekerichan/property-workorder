<template>
  <div class="page">
    <PageHeader title="设备台账" description="一物一档：电梯、给排水、供配电、暖通、安防、绿化统一建档">
      <template #actions>
        <el-button @click="equipmentApi.exportExcel({ facilityType: query.facilityType })">导出台账</el-button>
        <el-button type="primary" @click="openForm()">新增设备</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <div class="filter-bar">
        <el-select v-model="query.facilityType" placeholder="全部设施" clearable style="width: 130px">
          <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option label="正常运行" value="NORMAL" />
          <el-option label="停用检修" value="REPAIR" />
          <el-option label="禁止使用" value="LOCKED" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="编号 / 名称 / 位置" clearable style="width: 200px"
          @keyup.enter="load" />
        <el-button @click="load">筛选</el-button>
        <div class="spacer" />
        <span class="muted" style="font-size: 12px">共 {{ total }} 台</span>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column label="设备编号" width="120">
          <template #default="{ row }"><span class="mono code">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="设备名称" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <router-link :to="`/equipment/${row.id}`" class="link">{{ row.name }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="facilityLabel" label="设施类型" width="90" />
        <el-table-column prop="location" label="安装位置" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.maintainOverdue === 1 && row.status === 'NORMAL' ? 'REPAIR' : row.status" />
            <span v-if="row.maintainOverdue === 1" class="overdue-flag">逾期</span>
          </template>
        </el-table-column>
        <el-table-column prop="companyName" label="维保单位" width="180" show-overflow-tooltip />
        <el-table-column label="下次年检" width="110">
          <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ dateOnly(row.nextAnnualDate) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/equipment/${row.id}`)">档案</el-button>
            <el-button link @click="openQr(row)">二维码</el-button>
            <el-button link @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <div class="spacer" />
        <el-pagination layout="prev, pager, next, total" :total="total" background
          v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑设备' : '新增设备'" width="680px">
      <el-form :model="form" label-width="100px">
        <div class="form-grid">
          <el-form-item label="设备编号"><el-input v-model="form.code" placeholder="如 DT-1-01" /></el-form-item>
          <el-form-item label="设施类型">
            <el-select v-model="form.facilityType" style="width: 100%">
              <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="设备名称"><el-input v-model="form.name" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="所属小区">
            <el-select v-model="form.communityId" clearable style="width: 100%">
              <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼栋">
            <el-select v-model="form.buildingId" clearable style="width: 100%">
              <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="安装位置"><el-input v-model="form.location" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="投用日期">
            <el-date-picker v-model="form.commissionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="维保单位">
            <el-select v-model="form.companyId" clearable style="width: 100%">
              <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="安全管理员"><el-input v-model="form.safetyManager" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.safetyPhone" maxlength="11" /></el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="下次年检"><el-date-picker v-model="form.nextAnnualDate" type="date"
            value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          <el-form-item label="运行状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="正常运行" value="NORMAL" />
              <el-option label="停用检修" value="REPAIR" />
              <el-option label="禁止使用" value="LOCKED" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="经度"><el-input v-model="form.longitude" placeholder="116.404512" /></el-form-item>
          <el-form-item label="纬度"><el-input v-model="form.latitude" placeholder="39.915233" /></el-form-item>
        </div>
        <el-form-item label="扩展参数">
          <el-input v-model="form.extJson" type="textarea" :rows="4"
            placeholder='JSON 格式，如 {"额定载重":"1000kg","层站数":"12层12站"}' />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrVisible" title="设备二维码" width="380px">
      <div class="qr-box">
        <img :src="qrImage" alt="设备二维码" class="qr-img" />
        <div class="qr-content mono">{{ qrContent }}</div>
        <div class="muted" style="font-size: 12px; text-align: center">
          签名已内置，业主扫码即可查看公开维保信息
        </div>
        <div class="row" style="justify-content: center">
          <el-button @click="printQr">标记已打印</el-button>
          <el-button type="primary" @click="downloadQr">下载二维码</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { equipmentApi, baseDataApi } from '@/api'
import { dateOnly, facilityOptions } from '@/utils/format'

const router = useRouter()
const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref<any>({ pageNum: 1, pageSize: 10, facilityType: '', status: '', keyword: '' })
const formVisible = ref(false)
const form = ref<any>({})
const communities = ref<any[]>([])
const buildings = ref<any[]>([])
const companies = ref<any[]>([])
const qrVisible = ref(false)
const qrContent = ref('')
const qrImage = ref('')
const qrEquipmentId = ref(0)

async function load() {
  loading.value = true
  try {
    const data: any = await equipmentApi.page(query.value)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openForm(row?: any) {
  if (!communities.value.length) communities.value = (await baseDataApi.communities()) as unknown as any[]
  if (!companies.value.length) companies.value = (await baseDataApi.companies()) as unknown as any[]
  buildings.value = (await baseDataApi.buildings(row?.communityId)) as unknown as any[]
  if (row) {
    const detail: any = await equipmentApi.detail(row.id)
    form.value = { ...detail }
  } else {
    form.value = { status: 'NORMAL', communityId: 1 }
  }
  formVisible.value = true
}

async function save() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写设备编号与名称')
    return
  }
  await equipmentApi.save(form.value)
  ElMessage.success('设备档案已保存')
  formVisible.value = false
  load()
}

async function openQr(row: any) {
  qrEquipmentId.value = row.id
  const info: any = await equipmentApi.qrInfo(row.id)
  qrContent.value = info.content
  qrImage.value = `/api/admin/equipment/${row.id}/qr.png?token=${localStorage.getItem('wo_token')}`
  qrVisible.value = true
}

async function printQr() {
  await equipmentApi.recordPrint(qrEquipmentId.value)
  ElMessage.success('已记录打印次数')
}

function downloadQr() {
  const link = document.createElement('a')
  link.href = qrImage.value
  link.download = `设备二维码-${qrEquipmentId.value}.png`
  link.click()
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
}

.code {
  font-size: 11px;
  color: var(--brand);
}

.link {
  color: var(--ink);
  text-decoration: none;
  border-bottom: 1px solid rgba(217, 72, 15, 0.25);
}

.overdue-flag {
  color: var(--state-danger);
  font-size: 10px;
  border: 1px solid rgba(178, 58, 47, 0.3);
  padding: 0 4px;
  margin-left: 4px;
}

.pager {
  display: flex;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.qr-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.qr-img {
  width: 240px;
  height: 240px;
  align-self: center;
  border: 1px solid var(--line);
  padding: 8px;
  background: #fff;
}

.qr-content {
  font-size: 10px;
  color: var(--ink-2);
  word-break: break-all;
  background: #f6f4ef;
  padding: 6px 8px;
}
</style>

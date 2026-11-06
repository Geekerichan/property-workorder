<template>
  <div class="page">
    <PageHeader title="电子合同" description="接单自动生成，双方平台内确认生效，责任周期 45–60 天可配">
      <template #actions>
        <el-input v-model="query.keyword" placeholder="合同编号 / 服务方 / 设备" clearable style="width: 220px"
          @keyup.enter="load" />
        <el-button @click="load">查询</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="合同编号" width="160">
          <template #default="{ row }">
            <span class="mono contract-no" @click="openDetail(row)">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="设备 / 服务内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="partyB" label="服务方" min-width="180" show-overflow-tooltip />
        <el-table-column label="责任周期" width="90">
          <template #default="{ row }"><span class="mono">{{ row.responsibilityDays }} 天</span></template>
        </el-table-column>
        <el-table-column label="起止时间" width="200">
          <template #default="{ row }">
            <span class="mono" style="font-size: 11px">{{ shortTime(row.respStart) }}<br />{{ shortTime(row.respEnd) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag-box" :style="statusStyle(row.status)">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">合同详情</el-button>
            <el-button link @click="contractApi.pdf(row.id)">PDF</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <div class="spacer" />
        <el-pagination layout="prev, pager, next, total" :total="total" background
          v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="电子合同详情" size="620px">
      <div v-if="detail" class="contract-paper">
        <h1 class="paper-title">设备维保服务电子合同</h1>
        <div class="paper-no mono">合同编号：{{ detail.contractNo }}</div>
        <table class="paper-table">
          <tr><th>甲方（物业方）</th><td>{{ detail.partyA }}</td></tr>
          <tr><th>乙方（服务方）</th><td>{{ detail.partyB }}</td></tr>
          <tr><th>关联工单</th><td class="mono">{{ detail.order?.orderNo }}</td></tr>
          <tr><th>设备</th><td>{{ detail.equipmentName }}</td></tr>
          <tr><th>服务内容</th><td>{{ detail.serviceContent }}</td></tr>
          <tr><th>责任周期</th><td><span class="mono">{{ detail.responsibilityDays }} 天</span>（自工单完成时间起算）</td></tr>
          <tr><th>周期起止</th><td class="mono">{{ shortTime(detail.respStart) }} ~ {{ shortTime(detail.respEnd) }}</td></tr>
          <tr><th>甲方确认</th><td>
            {{ detail.signTimeA ? `${detail.signUserA}　${shortTime(detail.signTimeA)}` : '待确认' }}
            <el-button v-if="!detail.signTimeA" link type="primary" style="margin-left: 8px"
              @click="confirm('A')">物业方确认</el-button>
          </td></tr>
          <tr><th>乙方确认</th><td>
            {{ detail.signTimeB ? `${detail.signUserB}　${shortTime(detail.signTimeB)}` : '待确认' }}
            <el-button v-if="!detail.signTimeB" link type="primary" style="margin-left: 8px"
              @click="confirm('B')">接单方确认</el-button>
          </td></tr>
        </table>
        <div class="paper-sign">
          <span>甲方（签章）：____________</span>
          <span>乙方（签章）：____________</span>
        </div>
      </div>

      <div class="chain-head">同设备责任链</div>
      <div v-if="!detail?.chain?.length" class="empty-line">该设备暂无相邻合同</div>
      <div v-for="node in detail?.chain || []" :key="node.id" class="chain-node">
        <span class="mono chain-no">{{ node.contractNo }}</span>
        <span>{{ node.partyB }}</span>
        <span class="mono muted" style="font-size: 11px">{{ dateOnly(node.respStart) }} ~ {{ dateOnly(node.respEnd) }}</span>
        <div class="spacer" />
        <span class="tag-box" :style="statusStyle(node.status)">{{ node.statusLabel }}</span>
      </div>

      <template #footer>
        <el-button @click="contractApi.generatePdf(detail.id).then(() => load())">生成 PDF</el-button>
        <el-button type="primary" @click="contractApi.pdf(detail.id)">下载 / 打印 PDF</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { contractApi } from '@/api'
import { dateOnly, shortTime } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref<any>({ pageNum: 1, pageSize: 10, keyword: '' })
const detailVisible = ref(false)
const detail = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const data: any = await contractApi.page(query.value)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await contractApi.detail(row.id)
  detailVisible.value = true
}

async function confirm(party: string) {
  await contractApi.confirm(detail.value.id, party)
  ElMessage.success('确认成功，双方确认后合同自动生效')
  detail.value = await contractApi.detail(detail.value.id)
  load()
}

function statusStyle(status: string) {
  if (status === 'ACTIVE') return 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
  if (status === 'EXPIRED') return 'color:#8a8578;background:rgba(138,133,120,.08);border-color:rgba(138,133,120,.2)'
  return 'color:#c98a12;background:rgba(201,138,18,.1);border-color:rgba(201,138,18,.24)'
}

onMounted(load)
</script>

<style scoped>
.contract-no {
  font-size: 11px;
  color: var(--brand);
  cursor: pointer;
}

.pager {
  display: flex;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.contract-paper {
  border: 1px solid var(--line);
  padding: 22px 24px;
  background: #fff;
}

.paper-title {
  font-size: 20px;
  text-align: center;
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.paper-no {
  text-align: center;
  font-size: 11px;
  color: var(--ink-2);
  margin-bottom: 16px;
}

.paper-table {
  width: 100%;
  border-collapse: collapse;
}

.paper-table th,
.paper-table td {
  border: 1px solid var(--line-strong);
  padding: 7px 9px;
  font-size: 12px;
  text-align: left;
  vertical-align: top;
}

.paper-table th {
  width: 110px;
  background: #f6f4ef;
  font-weight: 500;
  color: var(--ink-2);
}

.paper-sign {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
  font-size: 12px;
}

.chain-head {
  margin: 18px 0 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
}

.chain-node {
  display: flex;
  align-items: center;
  gap: 10px;
  border-left: 3px solid var(--line-strong);
  padding: 8px 10px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
}

.chain-no {
  font-size: 11px;
  color: var(--brand);
}
</style>

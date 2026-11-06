<template>
  <div class="page">
    <PageHeader title="质量稽核" description="照片与强制项自动校验，驳回重做扣分进入信用台账" />

    <el-tabs v-model="tab">
      <el-tab-pane label="待稽核工单" name="pending">
        <div class="panel">
          <el-table :data="pending" v-loading="loading">
            <el-table-column label="工单号" width="168">
              <template #default="{ row }">
                <router-link :to="`/workorder/${row.id}`" class="link mono" style="font-size: 11px">{{ row.orderNo }}</router-link>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="facilityLabel" label="设施" width="90" />
            <el-table-column prop="mainWorkerName" label="主责人" width="100" />
            <el-table-column label="完工时间" width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.finishTime }}</span></template>
            </el-table-column>
            <el-table-column label="验收结论" width="100">
              <template #default="{ row }">{{ row.acceptResult === 'PASS' ? '已通过' : row.acceptResult === 'REJECT' ? '已驳回' : '待验收' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/workorder/${row.id}`)">去稽核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="验收记录" name="acceptance">
        <div class="panel">
          <el-table :data="acceptances">
            <el-table-column prop="orderId" label="工单 ID" width="90" />
            <el-table-column label="结论" width="100">
              <template #default="{ row }">
                <span class="tag-box" :style="row.result === 'PASS'
                  ? 'color:#3f7a56;background:rgba(63,122,86,.08);border-color:rgba(63,122,86,.2)'
                  : 'color:#b23a2f;background:rgba(178,58,47,.08);border-color:rgba(178,58,47,.22)'">
                  {{ row.result === 'PASS' ? '通过' : '驳回重做' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="checkDetail" label="自动校验" min-width="240" show-overflow-tooltip />
            <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="deductScore" label="扣分" width="70" />
            <el-table-column prop="operatorName" label="验收人" width="90" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.createTime }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="信用台账" name="credit">
        <div class="panel" style="margin-bottom: 16px">
          <div class="panel-head"><span class="panel-title">信用汇总</span></div>
          <el-table :data="summary" size="small">
            <el-table-column label="对象" width="120">
              <template #default="{ row }">{{ row.targetType === 'COMPANY' ? '维保公司' : '人员' }}</template>
            </el-table-column>
            <el-table-column prop="targetName" label="名称" min-width="180" />
            <el-table-column prop="avgScore" label="平均评价" width="110">
              <template #default="{ row }"><span class="mono">{{ row.avgScore }} 星</span></template>
            </el-table-column>
            <el-table-column prop="evaluateCount" label="评价次数" width="100" />
            <el-table-column prop="deductScore" label="累计扣分" width="100">
              <template #default="{ row }"><span class="mono" style="color: #b23a2f">{{ row.deductScore }}</span></template>
            </el-table-column>
            <el-table-column prop="deductCount" label="驳回次数" width="100" />
          </el-table>
        </div>
        <div class="panel">
          <div class="panel-head"><span class="panel-title">信用明细</span></div>
          <el-table :data="credits">
            <el-table-column label="对象" width="90">
              <template #default="{ row }">{{ row.targetType === 'COMPANY' ? '公司' : '人员' }}</template>
            </el-table-column>
            <el-table-column prop="targetName" label="名称" width="160" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <span :style="{ color: row.creditType === 'DEDUCT' ? '#b23a2f' : '#3f7a56' }">
                  {{ row.creditType === 'DEDUCT' ? '扣分' : '评价' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="70" />
            <el-table-column prop="reason" label="原因" min-width="240" show-overflow-tooltip />
            <el-table-column prop="operatorName" label="记录人" width="90" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.createTime }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { auditApi } from '@/api'

const router = useRouter()
const tab = ref('pending')
const pending = ref<any[]>([])
const acceptances = ref<any[]>([])
const credits = ref<any[]>([])
const summary = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const task: any = await auditApi.pending({ pageNum: 1, pageSize: 50 })
    pending.value = task.list
    const acc: any = await auditApi.acceptance({ pageNum: 1, pageSize: 50 })
    acceptances.value = acc.list
    const credit: any = await auditApi.credit({ pageNum: 1, pageSize: 50 })
    credits.value = credit.list
    summary.value = (await auditApi.creditSummary()) as unknown as any[]
  } finally {
    loading.value = false
  }
}

watch(tab, load)
onMounted(load)
</script>

<style scoped>
.link {
  color: var(--brand);
  text-decoration: none;
}
</style>

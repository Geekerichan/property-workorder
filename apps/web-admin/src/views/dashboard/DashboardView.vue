<template>
  <div class="page">
    <PageHeader title="工作台" description="工单、设备、计划与责任的实时台账视图" />

    <div class="metric-grid">
      <MetricCard label="报修总量" :value="metrics.totalOrders" unit="单" hint="含计划维保与业主报修" />
      <MetricCard label="待处理" :value="metrics.pendingOrders" unit="单" hint="待派单 / 已派单 / 处理中" />
      <MetricCard label="超时未完成" :value="metrics.overdueOrders" unit="单" hint="已过时限且未归档" />
      <MetricCard label="维保及时率" :value="metrics.maintainOnTimeRate" unit="%" hint="按时完成 / 已完成" />
      <MetricCard label="验收合格率" :value="metrics.acceptPassRate" unit="%" hint="一次验收通过占比" />
      <MetricCard label="隐患整改率" :value="metrics.hazardRectifyRate" unit="%" hint="巡检转单归档 / 巡检转单" />
    </div>

    <div class="panel demo-accounts">
      <div class="panel-head">
        <span class="panel-title">演示账号</span>
        <span class="muted mono" style="font-size: 11px">DEMO ACCOUNTS · 业主端验证码 123456</span>
      </div>
      <div class="panel-body" style="padding: 0">
        <table class="account-table">
          <thead>
            <tr>
              <th>账号</th>
              <th>密码</th>
              <th>角色</th>
              <th>使用入口</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in demoAccounts" :key="a.username">
              <td class="mono">{{ a.username }}</td>
              <td class="mono">{{ a.password }}</td>
              <td>{{ a.role }}</td>
              <td class="muted">{{ a.entry }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">近 30 天工单趋势</span>
          <span class="muted mono" style="font-size: 11px">DAILY / 2026</span>
        </div>
        <div ref="trendRef" class="chart" />
      </div>
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">各设施工单分布</span>
          <span class="muted mono" style="font-size: 11px">BY FACILITY</span>
        </div>
        <div ref="facilityRef" class="chart" />
      </div>
    </div>

    <div class="lower-grid">
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">预警清单</span>
          <span class="muted" style="font-size: 11px">红牌为维保逾期与锁停设备</span>
        </div>
        <div class="panel-body" style="padding: 0">
          <template v-if="warnList.length > 0">
            <div v-for="item in warnList" :key="item.key" class="warn-row" :class="{ warn: item.level === 'warn' }">
              <span class="tag-box" :style="item.level === 'warn'
                ? 'color:#c98a12;background:rgba(201,138,18,.1);border-color:rgba(201,138,18,.24)'
                : 'color:#b23a2f;background:rgba(178,58,47,.08);border-color:rgba(178,58,47,.22)'">
                {{ item.type }}
              </span>
              <span class="warn-name">{{ item.name }}</span>
              <div class="spacer" />
              <span class="mono warn-date">{{ item.date }}</span>
            </div>
          </template>
          <EmptyState v-else text="暂无预警事项" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">电梯维保专项</span>
          <span class="muted mono" style="font-size: 11px">15 DAYS</span>
        </div>
        <div class="panel-body">
          <div class="kv">
            <span class="muted">电梯总数</span><span class="mono kv-v">{{ elevator.equipmentCount || 0 }} 台</span>
          </div>
          <div class="kv">
            <span class="muted">近 90 天完成维保</span><span class="mono kv-v">{{ elevator.finishedCount || 0 }} 单</span>
          </div>
          <div class="kv">
            <span class="muted">15 天周期执行率</span>
            <span class="mono kv-v strong">{{ elevator.executionRate || 0 }}%</span>
          </div>
          <div class="kv">
            <span class="muted">逾期未维保</span>
            <span class="mono kv-v" :style="elevator.overdueCount > 0 ? 'color:#b23a2f' : ''">
              {{ elevator.overdueCount || 0 }} 台
            </span>
          </div>
          <div class="hint-line">执行率 = 近 90 天完成单数 ÷（电梯台数 × 6 个周期）</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/MetricCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { workbenchApi } from '@/api'

const metrics = ref<any>({})
const elevator = ref<any>({})
const warnings = ref<any>({})

const demoAccounts = [
  { username: 'admin', password: 'admin123', role: '管理员', entry: 'PC 管理端（派单 / 验收 / 台账 / 档案 / 系统）' },
  { username: 'worker', password: 'wk123', role: '作业人员 · 张伟（电梯）', entry: 'H5 作业端' },
  { username: 'worker2', password: 'wk123', role: '作业人员 · 李强（水电）', entry: 'H5 作业端' },
  { username: 'worker3', password: 'wk123', role: '作业人员 · 王敏（巡检）', entry: 'H5 作业端' },
  { username: 'company', password: 'cp123', role: '维保公司管理员（恒安电梯）', entry: 'PC 管理端（仅本公司数据）' },
  { username: 'owner', password: 'ow123', role: '业主 · 陈建国', entry: 'PC 演示；H5 用手机号 13800000001' }
]
const trendRef = ref<HTMLElement>()
const facilityRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let facilityChart: echarts.ECharts | null = null

const warnList = computed(() => {
  const list: any[] = []
  ;(warnings.value.overdueEquipments || []).forEach((item: any) => {
    list.push({ key: 'o' + item.id, type: '维保逾期', name: item.name, date: item.code, level: 'danger' })
  })
  ;(warnings.value.annualChecks || []).forEach((item: any) => {
    list.push({
      key: 'a' + item.id,
      type: item.daysLeft < 0 ? '年检超期' : '年检临期',
      name: item.name,
      date: `剩余 ${item.daysLeft} 天`,
      level: item.daysLeft <= 7 ? 'danger' : 'warn'
    })
  })
  ;(warnings.value.contracts || []).forEach((item: any) => {
    list.push({
      key: 'c' + item.id,
      type: '责任周期临期',
      name: item.contractNo + ' ' + item.partyB,
      date: `剩余 ${item.daysLeft} 天`,
      level: 'warn'
    })
  })
  ;(warnings.value.certificates || []).forEach((item: any) => {
    list.push({
      key: 'p' + item.personnelId,
      type: '人员资质临期',
      name: `${item.name} ${item.certName || ''}`,
      date: `剩余 ${item.daysLeft} 天`,
      level: 'warn'
    })
  })
  ;(warnings.value.qualifications || []).forEach((item: any) => {
    list.push({
      key: 'q' + item.id,
      type: '公司资质临期',
      name: `${item.companyName || ''} ${item.qualType}`,
      date: `剩余 ${item.daysLeft} 天`,
      level: item.daysLeft <= 7 ? 'danger' : 'warn'
    })
  })
  return list
})

onMounted(async () => {
  const data: any = await workbenchApi.dashboard()
  metrics.value = data.metrics || {}
  elevator.value = data.elevator || {}
  warnings.value = data.warnings || {}

  trendChart = echarts.init(trendRef.value!)
  trendChart.setOption({
    grid: { left: 44, right: 16, top: 24, bottom: 36 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: (data.trend || []).map((i: any) => i.day),
      axisLine: { lineStyle: { color: '#e4e0d6' } },
      axisLabel: { color: '#8a8578', fontSize: 10, interval: 4 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#efebe2' } },
      axisLabel: { color: '#8a8578', fontSize: 10 }
    },
    series: [
      {
        type: 'line',
        data: (data.trend || []).map((i: any) => i.count),
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: '#d9480f', width: 1.5 },
        itemStyle: { color: '#d9480f' },
        areaStyle: { color: 'rgba(217,72,15,0.06)' }
      }
    ]
  })

  facilityChart = echarts.init(facilityRef.value!)
  facilityChart.setOption({
    grid: { left: 70, right: 24, top: 20, bottom: 24 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#efebe2' } },
      axisLabel: { color: '#8a8578', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: (data.facilityDistribution || []).map((i: any) => i.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b675e', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        data: (data.facilityDistribution || []).map((i: any) => i.value),
        itemStyle: { color: '#8a8578' }
      }
    ]
  })

  window.addEventListener('resize', resize)
})

function resize() {
  trendChart?.resize()
  facilityChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  facilityChart?.dispose()
})
</script>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.chart {
  height: 260px;
}

.lower-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.kv-v {
  font-size: 14px;
}

.kv-v.strong {
  font-size: 20px;
  color: var(--brand);
}

.hint-line {
  margin-top: 12px;
  font-size: 11px;
  color: var(--ink-3);
  line-height: 1.6;
}

.warn-name {
  font-size: 13px;
}

.warn-date {
  font-size: 11px;
  color: var(--ink-3);
}

.demo-accounts {
  margin-top: 16px;
}

.account-table {
  width: 100%;
  border-collapse: collapse;
}

.account-table th,
.account-table td {
  border-bottom: 1px solid var(--line);
  padding: 9px 16px;
  text-align: left;
  font-size: 13px;
}

.account-table th {
  background: #f1eee7;
  font-weight: 500;
  color: var(--ink-2);
  font-size: 12px;
}

.account-table td.mono {
  font-size: 12px;
}

.account-table td.muted {
  font-size: 12px;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .chart-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}
</style>

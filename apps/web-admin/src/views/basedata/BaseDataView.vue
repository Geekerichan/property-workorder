<template>
  <div class="page">
    <PageHeader title="基础档案" description="小区→楼栋→单元→房屋→业主 五级档案，一房多业主，Excel 批量导入导出" />

    <el-tabs v-model="tab">
      <!-- 楼盘 -->
      <el-tab-pane label="楼盘结构" name="structure">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">小区结构树</span>
            <el-button size="small" type="primary" @click="openCommunity()">新增小区</el-button>
          </div>
          <div class="panel-body">
            <el-tree :data="tree" node-key="key" :props="{ label: 'name', children: 'children' }" default-expand-all>
              <template #default="{ node, data }">
                <div class="tree-row">
                  <span class="tree-name">{{ data.name }}</span>
                  <span v-if="data.type === 'community'" class="muted mono tree-meta">{{ data.address || '' }}</span>
                  <span v-if="data.type === 'building'" class="muted mono tree-meta">{{ data.floors }} 层</span>
                  <span v-if="data.type === 'unit'" class="muted mono tree-meta">{{ data.houseCount }} 户</span>
                  <div class="spacer" />
                  <el-button v-if="data.type === 'community'" link size="small" @click.stop="openBuilding(data)">加楼栋</el-button>
                  <el-button v-if="data.type === 'building'" link size="small" @click.stop="openUnit(data)">加单元</el-button>
                  <el-button link size="small" style="color: #b23a2f" @click.stop="removeNode(data)">删除</el-button>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
      </el-tab-pane>

      <!-- 房屋 -->
      <el-tab-pane label="房屋与业主" name="house">
        <div class="panel">
          <div class="filter-bar">
            <el-select v-model="houseQuery.communityId" placeholder="全部小区" clearable style="width: 140px"
              @change="onCommunityChange">
              <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="houseQuery.buildingId" placeholder="全部楼栋" clearable style="width: 120px"
              @change="onBuildingChange">
              <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
            <el-select v-model="houseQuery.unitId" placeholder="全部单元" clearable style="width: 110px">
              <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
            </el-select>
            <el-input v-model="houseQuery.roomNo" placeholder="房号" clearable style="width: 120px" />
            <el-input v-model="houseQuery.ownerKeyword" placeholder="业主姓名 / 手机号" clearable style="width: 160px"
              @keyup.enter="loadHouses" />
            <el-button @click="loadHouses">筛选</el-button>
            <div class="spacer" />
            <el-button @click="baseDataApi.houseTemplate()">下载模板</el-button>
            <el-upload :show-file-list="false" :http-request="uploadHouse" accept=".xlsx,.xls">
              <el-button>批量导入</el-button>
            </el-upload>
            <el-button @click="baseDataApi.houseExport()">导出档案</el-button>
            <el-button type="primary" @click="openHouse()">新增房屋</el-button>
          </div>

          <el-table :data="houses" v-loading="loading">
            <el-table-column label="房号" width="130">
              <template #default="{ row }"><span class="mono">{{ row.roomNo }}</span></template>
            </el-table-column>
            <el-table-column prop="communityName" label="小区" width="120" />
            <el-table-column prop="buildingName" label="楼栋" width="100" />
            <el-table-column prop="unitName" label="单元" width="80" />
            <el-table-column prop="floorNo" label="楼层" width="70" />
            <el-table-column label="面积" width="90">
              <template #default="{ row }"><span class="mono">{{ row.area || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="业主" min-width="200">
              <template #default="{ row }">
                <span v-for="(o, i) in row.owners" :key="o.ownerId" class="owner-chip">
                  {{ o.ownerName }}<span class="mono muted"> {{ o.phone }}</span>{{ i < row.owners.length - 1 ? '、' : '' }}
                </span>
                <span v-if="!row.owners?.length" class="muted">未绑定</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openOwners(row)">业主绑定</el-button>
                <el-button link @click="openHouse(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="removeHouse(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <div class="spacer" />
            <el-pagination layout="prev, pager, next, total" :total="houseTotal" background
              v-model:current-page="houseQuery.pageNum" v-model:page-size="houseQuery.pageSize" @change="loadHouses" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 班组人员 -->
      <el-tab-pane label="班组与人员" name="staff">
        <div class="panel" style="margin-bottom: 16px">
          <div class="panel-head">
            <span class="panel-title">班组</span>
            <el-button size="small" type="primary" @click="openTeam()">新增班组</el-button>
          </div>
          <el-table :data="teams" size="small">
            <el-table-column prop="name" label="班组名称" width="160" />
            <el-table-column prop="teamType" label="维修类型" width="120" />
            <el-table-column label="负责人" width="120">
              <template #default="{ row }">{{ personnelName(row.leaderId) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link @click="openTeam(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="removeTeam(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">人员与资质</span>
            <div class="row">
              <el-button size="small" @click="openPersonnel()">新增人员</el-button>
            </div>
          </div>
          <el-table :data="personnel" size="small">
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column label="手机号" width="130">
              <template #default="{ row }"><span class="mono">{{ row.phone }}</span></template>
            </el-table-column>
            <el-table-column prop="teamName" label="班组" width="110" />
            <el-table-column prop="skills" label="技能标签" min-width="160" show-overflow-tooltip />
            <el-table-column label="资质" min-width="180">
              <template #default="{ row }">
                {{ row.certName || '未登记' }}
                <span class="mono muted" style="font-size: 11px"> {{ dateOnly(row.certExpireDate) }}</span>
                <span v-if="row.certExpired" class="cert-danger">已过期</span>
                <span v-else-if="row.certMissing" class="cert-danger">缺失</span>
              </template>
            </el-table-column>
            <el-table-column label="在岗状态" width="110">
              <template #default="{ row }">
                <el-select :model-value="row.dutyStatus" size="small" style="width: 90px"
                  @change="(v: string) => changeDuty(row, v)">
                  <el-option label="在岗" value="ON_DUTY" />
                  <el-option label="请假" value="LEAVE" />
                  <el-option label="离职" value="RESIGNED" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button link @click="openPersonnel(row)">编辑</el-button>
                <el-button link style="color: #b23a2f" @click="removePersonnel(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 维保公司 -->
      <el-tab-pane label="维保公司" name="company">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">维保公司与资质</span>
            <el-button size="small" type="primary" @click="openCompany()">新增公司</el-button>
          </div>
          <div class="panel-body">
            <div v-for="c in companies" :key="c.id" class="company-block">
              <div class="row">
                <span class="company-name">{{ c.name }}</span>
                <span class="mono muted" style="font-size: 11px">{{ c.creditCode }}</span>
                <div class="spacer" />
                <el-button link size="small" @click="openQualification(c)">新增资质</el-button>
                <el-button link size="small" @click="openCompany(c)">编辑</el-button>
              </div>
              <div class="company-meta">联系人 {{ c.contactName }}　{{ c.contactPhone }}　{{ c.address }}</div>
              <table class="qual-table">
                <tr><th>资质类型</th><th>证书编号</th><th>有效期至</th><th>状态</th><th></th></tr>
                <tr v-for="q in c.qualifications || []" :key="q.id">
                  <td>{{ q.qualType }}</td>
                  <td class="mono">{{ q.certNo }}</td>
                  <td class="mono">{{ dateOnly(q.expireDate) }}</td>
                  <td :style="isExpiring(q.expireDate) ? 'color:#b23a2f' : ''">
                    {{ isExpiring(q.expireDate) ? '临期/过期' : '有效' }}
                  </td>
                  <td>
                    <el-button link size="small" style="color: #b23a2f"
                      @click="baseDataApi.deleteQualification(q.id).then(loadCompanies)">删除</el-button>
                  </td>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form :model="dialogForm" label-width="90px">
        <template v-if="dialogType === 'community'">
          <el-form-item label="小区名称"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="dialogForm.address" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="dialogForm.contactName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="dialogForm.contactPhone" maxlength="11" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'building'">
          <el-form-item label="所属小区"><el-input :model-value="dialogForm.communityName" disabled /></el-form-item>
          <el-form-item label="楼栋名称"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="楼层数"><el-input-number v-model="dialogForm.floors" :min="1" :max="99" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'unit'">
          <el-form-item label="所属楼栋"><el-input :model-value="dialogForm.buildingName" disabled /></el-form-item>
          <el-form-item label="单元名称"><el-input v-model="dialogForm.name" placeholder="如 1单元" /></el-form-item>
          <el-form-item label="楼层数"><el-input-number v-model="dialogForm.floors" :min="1" :max="99" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'house'">
          <el-form-item label="单元">
            <el-select v-model="dialogForm.unitId" filterable style="width: 100%">
              <el-option v-for="u in units" :key="u.id" :label="`${u.name}（楼栋 ${u.buildingId}）`" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房号"><el-input v-model="dialogForm.roomNo" placeholder="如 1-1-101" /></el-form-item>
          <el-form-item label="楼层"><el-input-number v-model="dialogForm.floorNo" :min="1" :max="99" /></el-form-item>
          <el-form-item label="建筑面积"><el-input-number v-model="dialogForm.area" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="房屋类型"><el-input v-model="dialogForm.houseType" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'team'">
          <el-form-item label="班组名称"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="维修类型"><el-input v-model="dialogForm.teamType" placeholder="如 电梯 / 水电" /></el-form-item>
          <el-form-item label="负责人">
            <el-select v-model="dialogForm.leaderId" clearable filterable style="width: 100%">
              <el-option v-for="p in personnel" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'personnel'">
          <el-form-item label="姓名"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="dialogForm.phone" maxlength="11" /></el-form-item>
          <el-form-item label="班组">
            <el-select v-model="dialogForm.teamId" clearable style="width: 100%">
              <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="技能标签"><el-input v-model="dialogForm.skills" placeholder="逗号分隔" /></el-form-item>
          <el-form-item label="资质名称"><el-input v-model="dialogForm.certName" placeholder="如 电工证" /></el-form-item>
          <el-form-item label="资质到期">
            <el-date-picker v-model="dialogForm.certExpireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="在岗状态">
            <el-select v-model="dialogForm.dutyStatus" style="width: 100%">
              <el-option label="在岗" value="ON_DUTY" />
              <el-option label="请假" value="LEAVE" />
              <el-option label="离职" value="RESIGNED" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'company'">
          <el-form-item label="公司名称"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="信用代码"><el-input v-model="dialogForm.creditCode" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="dialogForm.contactName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="dialogForm.contactPhone" maxlength="11" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="dialogForm.address" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'qualification'">
          <el-form-item label="公司"><el-input :model-value="dialogForm.companyName" disabled /></el-form-item>
          <el-form-item label="资质类型"><el-input v-model="dialogForm.qualType" /></el-form-item>
          <el-form-item label="证书编号"><el-input v-model="dialogForm.certNo" /></el-form-item>
          <el-form-item label="有效期至">
            <el-date-picker v-model="dialogForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDialog">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="ownerVisible" title="业主绑定" size="460px">
      <div class="owner-house mono">{{ currentHouse?.roomNo }} · {{ currentHouse?.communityName }}{{ currentHouse?.buildingName }}{{ currentHouse?.unitName }}</div>
      <div v-for="o in currentOwners" :key="o.relationId" class="owner-row">
        <div>
          <div>{{ o.ownerName }} <span class="tag-box" style="color:#6b675e;background:#f4f1ea;border-color:#e4e0d6">
            {{ o.relation === 'OWNER' ? '业主' : o.relation === 'FAMILY' ? '家属' : '租户' }}</span></div>
          <div class="mono muted" style="font-size: 11px">{{ o.phone }}{{ o.isPrimary === 1 ? ' · 主业主' : '' }}</div>
        </div>
        <div class="spacer" />
        <el-button link style="color: #b23a2f" @click="unbind(o)">解绑</el-button>
      </div>
      <div class="panel" style="margin-top: 16px">
        <div class="panel-head"><span class="panel-title">绑定业主</span></div>
        <div class="panel-body">
          <el-form label-width="70px">
            <el-form-item label="姓名"><el-input v-model="bindForm.name" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="bindForm.phone" maxlength="11" /></el-form-item>
            <el-form-item label="关系">
              <el-select v-model="bindForm.relation" style="width: 100%">
                <el-option label="业主" value="OWNER" />
                <el-option label="家属" value="FAMILY" />
                <el-option label="租户" value="TENANT" />
              </el-select>
            </el-form-item>
            <el-form-item label="主业主"><el-switch v-model="bindForm.primary" /></el-form-item>
            <el-button type="primary" @click="bindOwner">确认绑定</el-button>
          </el-form>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { baseDataApi } from '@/api'
import { dateOnly } from '@/utils/format'

const tab = ref('structure')
const tree = ref<any[]>([])
const communities = ref<any[]>([])
const buildings = ref<any[]>([])
const units = ref<any[]>([])
const houses = ref<any[]>([])
const houseTotal = ref(0)
const houseQuery = ref<any>({ pageNum: 1, pageSize: 10, communityId: undefined, buildingId: undefined, unitId: undefined, roomNo: '', ownerKeyword: '' })
const teams = ref<any[]>([])
const personnel = ref<any[]>([])
const companies = ref<any[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogType = ref('')
const dialogTitle = ref('')
const dialogForm = ref<any>({})

const ownerVisible = ref(false)
const currentHouse = ref<any>(null)
const currentOwners = ref<any[]>([])
const bindForm = ref<any>({ name: '', phone: '', relation: 'OWNER', primary: false })

async function loadTree() {
  const data: any[] = (await baseDataApi.tree()) as unknown as any[]
  tree.value = data.map((c) => ({
    key: 'c' + c.id,
    type: 'community',
    id: c.id,
    name: c.name,
    address: c.address,
    children: (c.buildings || []).map((b: any) => ({
      key: 'b' + b.id,
      type: 'building',
      id: b.id,
      communityId: c.id,
      name: b.name,
      floors: b.floors,
      children: (b.units || []).map((u: any) => ({
        key: 'u' + u.id,
        type: 'unit',
        id: u.id,
        buildingId: b.id,
        name: u.name,
        houseCount: u.houseCount
      }))
    }))
  }))
  communities.value = (await baseDataApi.communities()) as unknown as any[]
}

async function loadHouses() {
  loading.value = true
  try {
    const data: any = await baseDataApi.housePage(houseQuery.value)
    houses.value = data.list
    houseTotal.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadStaff() {
  teams.value = (await baseDataApi.teams()) as unknown as any[]
  const page: any = await baseDataApi.personnelPage({ pageNum: 1, pageSize: 200 })
  personnel.value = page.list
}

async function loadCompanies() {
  companies.value = (await baseDataApi.companies()) as unknown as any[]
}

async function onCommunityChange() {
  buildings.value = (await baseDataApi.buildings(houseQuery.value.communityId)) as unknown as any[]
  units.value = []
  houseQuery.value.buildingId = undefined
  houseQuery.value.unitId = undefined
}

async function onBuildingChange() {
  units.value = (await baseDataApi.units(houseQuery.value.buildingId)) as unknown as any[]
  houseQuery.value.unitId = undefined
}

function personnelName(id: number) {
  return personnel.value.find((p) => p.id === id)?.name || '—'
}

function openDialog(type: string, title: string, form: any) {
  dialogType.value = type
  dialogTitle.value = title
  dialogForm.value = { ...form }
  dialogVisible.value = true
}

function openCommunity() {
  openDialog('community', '新增小区', { name: '', address: '' })
}

function openBuilding(node: any) {
  openDialog('building', `新增楼栋 · ${node.name}`, { communityId: node.id, communityName: node.name, floors: 6 })
}

function openUnit(node: any) {
  openDialog('unit', `新增单元 · ${node.name}`, { buildingId: node.id, buildingName: node.name, floors: 6 })
}

async function openHouse(row?: any) {
  if (!communities.value.length) await loadTree()
  const communityId = row?.communityId || houseQuery.value.communityId || communities.value[0]?.id
  buildings.value = (await baseDataApi.buildings(communityId)) as unknown as any[]
  const buildingId = row?.buildingId || buildings.value[0]?.id
  units.value = (await baseDataApi.units(buildingId)) as unknown as any[]
  openDialog('house', row ? '编辑房屋' : '新增房屋', row ? { ...row } : { unitId: units.value[0]?.id, houseType: '住宅', floorNo: 1 })
}

function openTeam(row?: any) {
  openDialog('team', row ? '编辑班组' : '新增班组', row ? { ...row } : {})
}

function openPersonnel(row?: any) {
  openDialog('personnel', row ? '编辑人员' : '新增人员', row ? { ...row } : { dutyStatus: 'ON_DUTY' })
}

function openCompany(row?: any) {
  openDialog('company', row ? '编辑公司' : '新增公司', row ? { ...row } : {})
}

function openQualification(company: any) {
  openDialog('qualification', `新增资质 · ${company.name}`, { companyId: company.id, companyName: company.name })
}

async function saveDialog() {
  const type = dialogType.value
  const form = dialogForm.value
  if (type === 'community') await baseDataApi.saveCommunity(form)
  else if (type === 'building') await baseDataApi.saveBuilding(form)
  else if (type === 'unit') await baseDataApi.saveUnit(form)
  else if (type === 'house') await baseDataApi.saveHouse(form)
  else if (type === 'team') await baseDataApi.saveTeam(form)
  else if (type === 'personnel') await baseDataApi.savePersonnel(form)
  else if (type === 'company') await baseDataApi.saveCompany(form)
  else if (type === 'qualification') await baseDataApi.saveQualification(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  if (type === 'community' || type === 'building' || type === 'unit') await loadTree()
  else if (type === 'house') await loadHouses()
  else if (type === 'team' || type === 'personnel') await loadStaff()
  else await loadCompanies()
}

async function removeNode(node: any) {
  await ElMessageBox.confirm(`确认删除「${node.name}」？存在下级或引用时将被拒绝。`, '删除确认')
  if (node.type === 'community') await baseDataApi.deleteCommunity(node.id)
  else if (node.type === 'building') await baseDataApi.deleteBuilding(node.id)
  else await baseDataApi.deleteUnit(node.id)
  ElMessage.success('已删除')
  loadTree()
}

async function removeHouse(row: any) {
  await ElMessageBox.confirm(`确认删除房屋「${row.roomNo}」？`, '删除确认')
  await baseDataApi.deleteHouse(row.id)
  ElMessage.success('已删除')
  loadHouses()
}

async function changeDuty(row: any, status: string) {
  await baseDataApi.dutyStatus(row.id, status)
  ElMessage.success('在岗状态已更新')
  loadStaff()
}

async function removePersonnel(row: any) {
  await ElMessageBox.confirm(`确认删除人员「${row.name}」？`, '删除确认')
  await baseDataApi.deletePersonnel(row.id)
  loadStaff()
}

async function removeTeam(row: any) {
  await ElMessageBox.confirm(`确认删除班组「${row.name}」？`, '删除确认')
  await baseDataApi.deleteTeam(row.id)
  loadStaff()
}

async function uploadHouse(options: any) {
  const result: any = await baseDataApi.houseImport(options.file)
  if (result.errors?.length) {
    ElMessage.warning(`导入完成：成功 ${result.success} 行，失败 ${result.errors.length} 行`)
    ElMessageBox.alert(result.errors.slice(0, 20).join('<br/>'), '错误行提示', { dangerouslyUseHTMLString: true })
  } else {
    ElMessage.success(`导入成功 ${result.success} 行`)
  }
  loadHouses()
}

async function openOwners(row: any) {
  currentHouse.value = row
  currentOwners.value = row.owners || []
  bindForm.value = { name: '', phone: '', relation: 'OWNER', primary: false }
  ownerVisible.value = true
}

async function bindOwner() {
  if (!bindForm.value.name || !bindForm.value.phone) {
    ElMessage.warning('请填写业主姓名与手机号')
    return
  }
  const owner: any = await baseDataApi.saveOwner({ name: bindForm.value.name, phone: bindForm.value.phone })
  await baseDataApi.bindOwner({
    houseId: currentHouse.value.id,
    ownerId: owner.id,
    relation: bindForm.value.relation,
    primary: bindForm.value.primary
  })
  ElMessage.success('绑定成功')
  await loadHouses()
  currentOwners.value = (await baseDataApi.houseOwners(currentHouse.value.id)) as unknown as any[]
}

async function unbind(row: any) {
  await baseDataApi.unbindOwner(row.relationId)
  ElMessage.success('已解绑')
  await loadHouses()
  currentOwners.value = (await baseDataApi.houseOwners(currentHouse.value.id)) as unknown as any[]
}

function isExpiring(expireDate: string) {
  if (!expireDate) return false
  const days = (new Date(expireDate).getTime() - Date.now()) / 86400000
  return days <= 30
}

watch(tab, (value) => {
  if (value === 'house' && houses.value.length === 0) loadHouses()
  if (value === 'staff' && personnel.value.length === 0) loadStaff()
  if (value === 'company' && companies.value.length === 0) loadCompanies()
})

onMounted(async () => {
  await loadTree()
  await loadHouses()
  await loadStaff()
  await loadCompanies()
})
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

.tree-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding-right: 8px;
}

.tree-name {
  font-size: 13px;
}

.tree-meta {
  font-size: 11px;
}

.pager {
  display: flex;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}

.owner-chip {
  font-size: 12px;
}

.cert-danger {
  color: var(--state-danger);
  font-size: 10px;
  border: 1px solid rgba(178, 58, 47, 0.3);
  padding: 0 3px;
  margin-left: 4px;
}

.company-block {
  border: 1px solid var(--line);
  padding: 12px 14px;
  margin-bottom: 12px;
  background: #fbfaf6;
}

.company-name {
  font-size: 14px;
  font-weight: 600;
}

.company-meta {
  font-size: 11px;
  color: var(--ink-3);
  margin: 4px 0 8px;
}

.qual-table {
  width: 100%;
  border-collapse: collapse;
}

.qual-table th,
.qual-table td {
  border: 1px solid var(--line);
  padding: 5px 8px;
  font-size: 12px;
  text-align: left;
}

.qual-table th {
  background: #f1eee7;
  font-weight: 500;
  color: var(--ink-2);
}

.owner-house {
  font-size: 12px;
  color: var(--ink-2);
  border-bottom: 1px solid var(--line);
  padding-bottom: 10px;
  margin-bottom: 10px;
}

.owner-row {
  display: flex;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}
</style>

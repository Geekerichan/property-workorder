<template>
  <div class="page">
    <PageHeader title="系统管理" description="账号与角色、字典参数、审计日志、站内信与作业清单模板" />

    <el-tabs v-model="tab">
      <el-tab-pane label="用户与角色" name="user">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">账号列表</span>
            <div class="row">
              <el-input v-model="userQuery.keyword" placeholder="账号 / 姓名" clearable style="width: 180px"
                @keyup.enter="loadUsers" />
              <el-select v-model="userQuery.roleCode" placeholder="全部角色" clearable style="width: 140px">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="作业人员" value="WORKER" />
                <el-option label="维保公司管理员" value="COMPANY" />
                <el-option label="业主" value="OWNER" />
              </el-select>
              <el-button @click="loadUsers">筛选</el-button>
              <el-button type="primary" @click="openUser()">新增账号</el-button>
            </div>
          </div>
          <el-table :data="users">
            <el-table-column prop="username" label="登录账号" width="140">
              <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
            </el-table-column>
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="phone" label="手机号" width="130">
              <template #default="{ row }"><span class="mono">{{ row.phone || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="角色" width="120">
              <template #default="{ row }">{{ roleLabel(row.roleCode) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link @click="openUser(row)">编辑</el-button>
                <el-button link @click="resetPassword(row)">重置密码</el-button>
                <el-button link style="color: #b23a2f" @click="removeUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="字典管理" name="dict">
        <div class="two-col">
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">字典类型</span>
              <el-button size="small" @click="openDict()">新增</el-button>
            </div>
            <div class="dict-list">
              <div v-for="d in dicts" :key="d.id" class="dict-row" :class="{ active: currentDict === d.dictType }"
                @click="selectDict(d.dictType)">
                <span>{{ d.dictName }}</span>
                <span class="mono muted" style="font-size: 11px">{{ d.dictType }}</span>
              </div>
            </div>
          </div>
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">字典项 · {{ currentDict }}</span>
              <el-button size="small" @click="openDictItem()">新增字典项</el-button>
            </div>
            <el-table :data="dictItems" size="small">
              <el-table-column prop="itemLabel" label="显示名称" width="160" />
              <el-table-column prop="itemValue" label="字典值" width="180">
                <template #default="{ row }"><span class="mono">{{ row.itemValue }}</span></template>
              </el-table-column>
              <el-table-column prop="sortNo" label="排序" width="70" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link @click="openDictItem(row)">编辑</el-button>
                  <el-button link style="color: #b23a2f" @click="removeDictItem(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统参数" name="config">
        <div class="panel">
          <el-table :data="configs">
            <el-table-column prop="configName" label="参数名称" width="180" />
            <el-table-column prop="configKey" label="参数键" width="220">
              <template #default="{ row }"><span class="mono">{{ row.configKey }}</span></template>
            </el-table-column>
            <el-table-column label="参数值" width="160">
              <template #default="{ row }"><span class="mono">{{ row.configValue }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="220" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }"><el-button link @click="openConfig(row)">修改</el-button></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="log">
        <el-tabs v-model="logTab">
          <el-tab-pane label="操作日志" name="oper">
            <div class="panel">
              <el-table :data="operLogs" size="small">
                <el-table-column prop="module" label="模块" width="110" />
                <el-table-column prop="action" label="动作" width="140" />
                <el-table-column prop="content" label="摘要" min-width="220" show-overflow-tooltip />
                <el-table-column prop="operatorName" label="操作人" width="100" />
                <el-table-column prop="ip" label="IP" width="130">
                  <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.ip }}</span></template>
                </el-table-column>
                <el-table-column label="时间" width="170">
                  <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.operTime }}</span></template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="登录日志" name="login">
            <div class="panel">
              <el-table :data="loginLogs" size="small">
                <el-table-column prop="username" label="账号" width="140" />
                <el-table-column label="结果" width="80">
                  <template #default="{ row }">{{ row.success === 1 ? '成功' : '失败' }}</template>
                </el-table-column>
                <el-table-column prop="message" label="说明" min-width="160" />
                <el-table-column prop="ip" label="IP" width="130">
                  <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.ip }}</span></template>
                </el-table-column>
                <el-table-column label="时间" width="170">
                  <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.loginTime }}</span></template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>

      <el-tab-pane label="站内信" name="message">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">我的消息</span>
            <el-button size="small" @click="systemApi.readAll().then(loadMessages)">全部已读</el-button>
          </div>
          <div class="panel-body" style="padding: 0">
            <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ unread: msg.readFlag === 0 }"
              @click="readMessage(msg)">
              <div class="row">
                <span class="msg-title">{{ msg.title }}</span>
                <div class="spacer" />
                <span class="mono muted" style="font-size: 11px">{{ msg.createTime }}</span>
              </div>
              <div class="msg-content">{{ msg.content }}</div>
            </div>
            <div v-if="messages.length === 0" class="empty-line">暂无消息</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="作业清单模板" name="template">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">模板与强制拍照项</span>
            <el-button size="small" type="primary" @click="openTemplate()">新建模板</el-button>
          </div>
          <el-table :data="templates" size="small">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="template-items">
                  <div v-for="item in templateItemMap[row.id] || []" :key="item.id" class="template-item">
                    <span class="mono muted" style="width: 90px">{{ item.groupName }}</span>
                    <span>{{ item.itemName }}</span>
                    <span v-if="item.requirePhoto === 1" class="req-flag">强制拍照</span>
                    <span v-if="item.requireValue === 1" class="muted" style="font-size: 11px">读数 {{ item.unit || '' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="模板名称" min-width="200" />
            <el-table-column label="设施类型" width="110">
              <template #default="{ row }">{{ facilityLabel(row.facilityType) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }"><el-button link @click="editTemplate(row)">编辑</el-button></template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="dialogForm" label-width="90px">
        <template v-if="dialogType === 'user'">
          <el-form-item label="登录账号"><el-input v-model="dialogForm.username" /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="dialogForm.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="dialogForm.phone" maxlength="11" /></el-form-item>
          <el-form-item label="角色">
            <el-select v-model="dialogForm.roleCode" style="width: 100%">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="作业人员" value="WORKER" />
              <el-option label="维保公司管理员" value="COMPANY" />
              <el-option label="业主" value="OWNER" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="dialogForm.roleCode === 'COMPANY' || dialogForm.roleCode === 'WORKER'" label="所属公司">
            <el-select v-model="dialogForm.companyId" clearable style="width: 100%">
              <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="dialogForm.roleCode === 'WORKER'" label="关联人员">
            <el-select v-model="dialogForm.personnelId" clearable filterable style="width: 100%">
              <el-option v-for="p in personnelList" :key="p.id" :label="`${p.name}（${p.teamName || '未分组'}）`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="dialogForm.password" :placeholder="dialogForm.id ? '留空则不修改' : '至少 6 位'" show-password />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="dialogForm.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'dict'">
          <el-form-item label="字典类型"><el-input v-model="dialogForm.dictType" /></el-form-item>
          <el-form-item label="字典名称"><el-input v-model="dialogForm.dictName" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'dictItem'">
          <el-form-item label="显示名称"><el-input v-model="dialogForm.itemLabel" /></el-form-item>
          <el-form-item label="字典值"><el-input v-model="dialogForm.itemValue" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="dialogForm.sortNo" :min="0" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'config'">
          <el-form-item label="参数名称"><el-input v-model="dialogForm.configName" /></el-form-item>
          <el-form-item label="参数键"><el-input v-model="dialogForm.configKey" :disabled="!!dialogForm.id" /></el-form-item>
          <el-form-item label="参数值"><el-input v-model="dialogForm.configValue" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="dialogForm.remark" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'template'">
          <el-form-item label="模板名称"><el-input v-model="dialogForm.name" /></el-form-item>
          <el-form-item label="设施类型">
            <el-select v-model="dialogForm.facilityType" style="width: 100%">
              <el-option v-for="f in facilityOptions" :key="f.value" :label="f.label" :value="f.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="清单项">
            <div style="width: 100%">
              <div v-for="(item, index) in templateItems" :key="index" class="row" style="margin-bottom: 5px">
                <el-input v-model="item.groupName" placeholder="分组" style="width: 90px" size="small" />
                <el-input v-model="item.itemName" placeholder="项目名称" style="width: 170px" size="small" />
                <el-checkbox v-model="item.requirePhoto" :true-value="1" :false-value="0" size="small">拍照</el-checkbox>
                <el-checkbox v-model="item.requireValue" :true-value="1" :false-value="0" size="small">读数</el-checkbox>
                <el-button link size="small" style="color: #b23a2f" @click="templateItems.splice(index, 1)">删</el-button>
              </div>
              <el-button size="small" link type="primary"
                @click="templateItems.push({ groupName: '', itemName: '', requirePhoto: 0, requireValue: 0 })">
                添加清单项
              </el-button>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDialog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { systemApi, baseDataApi } from '@/api'
import { facilityOptions } from '@/utils/format'

const tab = ref('user')
const logTab = ref('oper')
const users = ref<any[]>([])
const userQuery = ref<any>({ pageNum: 1, pageSize: 20, keyword: '', roleCode: '' })
const dicts = ref<any[]>([])
const dictItems = ref<any[]>([])
const currentDict = ref('facility_type')
const configs = ref<any[]>([])
const operLogs = ref<any[]>([])
const loginLogs = ref<any[]>([])
const messages = ref<any[]>([])
const templates = ref<any[]>([])
const templateItemMap = ref<Record<number, any[]>>({})
const companies = ref<any[]>([])
const personnelList = ref<any[]>([])

const dialogVisible = ref(false)
const dialogType = ref('')
const dialogTitle = ref('')
const dialogForm = ref<any>({})
const templateItems = ref<any[]>([])

function roleLabel(code: string) {
  return { ADMIN: '管理员', WORKER: '作业人员', COMPANY: '维保公司管理员', OWNER: '业主' }[code] || code
}

function facilityLabel(value: string) {
  return facilityOptions.find((f) => f.value === value)?.label || value
}

async function loadUsers() {
  const data: any = await systemApi.userPage(userQuery.value)
  users.value = data.list
}

async function loadDicts() {
  dicts.value = (await systemApi.dictList()) as unknown as any[]
  await selectDict(currentDict.value)
}

async function selectDict(type: string) {
  currentDict.value = type
  dictItems.value = (await systemApi.dictItems(type)) as unknown as any[]
}

async function loadConfigs() {
  configs.value = (await systemApi.configList()) as unknown as any[]
}

async function loadLogs() {
  const oper: any = await systemApi.operLogs({ pageNum: 1, pageSize: 50 })
  operLogs.value = oper.list
  const login: any = await systemApi.loginLogs({ pageNum: 1, pageSize: 50 })
  loginLogs.value = login.list
}

async function loadMessages() {
  const page: any = await systemApi.messagePage({ pageNum: 1, pageSize: 50 })
  messages.value = page.list
}

async function loadTemplates() {
  templates.value = (await systemApi.templates()) as unknown as any[]
  for (const t of templates.value) {
    templateItemMap.value[t.id] = (await systemApi.templateItems(t.id)) as unknown as any[]
  }
}

async function loadResources() {
  if (!companies.value.length) companies.value = (await baseDataApi.companies()) as unknown as any[]
  if (!personnelList.value.length) {
    const page: any = await baseDataApi.personnelPage({ pageNum: 1, pageSize: 200 })
    personnelList.value = page.list
  }
}

function openDialog(type: string, title: string, form: any) {
  dialogType.value = type
  dialogTitle.value = title
  dialogForm.value = { ...form }
  dialogVisible.value = true
}

async function openUser(row?: any) {
  await loadResources()
  openDialog('user', row ? '编辑账号' : '新增账号', row ? { ...row, password: '' } : { roleCode: 'ADMIN', status: 'ENABLED' })
}

function openDict(row?: any) {
  openDialog('dict', row ? '编辑字典' : '新增字典', row ? { ...row } : {})
}

function openDictItem(row?: any) {
  openDialog('dictItem', row ? '编辑字典项' : '新增字典项', row ? { ...row } : { dictType: currentDict.value, sortNo: 0 })
}

function openConfig(row: any) {
  openDialog('config', '修改参数', { ...row })
}

async function openTemplate() {
  templateItems.value = [{ groupName: '现场确认', itemName: '', requirePhoto: 0, requireValue: 0 }]
  openDialog('template', '新建作业清单模板', { facilityType: 'ELEVATOR' })
}

async function editTemplate(row: any) {
  templateItems.value = (templateItemMap.value[row.id] || []).map((i: any) => ({
    id: i.id,
    groupName: i.groupName,
    itemName: i.itemName,
    requirePhoto: i.requirePhoto,
    requireValue: i.requireValue,
    unit: i.unit
  }))
  openDialog('template', '编辑作业清单模板', { ...row })
}

async function saveDialog() {
  const type = dialogType.value
  if (type === 'user') {
    await systemApi.saveUser(dialogForm.value)
    loadUsers()
  } else if (type === 'dict') {
    await systemApi.saveDict(dialogForm.value)
    loadDicts()
  } else if (type === 'dictItem') {
    await systemApi.saveDictItem(dialogForm.value)
    selectDict(currentDict.value)
  } else if (type === 'config') {
    await systemApi.saveConfig(dialogForm.value)
    loadConfigs()
  } else if (type === 'template') {
    await systemApi.saveTemplate({ template: dialogForm.value, items: templateItems.value })
    loadTemplates()
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
}

async function resetPassword(row: any) {
  const { value } = await ElMessageBox.prompt(`为账号「${row.username}」设置新密码`, '重置密码', {
    inputPattern: /^.{6,32}$/,
    inputErrorMessage: '密码长度需 6-32 位'
  })
  await systemApi.resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function removeUser(row: any) {
  await ElMessageBox.confirm(`确认删除账号「${row.username}」？`, '删除确认')
  await systemApi.deleteUser(row.id)
  loadUsers()
}

async function removeDictItem(row: any) {
  await ElMessageBox.confirm(`确认删除字典项「${row.itemLabel}」？`, '删除确认')
  await systemApi.deleteDictItem(row.id)
  selectDict(currentDict.value)
}

async function readMessage(msg: any) {
  if (msg.readFlag === 0) {
    await systemApi.read(msg.id)
    loadMessages()
  }
}

watch(tab, (value) => {
  if (value === 'dict') loadDicts()
  if (value === 'config') loadConfigs()
  if (value === 'log') loadLogs()
  if (value === 'message') loadMessages()
  if (value === 'template') loadTemplates()
})

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.two-col {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}

.dict-list {
  max-height: 420px;
  overflow: auto;
}

.dict-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 14px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
  cursor: pointer;
}

.dict-row.active {
  border-left: 3px solid var(--brand);
  background: #fdf8f4;
}

.msg-row {
  padding: 10px 16px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}

.msg-row.unread {
  border-left: 3px solid var(--brand);
  background: #fdf9f5;
}

.msg-title {
  font-size: 13px;
  font-weight: 600;
}

.msg-content {
  font-size: 12px;
  color: var(--ink-2);
  margin-top: 4px;
}

.template-items {
  padding: 8px 24px;
  background: #fbfaf6;
}

.template-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
  font-size: 12px;
}

.req-flag {
  color: var(--brand);
  font-size: 10px;
  border: 1px solid rgba(217, 72, 15, 0.3);
  padding: 0 4px;
}
</style>

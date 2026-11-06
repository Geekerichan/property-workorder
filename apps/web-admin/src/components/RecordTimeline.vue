<template>
  <div>
    <div v-if="!logs || logs.length === 0" class="empty-line">暂无流转记录</div>
    <div v-else class="timeline">
      <div v-for="log in logs" :key="log.id" class="timeline-node">
        <div class="row">
          <span class="node-action">{{ actionLabel(log.action) }}</span>
          <div class="spacer" />
          <span class="timeline-time">{{ log.createTime }}</span>
        </div>
        <div class="node-meta">
          <span v-if="log.fromStatus || log.toStatus" class="mono node-status">
            {{ statusText(log.fromStatus) }} → {{ statusText(log.toStatus) }}
          </span>
          <span class="node-operator">{{ log.operatorName || '系统' }}</span>
        </div>
        <div v-if="log.reason" class="node-reason">{{ log.reason }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ logs: any[] }>()

const actionMap: Record<string, string> = {
  CREATE: '创建工单',
  DISPATCH: '派单',
  CLAIM: '抢单',
  START: '开始作业',
  SUBMIT: '完工提交',
  ACCEPT: '验收通过',
  REJECT: '驳回重做',
  RETURN: '退回重新派单',
  CANCEL: '作废',
  ARCHIVE: '归档',
  URGE: '催办',
  TRANSFER: '转派',
  REVIEW: '回访记录',
  PUBLISH: '公示发布',
  SOURCE: '触发源记录'
}

const statusMap: Record<string, string> = {
  PENDING_DISPATCH: '待派单',
  DISPATCHED: '已派单',
  PROCESSING: '处理中',
  PENDING_REVIEW: '待回访',
  ARCHIVED: '归档',
  CANCELED: '已作废'
}

function actionLabel(action: string) {
  return actionMap[action] || action
}

function statusText(status: string) {
  return statusMap[status] || '—'
}
</script>

<style scoped>
.node-action {
  font-size: 13px;
  font-weight: 600;
}

.node-meta {
  margin-top: 3px;
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--ink-2);
}

.node-status {
  font-size: 10px;
  color: var(--ink-3);
}

.node-reason {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-2);
  line-height: 1.6;
}
</style>

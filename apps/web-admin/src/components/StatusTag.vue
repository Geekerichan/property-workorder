<template>
  <span class="tag-box" :style="style">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string; type?: 'order' | 'urgency' | 'equipment' }>()

const statusMap: Record<string, { label: string; color: string }> = {
  PENDING_DISPATCH: { label: '待派单', color: '#8a8578' },
  DISPATCHED: { label: '已派单', color: '#3b6ea5' },
  PROCESSING: { label: '处理中', color: '#3b6ea5' },
  PENDING_REVIEW: { label: '待回访', color: '#c98a12' },
  ARCHIVED: { label: '归档', color: '#3f7a56' },
  CANCELED: { label: '已作废', color: '#8a8578' },
  LOW: { label: '低', color: '#8a8578' },
  NORMAL: { label: '一般', color: '#3b6ea5' },
  HIGH: { label: '紧急', color: '#c98a12' },
  URGENT: { label: '特急', color: '#b23a2f' },
  REPAIR: { label: '停用检修', color: '#c98a12' },
  LOCKED: { label: '禁止使用', color: '#b23a2f' }
}

const label = computed(() => statusMap[props.status]?.label || props.status || '—')
const color = computed(() => statusMap[props.status]?.color || '#8a8578')
const style = computed(() => ({
  color: color.value,
  background: hexToRgba(color.value, 0.1),
  borderColor: hexToRgba(color.value, 0.24)
}))

function hexToRgba(hex: string, alpha: number) {
  const value = hex.replace('#', '')
  const r = parseInt(value.substring(0, 2), 16)
  const g = parseInt(value.substring(2, 4), 16)
  const b = parseInt(value.substring(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

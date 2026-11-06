<template>
  <div class="metric">
    <div class="metric-label">{{ label }}</div>
    <div class="metric-value mono">
      {{ value }}<span class="metric-unit">{{ unit }}</span>
    </div>
    <div v-if="hint" class="metric-hint">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string | number; unit?: string; hint?: string }>()
</script>

<style scoped>
.metric {
  background: #fff;
  border: 1px solid var(--line);
  padding: 14px 16px 12px;
}

.metric-label {
  font-size: 12px;
  color: var(--ink-2);
  letter-spacing: 0.3px;
}

.metric-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 500;
  line-height: 1.1;
  color: var(--ink);
}

.metric-unit {
  font-size: 12px;
  color: var(--ink-3);
  margin-left: 4px;
}

.metric-hint {
  margin-top: 6px;
  font-size: 11px;
  color: var(--ink-3);
}
</style>

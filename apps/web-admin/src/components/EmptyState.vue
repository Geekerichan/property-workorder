<template>
  <div class="empty-line">{{ text }}</div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), { text: '暂无数据' })
</script>

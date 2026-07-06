<template>
  <el-card>
    <template #header>{{ title }}</template>
    <pre class="content">{{ content }}</pre>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ title: string; content: string }>();
</script>

<style scoped>
.content { white-space: pre-wrap; font-family: inherit; margin: 0; }
</style>

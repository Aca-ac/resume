<template>
  <div class="page">
    <h2>模板广场</h2>
    <p>模板选择页面开发中，可先使用简历列表导出功能。</p>
    <el-button type="primary" @click="$router.push('/resumes')">前往我的简历</el-button>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page {
  padding: 24px;
}
</style>

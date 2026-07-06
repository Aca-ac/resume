<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">Resume Assistant</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/resumes">Resumes</el-menu-item>
        <el-menu-item index="/match">Job Match</el-menu-item>
        <el-menu-item index="/interview/start">Interview</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ auth.username }}</span>
        <el-button link type="danger" @click="onLogout">Logout</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";

const auth = useAuthStore();
auth.load();
const router = useRouter();

function onLogout() {
  auth.logout();
  router.push("/login");
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #1f2d3d; color: #fff; }
.brand { padding: 16px; font-weight: 700; }
.header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; }
</style>

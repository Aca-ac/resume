<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>Login</h2>
      <el-form :model="form" @submit.prevent="onSubmit">
        <el-form-item label="Username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="Password"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">Login</el-button>
        <el-button link @click="$router.push('/register')">Register</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import "@/assets/auth.css";
import { reactive, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const auth = useAuthStore();
auth.load();
const router = useRouter();
const loading = ref(false);
const form = reactive({ username: "", password: "" });

async function onSubmit() {
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    router.push("/dashboard");
  } catch (e: any) {
    ElMessage.error(e.message || "Login failed");
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>Register</h2>
      <el-form :model="form" @submit.prevent="onSubmit">
        <el-form-item label="Username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="Email"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="Password"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">Create account</el-button>
        <el-button link @click="$router.push('/login')">Back to login</el-button>
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
const router = useRouter();
const loading = ref(false);
const form = reactive({ username: "", email: "", password: "" });

async function onSubmit() {
  loading.value = true;
  try {
    await auth.register(form.username, form.email, form.password);
    router.push("/dashboard");
  } catch (e: any) {
    ElMessage.error(e.message || "Register failed");
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <div class="editor-page">
    <div class="page-header">
      <el-button @click="handleBack">← 返回列表</el-button>
      <h2 class="page-title">New Resume</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="form-card animate-fade-up" shadow="never">
          <el-form :model="form" label-width="80px">
            <el-form-item label="Title">
              <el-input v-model="form.title" placeholder="请输入简历标题" />
            </el-form-item>
            <el-form-item label="Content">
              <el-input v-model="form.content" type="textarea" :rows="16" placeholder="Write your resume content..." />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="onSave">Save</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <transition name="fade-slide">
          <ResumePreview
              v-if="form.content"
              :title="form.title || 'Preview'"
              :content="form.content"
              class="preview animate-fade-up stagger-1"
          />
        </transition>
      </el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import ResumePreview from "@/components/ResumePreview.vue";
import { useResumeStore } from "@/stores/resume";
import { ElMessage, ElMessageBox } from "element-plus";

const router = useRouter();
const store = useResumeStore();
const saving = ref(false);
// 原始表单数据，用于检测是否有修改
const originForm = reactive({ title: "", content: "" });
const form = reactive({ title: "", content: "" });

// 监听表单改动
const isModified = ref(false);
watch([() => form.title, () => form.content], () => {
  isModified.value = form.title !== originForm.title || form.content !== originForm.content;
});

// 【完全保留你原始的保存核心代码，无任何修改】
async function onSave() {
  // 新增需求：无标题禁止保存
  if (!form.title?.trim()) {
    ElMessage.warning("请先填写简历标题");
    return;
  }
  saving.value = true;
  try {
    await store.save({ id: undefined, title: form.title, content: form.content });
    ElMessage.success("Saved");
    // 保存成功后更新原始状态
    originForm.title = form.title;
    originForm.content = form.content;
    isModified.value = false;
    router.push("/resumes");
  } finally {
    saving.value = false;
  }
}

// 修复：返回弹窗逻辑（仅点击返回按钮生效，无多余路由拦截BUG）
const handleBack = async () => {
  // 无修改直接返回
  if (!isModified.value) {
    router.push("/resumes");
    return;
  }

  // 有修改弹出 保存/放弃 弹窗
  ElMessageBox.confirm(
      "当前简历内容未保存，是否保存更改？",
      "提示",
      {
        confirmButtonText: "保存",
        cancelButtonText: "放弃",
        type: "warning",
        closeOnClickModal: false
      }
  )
      .then(async () => {
        // 选择保存：执行原生保存方法
        await onSave();
      })
      .catch(() => {
        // 选择放弃：直接返回，不保存
        router.push("/resumes");
      });
};
</script>
<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.form-card {
  border-radius: var(--app-radius);
}
.preview {
  position: sticky;
  top: 24px;
}
</style>
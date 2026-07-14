<!-- src/components/ListEditor.vue -->
<template>
  <div class="list-editor">
    <div
        v-for="(item, index) in modelValue"
        :key="index"
        class="list-item"
    >
      <div class="item-fields">
        <el-input
            v-for="field in fields"
            :key="field.key"
            v-model="item[field.key]"
            :type="field.type || 'text'"
            :rows="field.rows || 1"
            :placeholder="field.placeholder"
            size="small"
        />
      </div>
      <el-button type="danger" link @click="removeItem(index)">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
    <el-button type="primary" link @click="handleAdd">
      <el-icon><Plus /></el-icon> 添加{{ itemLabel }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Plus, Delete } from '@element-plus/icons-vue'

// ===== Props =====
const props = defineProps<{
  modelValue: any[]
  fields: Array<{
    key: string
    label: string
    placeholder: string
    type?: 'text' | 'textarea'
    rows?: number
  }>
  itemLabel?: string
}>()

// ===== Emits =====
const emit = defineEmits<{
  (e: 'update:modelValue', value: any[]): void
  (e: 'add'): void
}>()

// ===== Methods =====
function removeItem(index: number) {
  const newValue = [...props.modelValue]
  newValue.splice(index, 1)
  emit('update:modelValue', newValue)
}

function handleAdd() {
  emit('add')
}
</script>

<style scoped>
.list-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list-item {
  display: flex;
  gap: 8px;
  padding: 10px;
  background: #f8faf9;
  border-radius: 6px;
  align-items: flex-start;
}

.item-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-fields .el-input,
.item-fields .el-textarea {
  width: 100%;
}
</style>
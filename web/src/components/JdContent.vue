<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  content: string;
  jobName?: string;
  showHeader?: boolean;
  sources?: string[];  // 新增：来源列表
}>();

// 解析JD内容，将换行转为<br>或分段
const formattedContent = computed(() => {
  if (!props.content) return ['暂无JD内容'];

  // 按空行分段
  const sections = props.content.split(/\n\s*\n/);
  return sections.filter(s => s.trim());
});

// 检测是否是结构化JD（包含关键词）
const isStructured = computed(() => {
  const keywords = ['岗位职责', '任职要求', '职位描述', '薪资', '工作地点'];
  return keywords.some(kw => props.content.includes(kw));
});

// 是否有来源信息
const hasSources = computed(() => {
  return props.sources && props.sources.length > 0;
});

// 渲染文本（简单转义）
function renderText(text: string): string {
  return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
}

// 渲染来源信息
function renderSources(): string {
  if (!props.sources || props.sources.length === 0) return '';

  const sourceText = props.sources.map(s => `• ${s}`).join('\n');
  return `
    <div class="jd-sources">
      <div class="sources-divider"></div>
      <div class="sources-label">📌 信息来源：</div>
      <div class="sources-content">${sourceText.replace(/\n/g, '<br>')}</div>
    </div>
  `;
}
</script>

<template>
  <div class="jd-content">
    <!-- 头部 -->
    <div v-if="showHeader && jobName" class="jd-header">
      <h3 class="jd-title">{{ jobName }}</h3>
    </div>

    <!-- 内容 -->
    <div v-if="content" class="jd-body">
      <template v-if="isStructured">
        <!-- 结构化展示 -->
        <div v-for="(section, index) in formattedContent" :key="index" class="jd-section">
          <div v-html="renderText(section)" class="jd-text"></div>
        </div>
      </template>
      <template v-else>
        <!-- 纯文本展示 -->
        <div class="jd-text" v-html="renderText(content)"></div>
      </template>

      <!-- 来源信息 - 放在内容末尾 -->
      <div v-if="hasSources" class="jd-sources-wrapper">
        <el-divider />
        <div class="jd-sources">
          <div class="sources-label">
            <span class="label-icon">📌</span>
            <span class="label-text">信息来源：</span>
          </div>
          <div class="sources-list">
            <div
                v-for="(source, index) in sources"
                :key="index"
                class="source-item"
            >
              <el-link
                  :href="source"
                  target="_blank"
                  type="primary"
                  :underline="false"
                  class="source-link"
              >
                {{ source }}
              </el-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无JD内容" :image-size="60" />
  </div>
</template>

<style scoped>
.jd-content {
  padding: 4px 0;
}

.jd-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.jd-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.jd-body {
  font-size: 14px;
  color: #444;
  line-height: 1.8;
}

.jd-section {
  margin-bottom: 12px;
}

.jd-section:last-child {
  margin-bottom: 0;
}

.jd-text :deep(br) {
  display: block;
  margin: 2px 0;
}

/* 来源信息样式 - 放在内容末尾 */
.jd-sources-wrapper {
  margin-top: 20px;
  padding-top: 8px;
}

.jd-sources {
  padding: 12px 16px;
  background: rgba(100, 163, 134, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(100, 163, 134, 0.12);
}

.sources-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #4a7a64;
  margin-bottom: 8px;
}

.label-icon {
  font-size: 16px;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 4px;
}

.source-item {
  padding: 2px 0;
}

.source-link {
  font-size: 13px;
  color: #64A386;
  word-break: break-all;
}

.source-link:hover {
  color: #4a7a64;
}

/* 响应式 */
@media (max-width: 768px) {
  .jd-sources {
    padding: 10px 12px;
  }

  .source-link {
    font-size: 12px;
  }
}
</style>
<template>
  <div class="editor-page">
    <h1 class="page-title">字幕编辑器</h1>
    <div class="editor-content">
      <a-card title="字幕编辑功能开发中...">
        <p>此页面将包含视频预览和字幕编辑功能</p>
        <a-button type="primary" @click="goBack">返回</a-button>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.push('/upload')
}
</script>

<style scoped>
.editor-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 2rem;
}

.editor-content {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}
</style> 
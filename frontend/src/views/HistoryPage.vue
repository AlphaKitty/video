<template>
  <div class="history-page">
    <h1 class="page-title">历史记录</h1>
    <div class="history-content">
      <a-card title="历史任务">
        <div v-if="videoStore.loading" class="loading-section">
          <a-spin size="large" />
          <div class="loading-text">正在加载...</div>
        </div>
        
        <div v-else-if="videoStore.tasks.length === 0" class="empty-section">
          <div class="empty-icon">📁</div>
          <div class="empty-text">暂无历史记录</div>
          <a-button type="primary" @click="goToUpload">上传视频</a-button>
        </div>
        
        <div v-else class="tasks-list">
          <div 
            v-for="task in videoStore.tasks" 
            :key="task.id"
            class="task-item"
            @click="handleTaskClick(task)"
          >
            <div class="task-info">
              <div class="task-name">{{ task.originalName }}</div>
              <div class="task-time">{{ formatTime(task.createTime) }}</div>
            </div>
            <div class="task-status">
              <a-tag :color="getStatusColor(task.status)">
                {{ getStatusText(task.status) }}
              </a-tag>
            </div>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVideoStore } from '../stores/video'

const router = useRouter()
const videoStore = useVideoStore()

const goToUpload = () => {
  router.push('/upload')
}

const handleTaskClick = (task) => {
  if (task.status === 'PROCESSING') {
    router.push(`/processing/${task.id}`)
  } else if (task.status === 'COMPLETED') {
    router.push(`/editor/${task.id}`)
  }
}

const getStatusColor = (status) => {
  const colors = {
    'UPLOADED': 'blue',
    'PROCESSING': 'orange',
    'COMPLETED': 'green',
    'FAILED': 'red'
  }
  return colors[status] || 'gray'
}

const getStatusText = (status) => {
  const texts = {
    'UPLOADED': '已上传',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return texts[status] || '未知'
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  return new Date(timeString).toLocaleString()
}

onMounted(() => {
  videoStore.getAllTasks()
})
</script>

<style scoped>
.history-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 2rem;
}

.history-content {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

.loading-section, .empty-section {
  padding: 3rem 0;
  text-align: center;
}

.loading-text, .empty-text {
  margin-top: 1rem;
  color: #6b7280;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.tasks-list {
  padding: 1rem;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.task-item:hover {
  background: #f9fafb;
  border-color: #3b82f6;
}

.task-info {
  flex: 1;
}

.task-name {
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.task-time {
  font-size: 0.85rem;
  color: #6b7280;
}
</style> 
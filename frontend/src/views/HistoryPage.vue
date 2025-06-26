<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">处理历史</h1>
      <p class="page-description">查看所有视频处理任务的状态和结果</p>
      
      <div class="header-actions">
        <a-button @click="refreshTasks" :loading="videoStore.loading">
          刷新
        </a-button>
        <a-button type="primary" @click="goToUpload">
          新建任务
        </a-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-number">{{ videoStore.tasks.length }}</div>
        <div class="stat-label">总任务数</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ videoStore.processingTasks.length }}</div>
        <div class="stat-label">处理中</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ videoStore.completedTasks.length }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ failedTasks.length }}</div>
        <div class="stat-label">失败</div>
      </div>
    </div>

    <!-- 筛选和搜索 -->
    <div class="filter-section">
      <div class="filter-controls">
        <a-select 
          v-model="statusFilter" 
          placeholder="按状态筛选"
          style="width: 120px;"
          allow-clear
        >
          <a-option value="">全部</a-option>
          <a-option value="UPLOADED">已上传</a-option>
          <a-option value="PROCESSING">处理中</a-option>
          <a-option value="COMPLETED">已完成</a-option>
          <a-option value="FAILED">失败</a-option>
        </a-select>
        
        <a-input 
          v-model="searchKeyword" 
          placeholder="搜索文件名..."
          style="width: 200px;"
          allow-clear
        >
          <template #prefix>
            <icon-search />
          </template>
        </a-input>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="tasks-section">
      <div v-if="filteredTasks.length === 0" class="empty-state">
        <div class="empty-icon">📁</div>
        <div class="empty-text">暂无任务记录</div>
        <a-button type="primary" @click="goToUpload">
          上传第一个视频
        </a-button>
      </div>
      
      <div v-else class="task-list">
        <div 
          v-for="task in filteredTasks" 
          :key="task.id"
          class="task-item"
          @click="goToTask(task)"
        >
          <div class="task-info">
            <div class="task-header">
              <div class="task-name">{{ task.originalName }}</div>
              <div class="task-time">{{ formatTime(task.createTime) }}</div>
            </div>
            
            <div class="task-meta">
              <span class="task-id">ID: {{ task.id }}</span>
              <span class="task-size" v-if="task.fileSize">
                {{ formatFileSize(task.fileSize) }}
              </span>
            </div>
            
            <div class="task-progress" v-if="task.status === 'PROCESSING'">
              <a-progress 
                :percent="task.progress || 0" 
                size="small"
                :show-text="false"
              />
              <span class="progress-text">{{ task.progress || 0 }}%</span>
            </div>
          </div>
          
          <div class="task-status">
            <a-tag 
              :color="getStatusColor(task.status)"
              class="status-tag"
            >
              {{ getStatusText(task.status) }}
            </a-tag>
            
            <div class="task-actions" @click.stop>
              <a-dropdown>
                <a-button type="text" size="small">
                  <icon-more />
                </a-button>
                <template #content>
                  <a-doption 
                    v-if="task.status === 'PROCESSING'"
                    @click="refreshTask(task.id)"
                  >
                    刷新状态
                  </a-doption>
                  <a-doption 
                    v-if="task.status === 'COMPLETED'"
                    @click="goToEditor(task.id)"
                  >
                    编辑字幕
                  </a-doption>
                  <a-doption 
                    v-if="task.status === 'COMPLETED'"
                    @click="downloadSubtitle(task.id)"
                  >
                    下载字幕
                  </a-doption>
                  <a-doption 
                    v-if="task.status === 'FAILED'"
                    @click="retryTask(task.id)"
                  >
                    重新处理
                  </a-doption>
                  <a-doption 
                    @click="deleteTask(task.id)"
                    class="danger-option"
                  >
                    删除任务
                  </a-doption>
                </template>
              </a-dropdown>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { IconSearch, IconMore } from '@arco-design/web-vue/es/icon'
import { useVideoStore } from '../stores/video'

const router = useRouter()
const videoStore = useVideoStore()

// 响应式数据
const statusFilter = ref('')
const searchKeyword = ref('')

// 计算属性
const failedTasks = computed(() =>
  videoStore.tasks.filter(task => task.status === 'FAILED')
)

const filteredTasks = computed(() => {
  let filtered = videoStore.tasks

  // 状态筛选
  if (statusFilter.value) {
    filtered = filtered.filter(task => task.status === statusFilter.value)
  }

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(task => 
      task.originalName.toLowerCase().includes(keyword)
    )
  }

  // 按创建时间倒序排列
  return filtered.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
})

// 方法
const refreshTasks = async () => {
  try {
    await videoStore.getAllTasks()
    Message.success('任务列表已刷新')
  } catch (error) {
    Message.error('刷新失败: ' + (error.message || '未知错误'))
  }
}

const refreshTask = async (taskId) => {
  try {
    await videoStore.getTask(taskId)
    Message.success('任务状态已刷新')
  } catch (error) {
    Message.error('刷新失败: ' + (error.message || '未知错误'))
  }
}

const goToUpload = () => {
  router.push('/upload')
}

const goToTask = (task) => {
  if (task.status === 'PROCESSING') {
    router.push(`/processing/${task.id}`)
  } else if (task.status === 'COMPLETED') {
    router.push(`/editor/${task.id}`)
  } else if (task.status === 'FAILED') {
    router.push(`/processing/${task.id}`)
  }
}

const goToEditor = (taskId) => {
  router.push(`/editor/${taskId}`)
}

const retryTask = async (taskId) => {
  try {
    await videoStore.processVideo(taskId)
    Message.success('重新处理已开始')
    router.push(`/processing/${taskId}`)
  } catch (error) {
    Message.error('重新处理失败: ' + (error.message || '未知错误'))
  }
}

const downloadSubtitle = (taskId) => {
  // TODO: 实现字幕下载功能
  Message.info('字幕下载功能开发中...')
}

const deleteTask = (taskId) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个任务吗？此操作不可恢复。',
    onOk: async () => {
      try {
        // TODO: 实现删除任务的API
        Message.success('任务已删除')
        await refreshTasks()
      } catch (error) {
        Message.error('删除失败: ' + (error.message || '未知错误'))
      }
    }
  })
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
  
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  } else {
    return date.toLocaleDateString()
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return ''
  
  if (bytes < 1024) {
    return bytes + ' B'
  } else if (bytes < 1048576) {
    return Math.round(bytes / 1024) + ' KB'
  } else if (bytes < 1073741824) {
    return Math.round(bytes / 1048576) + ' MB'
  } else {
    return Math.round(bytes / 1073741824) + ' GB'
  }
}

// 生命周期
onMounted(() => {
  refreshTasks()
})
</script>

<style scoped>
.history-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
}

.page-title {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.page-description {
  color: #6b7280;
  margin: 0.5rem 0 0 0;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border-radius: 0.5rem;
  padding: 1.5rem;
  text-align: center;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

.stat-number {
  font-size: 2rem;
  font-weight: bold;
  color: #3b82f6;
  margin-bottom: 0.5rem;
}

.stat-label {
  color: #6b7280;
  font-size: 0.9rem;
}

.filter-section {
  background: white;
  border-radius: 0.5rem;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
}

.filter-controls {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.tasks-section {
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-text {
  color: #6b7280;
  margin-bottom: 1.5rem;
  font-size: 1.1rem;
}

.task-list {
  divide-y: 1px solid #e5e7eb;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.task-item:hover {
  background: #f9fafb;
}

.task-info {
  flex: 1;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.task-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 1.1rem;
}

.task-time {
  color: #6b7280;
  font-size: 0.9rem;
}

.task-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.task-id {
  color: #6b7280;
  font-size: 0.85rem;
  font-family: monospace;
}

.task-size {
  color: #6b7280;
  font-size: 0.85rem;
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 0.5rem;
}

.progress-text {
  font-size: 0.85rem;
  color: #6b7280;
  white-space: nowrap;
}

.task-status {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-tag {
  font-weight: 500;
}

.task-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.task-item:hover .task-actions {
  opacity: 1;
}

.danger-option {
  color: #ef4444;
}

.danger-option:hover {
  background: #fef2f2;
}
</style> 
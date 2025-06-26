<template>
  <div class="upload-page">
    <!-- 上传区域 -->
    <div class="upload-section">
      <h1 class="section-title">中越双语教学视频转换工具</h1>
      <p class="section-description">
        上传您的教学视频，我们将自动识别中文语音，翻译成越南语，并生成双语字幕
      </p>
      
      <!-- 文件上传区 -->
      <div 
        class="upload-area"
        :class="{ 'dragover': isDragOver }"
        @drop="handleDrop"
        @dragover.prevent="isDragOver = true"
        @dragleave="isDragOver = false"
        @click="triggerFileInput"
      >
        <div class="upload-icon">📹</div>
        <div class="upload-text">
          {{ selectedFile ? selectedFile.name : '点击或拖拽视频文件到此处' }}
        </div>
        <div class="upload-hint">
          支持 MP4, AVI, MOV 格式，最大 500MB
        </div>
        
        <input 
          ref="fileInput"
          type="file"
          accept="video/*"
          @change="handleFileSelect"
          style="display: none"
        />
      </div>
      
      <!-- 上传按钮 -->
      <div class="upload-actions" v-if="selectedFile">
        <a-button 
          type="primary" 
          size="large"
          :loading="videoStore.loading"
          @click="handleUpload"
        >
          开始上传
        </a-button>
        <a-button 
          size="large"
          @click="clearFile"
        >
          重新选择
        </a-button>
      </div>
    </div>
    
    <!-- 最近任务 -->
    <div class="recent-tasks" v-if="videoStore.tasks.length > 0">
      <h2 class="section-title">最近任务</h2>
      <div class="task-grid">
        <div 
          v-for="task in recentTasks" 
          :key="task.id"
          class="task-card"
          @click="goToTask(task)"
        >
          <div class="task-info">
            <div class="task-name">{{ task.originalName }}</div>
            <div class="task-time">{{ formatTime(task.createTime) }}</div>
          </div>
          <div class="task-status">
            <a-tag 
              :color="getStatusColor(task.status)"
              class="status-tag"
            >
              {{ getStatusText(task.status) }}
            </a-tag>
            <div class="task-progress" v-if="task.status === 'PROCESSING'">
              {{ task.progress }}%
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
import { Message } from '@arco-design/web-vue'
import { useVideoStore } from '../stores/video'

const router = useRouter()
const videoStore = useVideoStore()

// 响应式数据
const selectedFile = ref(null)
const isDragOver = ref(false)
const fileInput = ref(null)

// 计算属性
const recentTasks = computed(() => 
  videoStore.tasks.slice(0, 6)
)

// 方法
const triggerFileInput = () => {
  fileInput.value.click()
}

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    validateAndSetFile(file)
  }
}

const handleDrop = (event) => {
  event.preventDefault()
  isDragOver.value = false
  
  const file = event.dataTransfer.files[0]
  if (file) {
    validateAndSetFile(file)
  }
}

const validateAndSetFile = (file) => {
  // 检查文件类型
  if (!file.type.startsWith('video/')) {
    Message.error('请选择视频文件')
    return
  }
  
  // 检查文件大小 (500MB)
  if (file.size > 500 * 1024 * 1024) {
    Message.error('文件大小不能超过 500MB')
    return
  }
  
  selectedFile.value = file
}

const clearFile = () => {
  selectedFile.value = null
  fileInput.value.value = ''
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    Message.error('请先选择文件')
    return
  }
  
  try {
    videoStore.addLog(`开始上传文件: ${selectedFile.value.name}`)
    const task = await videoStore.uploadVideo(selectedFile.value)
    Message.success('上传成功！')
    videoStore.addLog(`文件上传成功，任务ID: ${task.id}`)
    
    // 跳转到处理页面
    router.push(`/processing/${task.id}`)
  } catch (error) {
    videoStore.addLog(`文件上传失败: ${error.message}`)
    Message.error(error.message || '上传失败')
  }
}

const goToTask = (task) => {
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
  
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleDateString()
  }
}

// 生命周期
onMounted(() => {
  videoStore.getAllTasks()
})
</script>

<style scoped>
.upload-page {
  max-width: 800px;
  margin: 0 auto;
}

.upload-section {
  background: white;
  border-radius: 1rem;
  padding: 3rem;
  text-align: center;
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  margin-bottom: 2rem;
}

.section-title {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 1rem;
}

.section-description {
  color: #6b7280;
  font-size: 1.1rem;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 1rem;
  padding: 3rem;
  margin: 2rem 0;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: #3b82f6;
  background: #f9fafb;
}

.upload-area.dragover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.upload-text {
  font-size: 1.2rem;
  color: #374151;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.upload-hint {
  color: #6b7280;
  font-size: 0.9rem;
}

.upload-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 2rem;
}

.recent-tasks {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

.task-grid {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.task-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
}

.task-card:hover {
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

.task-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-tag {
  font-size: 0.8rem;
}

.task-progress {
  font-size: 0.8rem;
  color: #f59e0b;
  font-weight: 500;
}
</style> 
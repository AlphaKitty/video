<template>
  <div class="processing-page">
    <div class="processing-section">
      <h1 class="section-title">视频处理中</h1>
      <p class="section-description" v-if="task">
        正在处理: {{ task.originalName }}
      </p>
      
      <!-- 处理进度 -->
      <div class="progress-container" v-if="task">
        <!-- 总进度条 -->
        <div class="overall-progress">
          <div class="progress-label">
            <span>总体进度</span>
            <span class="progress-percent">{{ task.progress || 0 }}%</span>
          </div>
          <a-progress 
            :percent="task.progress || 0" 
            :status="getProgressStatus(task.status)"
            size="large"
          />
        </div>
        
        <!-- 处理步骤 -->
        <div class="process-steps">
          <div 
            v-for="(step, index) in processSteps" 
            :key="index"
            class="process-step"
            :class="getStepClass(step, task)"
          >
            <div class="step-icon">
              <span v-if="getStepStatus(step, task) === 'completed'">✓</span>
              <span v-else-if="getStepStatus(step, task) === 'active'">{{ index + 1 }}</span>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-description">{{ step.description }}</div>
              <div 
                v-if="getStepStatus(step, task) === 'active'" 
                class="step-current"
              >
                {{ task.currentStep }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 处理完成或失败 -->
      <div class="result-section" v-if="task">
        <div v-if="task.status === 'COMPLETED'" class="success-result">
          <div class="result-icon">🎉</div>
          <div class="result-title">处理完成！</div>
          <div class="result-description">
            视频已成功处理，双语字幕已生成
          </div>
          <div class="result-actions">
            <a-button 
              type="primary" 
              size="large"
              @click="goToEditor"
            >
              编辑字幕
            </a-button>
            <a-button 
              size="large"
              @click="downloadSubtitle"
            >
              下载字幕
            </a-button>
          </div>
        </div>
        
        <div v-else-if="task.status === 'FAILED'" class="error-result">
          <div class="result-icon">❌</div>
          <div class="result-title">处理失败</div>
          <div class="result-description">
            {{ task.errorMessage || '处理过程中发生错误，请重试' }}
          </div>
          <div class="result-actions">
            <a-button 
              type="primary" 
              size="large"
              @click="retryProcess"
            >
              重新处理
            </a-button>
            <a-button 
              size="large"
              @click="goBack"
            >
              返回上传
            </a-button>
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="!task" class="loading-section">
        <a-spin size="large" />
        <div class="loading-text">正在获取任务信息...</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useVideoStore } from '../stores/video'

const route = useRoute()
const router = useRouter()
const videoStore = useVideoStore()

// 响应式数据
const task = computed(() => videoStore.currentTask)

// 处理步骤配置
const processSteps = ref([
  {
    key: 'upload',
    title: '文件上传',
    description: '视频文件已成功上传到服务器'
  },
  {
    key: 'extract',
    title: '音频提取',
    description: '从视频中提取音频用于语音识别'
  },
  {
    key: 'transcribe',
    title: '语音识别',
    description: '使用 Whisper 识别中文语音内容'
  },
  {
    key: 'translate',
    title: '翻译处理',
    description: '将中文翻译成越南语'
  },
  {
    key: 'segment',
    title: '词组拆分',
    description: '对文本进行智能分词处理'
  },
  {
    key: 'generate',
    title: '生成字幕',
    description: '生成双语字幕文件'
  }
])

// 方法
const getProgressStatus = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'normal'
}

const getStepStatus = (step, task) => {
  if (!task) return 'pending'
  
  const progress = task.progress || 0
  
  // 根据进度判断步骤状态
  const stepProgress = {
    'upload': 10,
    'extract': 20,
    'transcribe': 40,
    'translate': 60,
    'segment': 80,
    'generate': 100
  }
  
  if (progress >= stepProgress[step.key]) {
    return 'completed'
  } else if (progress >= (stepProgress[step.key] - 20)) {
    return 'active'
  } else {
    return 'pending'
  }
}

const getStepClass = (step, task) => {
  const status = getStepStatus(step, task)
  return {
    'step-pending': status === 'pending',
    'step-active': status === 'active',
    'step-completed': status === 'completed'
  }
}

const goToEditor = () => {
  router.push(`/editor/${task.value.id}`)
}

const downloadSubtitle = () => {
  // TODO: 实现字幕下载功能
  Message.info('字幕下载功能开发中...')
}

const retryProcess = async () => {
  try {
    await videoStore.processVideo(task.value.id)
    Message.success('重新开始处理')
  } catch (error) {
    Message.error(error.message || '重新处理失败')
  }
}

const goBack = () => {
  router.push('/upload')
}

const startProcessing = async () => {
  try {
    await videoStore.processVideo(route.params.id)
  } catch (error) {
    Message.error(error.message || '开始处理失败')
  }
}

// 生命周期
onMounted(async () => {
  const taskId = parseInt(route.params.id)
  
  try {
    // 获取任务信息
    await videoStore.getTask(taskId)
    
    // 如果任务状态是已上传，开始处理
    if (task.value && task.value.status === 'UPLOADED') {
      await startProcessing()
    }
    
    // 如果任务正在处理中，开始轮询
    if (task.value && task.value.status === 'PROCESSING') {
      videoStore.startPolling(taskId)
    }
  } catch (error) {
    Message.error('获取任务信息失败')
    router.push('/upload')
  }
})

onUnmounted(() => {
  // 清理轮询
  videoStore.stopPolling()
})
</script>

<style scoped>
.processing-page {
  max-width: 800px;
  margin: 0 auto;
}

.processing-section {
  background: white;
  border-radius: 1rem;
  padding: 3rem;
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  text-align: center;
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
}

.progress-container {
  margin: 2rem 0;
}

.overall-progress {
  margin-bottom: 3rem;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  font-weight: 500;
}

.progress-percent {
  color: #3b82f6;
  font-size: 1.1rem;
}

.process-steps {
  text-align: left;
}

.process-step {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  transition: all 0.3s;
}

.step-pending {
  background: #f9fafb;
}

.step-active {
  background: #eff6ff;
  border: 1px solid #3b82f6;
}

.step-completed {
  background: #f0fdf4;
  border: 1px solid #10b981;
}

.step-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.step-pending .step-icon {
  background: #e5e7eb;
  color: #6b7280;
}

.step-active .step-icon {
  background: #3b82f6;
  color: white;
}

.step-completed .step-icon {
  background: #10b981;
  color: white;
}

.step-content {
  flex: 1;
}

.step-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.step-description {
  color: #6b7280;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.step-current {
  color: #3b82f6;
  font-size: 0.85rem;
  font-weight: 500;
}

.result-section {
  margin-top: 2rem;
}

.success-result, .error-result {
  padding: 2rem;
  border-radius: 1rem;
}

.success-result {
  background: #f0fdf4;
  border: 1px solid #10b981;
}

.error-result {
  background: #fef2f2;
  border: 1px solid #ef4444;
}

.result-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.result-title {
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.success-result .result-title {
  color: #059669;
}

.error-result .result-title {
  color: #dc2626;
}

.result-description {
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.result-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.loading-section {
  padding: 3rem 0;
}

.loading-text {
  margin-top: 1rem;
  color: #6b7280;
}
</style> 
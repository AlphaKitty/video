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

        <!-- 调试和测试区域 -->
        <div class="debug-section" v-if="showDebug">
          <div class="debug-header">
            <h3>调试工具</h3>
            <a-button size="small" @click="showDebug = false">隐藏</a-button>
          </div>
          
          <div class="debug-actions">
            <a-button 
              size="small" 
              @click="handleTestExtractAudio"
              :loading="debugLoading.extract"
            >
              测试音频提取
            </a-button>
            <a-button 
              size="small" 
              @click="handleTestTranscribe"
              :loading="debugLoading.transcribe"
            >
              测试语音识别
            </a-button>
            <a-button 
              size="small" 
              @click="handleCheckStatus"
              :loading="debugLoading.status"
            >
              检查服务状态
            </a-button>
            <a-button 
              size="small" 
              @click="handleRefreshTask"
              :loading="debugLoading.refresh"
            >
              刷新任务状态
            </a-button>
          </div>

          <!-- 日志显示 -->
          <div class="logs-section">
            <div class="logs-header">
              <span>处理日志</span>
              <a-button size="mini" @click="videoStore.clearLogs()">清空</a-button>
            </div>
            <div class="logs-container">
              <div 
                v-for="(log, index) in videoStore.processingLogs" 
                :key="index"
                class="log-item"
                :class="getLogClass(log)"
              >
                {{ log }}
              </div>
              <div v-if="videoStore.processingLogs.length === 0" class="no-logs">
                暂无日志信息
              </div>
            </div>
          </div>
        </div>

        <!-- 显示调试按钮 -->
        <div class="debug-toggle" v-if="!showDebug">
          <a-button type="text" size="small" @click="showDebug = true">
            显示调试工具
          </a-button>
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
const showDebug = ref(false)
const debugLoading = ref({
  extract: false,
  transcribe: false,
  status: false,
  refresh: false
})

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

// 调试方法
const handleTestExtractAudio = async () => {
  debugLoading.value.extract = true
  try {
    const result = await videoStore.testExtractAudio(task.value.id)
    Message.success('音频提取测试完成')
  } catch (error) {
    Message.error(error.message || '音频提取测试失败')
  } finally {
    debugLoading.value.extract = false
  }
}

const handleTestTranscribe = async () => {
  debugLoading.value.transcribe = true
  try {
    const result = await videoStore.testTranscribe(task.value.id)
    Message.success('语音识别测试完成')
  } catch (error) {
    Message.error(error.message || '语音识别测试失败')
  } finally {
    debugLoading.value.transcribe = false
  }
}

const handleCheckStatus = async () => {
  debugLoading.value.status = true
  try {
    const status = await videoStore.checkServiceStatus()
    Message.success('服务状态检查完成')
  } catch (error) {
    Message.error(error.message || '服务状态检查失败')
  } finally {
    debugLoading.value.status = false
  }
}

const handleRefreshTask = async () => {
  debugLoading.value.refresh = true
  try {
    await videoStore.getTask(task.value.id)
    Message.success('任务状态已刷新')
  } catch (error) {
    Message.error(error.message || '刷新任务状态失败')
  } finally {
    debugLoading.value.refresh = false
  }
}

// 原有方法
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

const getLogClass = (log) => {
  if (log.includes('成功') || log.includes('完成')) {
    return 'log-success'
  } else if (log.includes('失败') || log.includes('错误')) {
    return 'log-error'
  } else if (log.includes('开始') || log.includes('测试')) {
    return 'log-info'
  }
  return 'log-default'
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
  
  // 清空之前的日志
  videoStore.clearLogs()
  videoStore.addLog(`开始处理任务 ID: ${taskId}`)
  
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
    videoStore.addLog(`获取任务信息失败: ${error.message}`)
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
  max-width: 900px;
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
  margin-bottom: 2rem;
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

/* 调试区域样式 */
.debug-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  padding: 1.5rem;
  margin-top: 2rem;
  text-align: left;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.debug-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 1.1rem;
}

.debug-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.debug-toggle {
  margin-top: 1rem;
}

/* 日志区域样式 */
.logs-section {
  background: #1a1a1a;
  border-radius: 0.5rem;
  padding: 1rem;
  color: white;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  color: #9ca3af;
  font-size: 0.9rem;
}

.logs-container {
  max-height: 200px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 0.8rem;
  line-height: 1.4;
}

.log-item {
  padding: 0.25rem 0;
  border-bottom: 1px solid #374151;
}

.log-item:last-child {
  border-bottom: none;
}

.log-success {
  color: #10b981;
}

.log-error {
  color: #ef4444;
}

.log-info {
  color: #3b82f6;
}

.log-default {
  color: #e5e7eb;
}

.no-logs {
  color: #6b7280;
  text-align: center;
  padding: 1rem;
  font-style: italic;
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
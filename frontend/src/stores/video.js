import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useVideoStore = defineStore('video', () => {
  // 状态
  const tasks = ref([])
  const currentTask = ref(null)
  const loading = ref(false)
  
  // 计算属性
  const completedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'COMPLETED')
  )
  
  const processingTasks = computed(() => 
    tasks.value.filter(task => task.status === 'PROCESSING')
  )
  
  // API 基础配置
  const API_BASE = 'http://localhost:8080/api'
  
  // 方法
  const uploadVideo = async (file) => {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      
      const response = await fetch(`${API_BASE}/video/upload`, {
        method: 'POST',
        body: formData
      })
      
      const result = await response.json()
      
      if (result.code === 0) {
        tasks.value.unshift(result.data)
        currentTask.value = result.data
        return result.data
      } else {
        throw new Error(result.msg)
      }
    } catch (error) {
      console.error('上传失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  const processVideo = async (taskId) => {
    try {
      const response = await fetch(`${API_BASE}/video/process/${taskId}`, {
        method: 'POST'
      })
      
      const result = await response.json()
      
      if (result.code !== 0) {
        throw new Error(result.msg)
      }
      
      // 开始轮询任务状态
      startPolling(taskId)
    } catch (error) {
      console.error('处理失败:', error)
      throw error
    }
  }
  
  const getTask = async (taskId) => {
    try {
      const response = await fetch(`${API_BASE}/video/task/${taskId}`)
      const result = await response.json()
      
      if (result.code === 0) {
        // 更新任务列表中的对应任务
        const index = tasks.value.findIndex(task => task.id === taskId)
        if (index !== -1) {
          tasks.value[index] = result.data
        }
        
        if (currentTask.value && currentTask.value.id === taskId) {
          currentTask.value = result.data
        }
        
        return result.data
      } else {
        throw new Error(result.msg)
      }
    } catch (error) {
      console.error('获取任务失败:', error)
      throw error
    }
  }
  
  const getAllTasks = async () => {
    loading.value = true
    try {
      const response = await fetch(`${API_BASE}/video/tasks`)
      const result = await response.json()
      
      if (result.code === 0) {
        tasks.value = result.data
      } else {
        throw new Error(result.msg)
      }
    } catch (error) {
      console.error('获取任务列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  const updateSubtitle = async (taskId, subtitleContent) => {
    try {
      const response = await fetch(`${API_BASE}/video/subtitle/${taskId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(subtitleContent)
      })
      
      const result = await response.json()
      
      if (result.code !== 0) {
        throw new Error(result.msg)
      }
      
      // 重新获取任务信息
      await getTask(taskId)
    } catch (error) {
      console.error('更新字幕失败:', error)
      throw error
    }
  }
  
  // 轮询任务状态
  let pollingTimer = null
  
  const startPolling = (taskId) => {
    if (pollingTimer) {
      clearInterval(pollingTimer)
    }
    
    pollingTimer = setInterval(async () => {
      try {
        const task = await getTask(taskId)
        
        // 如果任务完成或失败，停止轮询
        if (task.status === 'COMPLETED' || task.status === 'FAILED') {
          stopPolling()
        }
      } catch (error) {
        console.error('轮询任务状态失败:', error)
        stopPolling()
      }
    }, 2000) // 每2秒查询一次
  }
  
  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }
  
  const setCurrentTask = (task) => {
    currentTask.value = task
  }
  
  return {
    // 状态
    tasks,
    currentTask,
    loading,
    
    // 计算属性
    completedTasks,
    processingTasks,
    
    // 方法
    uploadVideo,
    processVideo,
    getTask,
    getAllTasks,
    updateSubtitle,
    startPolling,
    stopPolling,
    setCurrentTask
  }
}) 
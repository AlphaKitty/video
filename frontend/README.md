# 中越双语教学视频转换工具 - 前端

基于 Vue 3 + ArcoDesign + Pinia 构建的现代化视频处理前端应用。

## 🚀 功能特性

### 📹 视频处理流程
- **文件上传**: 支持拖拽上传，实时进度显示
- **音频提取**: 自动从视频中提取音频文件
- **语音识别**: 使用 Whisper API 识别中文语音
- **智能翻译**: 中文转越南语翻译
- **字幕生成**: 生成双语字幕文件

### 🛠️ 调试工具
- **实时日志**: 查看处理过程的详细日志
- **状态监控**: 实时监控任务处理状态
- **测试接口**: 独立测试音频提取和语音识别
- **服务状态**: 检查后端服务可用性

### 📊 任务管理
- **历史记录**: 查看所有处理任务
- **状态筛选**: 按处理状态筛选任务
- **搜索功能**: 按文件名搜索任务
- **批量操作**: 重试、删除、下载等操作

## 🎯 页面说明

### 1. 上传页面 (`/upload`)
- 支持点击或拖拽上传视频文件
- 文件格式验证（MP4, AVI, MOV）
- 文件大小限制（最大500MB）
- 显示最近任务快速访问

### 2. 处理页面 (`/processing/:id`)
- 实时进度条显示
- 分步骤处理状态展示
- 调试工具面板
- 实时日志输出
- 测试功能按钮

### 3. 编辑页面 (`/editor/:id`)
- 字幕编辑和预览
- 双语字幕对照
- 时间轴调整
- 导出功能

### 4. 历史页面 (`/history`)
- 任务列表展示
- 状态统计面板
- 搜索和筛选
- 任务操作菜单

## 🔧 技术栈

- **框架**: Vue 3 (Composition API)
- **UI库**: ArcoDesign
- **状态管理**: Pinia
- **路由**: Vue Router
- **样式**: Tailwind CSS + Scoped CSS
- **构建工具**: Vite

## 📝 API接口

### 核心接口
- `POST /api/video/upload` - 上传视频文件
- `POST /api/video/process/{id}` - 开始处理任务
- `GET /api/video/task/{id}` - 获取任务详情
- `GET /api/video/tasks` - 获取任务列表

### 调试接口
- `POST /api/video/test/extract-audio/{id}` - 测试音频提取
- `POST /api/video/test/transcribe/{id}` - 测试语音识别
- `GET /api/video/status` - 检查服务状态

## 🚀 快速开始

### 安装依赖
```bash
npm install
```

### 开发环境
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 预览生产版本
```bash
npm run preview
```

## 🔍 使用说明

### 1. 上传视频
1. 访问上传页面
2. 点击或拖拽上传视频文件
3. 确认文件信息后点击"开始上传"
4. 自动跳转到处理页面

### 2. 监控处理
1. 在处理页面查看实时进度
2. 点击"显示调试工具"查看详细信息
3. 使用测试按钮验证各个处理步骤
4. 查看实时日志了解处理状态

### 3. 调试功能
- **测试音频提取**: 验证FFmpeg是否正常工作
- **测试语音识别**: 验证Whisper API是否可用
- **检查服务状态**: 检查所有服务组件状态
- **刷新任务状态**: 手动更新任务信息

### 4. 历史管理
1. 在历史页面查看所有任务
2. 使用筛选器快速找到特定任务
3. 通过操作菜单进行任务管理
4. 查看统计信息了解整体状态

## 🛠️ 开发指南

### 状态管理 (Pinia Store)
```javascript
// 获取video store
const videoStore = useVideoStore()

// 上传文件
await videoStore.uploadVideo(file)

// 处理视频
await videoStore.processVideo(taskId)

// 测试功能
await videoStore.testExtractAudio(taskId)
await videoStore.testTranscribe(taskId)
```

### API调用示例
```javascript
// 检查服务状态
const status = await videoStore.checkServiceStatus()

// 添加日志
videoStore.addLog('处理开始')

// 清空日志
videoStore.clearLogs()
```

## 🔗 相关链接

- [后端API文档](../backend/README.md)
- [项目开发路线图](../DEVELOPMENT_ROADMAP.md)
- [ArcoDesign文档](https://arco.design/vue)
- [Vue 3文档](https://v3.vuejs.org/)

## 📞 问题反馈

如有问题或建议，请在项目Issues中提出。

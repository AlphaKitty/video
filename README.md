# 中越双语教学视频转换工具

基于 Vue 3 + Spring Boot 的视频处理工具，集成 Whisper 语音识别和 GPT 翻译功能。

## 项目结构

```
video/
├── frontend/           # Vue 3 前端应用
│   ├── src/
│   │   ├── views/      # 页面组件
│   │   ├── stores/     # Pinia 状态管理
│   │   └── ...
├── backend/            # Spring Boot 后端服务
│   ├── src/main/java/com/video/
│   │   ├── controller/ # REST API 控制器
│   │   ├── service/    # 业务逻辑服务
│   │   ├── model/      # 数据模型
│   │   └── ...
└── video_transfer_prototype.html  # 原型文件
```

## 功能特性

### 已实现功能
- ✅ 前端 Vue 3 + ArcoDesign + Pinia 基础架构
- ✅ 后端 Spring Boot 基础架构
- ✅ 视频上传页面
- ✅ 处理进度页面
- ✅ 历史记录页面
- ✅ 基础 API 接口定义

### 开发中功能
- 🔄 后端 Lombok 编译问题修复
- 🔄 Whisper 语音识别集成
- 🔄 GPT 翻译服务集成
- 🔄 字幕编辑器
- 🔄 视频预览功能

### 计划功能
- 📋 文件下载功能
- 📋 设置页面完善
- 📋 用户认证
- 📋 批量处理

## 开发环境启动

### 前端启动
```bash
cd frontend
npm install
npm run dev
```
前端将在 http://localhost:5173 运行

### 后端启动
```bash
cd backend
# 注意：当前后端存在 Lombok 编译问题，需要修复
mvn clean compile
mvn spring-boot:run
```
后端将在 http://localhost:8080 运行

## API 接口

### 视频处理接口
- `POST /api/video/upload` - 上传视频文件
- `POST /api/video/process/{id}` - 开始处理视频
- `GET /api/video/task/{id}` - 获取任务状态
- `GET /api/video/tasks` - 获取所有任务
- `POST /api/video/subtitle/{id}` - 更新字幕内容

## 技术栈

### 前端
- Vue 3 + Composition API
- ArcoDesign Vue 组件库
- Pinia 状态管理
- Vue Router 路由
- Vite 构建工具

### 后端
- Spring Boot 3.2.1
- Spring Web
- Spring WebFlux (用于 AI API 调用)
- Lombok
- Maven

### AI 服务
- Whisper API (语音识别)
- OpenAI GPT API (翻译和分词)

## 当前状态

项目基础架构已搭建完成，前端可以正常运行并显示各个页面。后端存在 Lombok 注解编译问题需要修复。

下一步开发重点：
1. 修复后端编译问题
2. 完善 AI 服务集成
3. 实现字幕编辑功能
4. 完善错误处理和用户体验

## 开发说明

- 前端使用现代化的 Vue 3 Composition API
- 后端采用分层架构设计
- API 遵循 RESTful 规范
- 支持文件上传和实时进度追踪
- 响应式设计，支持移动端访问 
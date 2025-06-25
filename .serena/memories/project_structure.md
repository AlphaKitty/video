# 项目结构（最新版本）

## 根目录结构
```
video/
├── frontend/                    # Vue 3 前端工程
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   │   ├── UploadPage.vue
│   │   │   ├── ProcessingPage.vue
│   │   │   ├── EditorPage.vue
│   │   │   ├── HistoryPage.vue
│   │   │   └── SettingsPage.vue
│   │   ├── stores/             # Pinia状态管理
│   │   │   └── video.js
│   │   ├── components/         # 公共组件
│   │   ├── assets/            # 静态资源
│   │   ├── App.vue            # 主应用组件
│   │   └── main.js            # 入口文件
│   ├── public/                # 公共资源
│   └── package.json           # 前端依赖
├── backend/                   # Spring Boot 后端工程
│   ├── src/main/java/com/video/
│   │   ├── controller/        # REST控制器
│   │   ├── service/          # 业务逻辑层
│   │   ├── model/            # 数据模型
│   │   ├── common/           # 公共类
│   │   └── config/           # 配置类
│   ├── src/main/resources/
│   │   └── application.yml   # 应用配置
│   └── pom.xml              # Maven依赖
├── video_transfer_prototype.html # 原型文件
└── README.md                # 项目说明
```

## 关键文件说明
- **frontend/src/App.vue**: 主应用布局和路由
- **frontend/src/stores/video.js**: 视频处理状态管理
- **backend/src/main/java/com/video/controller/VideoController.java**: 主要API接口
- **backend/src/main/resources/application.yml**: 后端配置（端口8080）
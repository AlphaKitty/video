常用开发命令（已修正路径）：
- 前端：
  - cd frontend && npm install
  - npm run dev
  - npm run build
- 后端：
  - cd backend && mvn clean package
  - mvn exec:java -Dexec.mainClass="com.video.App"
- 目录切换、文件操作：cd、ls、cat、grep、find 等（Darwin/macOS 环境）
- Git 操作：git status、git add、git commit、git push

后续集成 Spring Boot、Whisper、GPT 时需补充相关命令。
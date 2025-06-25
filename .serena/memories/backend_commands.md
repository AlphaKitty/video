后端开发常用命令（已修正路径）：
- 构建项目：cd backend && mvn clean package
- 运行主类：cd backend && mvn exec:java -Dexec.mainClass="com.video.App"

后续集成 Spring Boot 后，推荐使用 Spring Boot 启动命令。
代码风格：推荐使用统一响应结构、Lombok、MyBatis-Plus、分层（Controller/Service/Mapper/Model）。
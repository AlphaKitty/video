每次开发任务完成后：
- 前端：建议运行 npm run lint、npm run build 检查和构建
- 后端：建议运行 mvn clean package、mvn test
- 代码需自测通过，风格规范
- 如涉及数据库，需同步更新 init.sql 和 update-xx.sql
- 重要变更建议 git commit 记录
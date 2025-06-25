<script setup>
import { ref } from 'vue'

const navItems = ref([
  { path: '/upload', title: '上传视频' },
  { path: '/history', title: '历史记录' },
  { path: '/settings', title: '设置' }
])
</script>

<template>
  <div id="app">
    <!-- 头部导航 -->
    <header class="header">
      <div class="container">
        <div class="header-content">
          <!-- Logo -->
          <div class="logo">
            <div class="logo-icon">V</div>
            视频转换工具
          </div>
          
          <!-- 导航菜单 -->
          <nav class="nav">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{ active: $route.path === item.path || $route.path.startsWith(item.path + '/') }"
            >
              {{ item.title }}
            </router-link>
          </nav>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main">
      <div class="container">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.5rem;
  font-weight: bold;
  color: #3b82f6;
}

.logo-icon {
  width: 2rem;
  height: 2rem;
  background: #3b82f6;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
}

.nav {
  display: flex;
  gap: 2rem;
}

.nav-item {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  text-decoration: none;
  color: #374151;
}

.nav-item:hover {
  background: #f3f4f6;
}

.nav-item.active {
  background: #3b82f6;
  color: white;
}

.main {
  min-height: calc(100vh - 80px);
  padding: 2rem 0;
  background: #f9fafb;
}
</style>

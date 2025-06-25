import './assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ArcoVue from '@arco-design/web-vue'
import App from './App.vue'
import '@arco-design/web-vue/dist/arco.css'

// 导入页面组件
import UploadPage from './views/UploadPage.vue'
import ProcessingPage from './views/ProcessingPage.vue'
import EditorPage from './views/EditorPage.vue'
import HistoryPage from './views/HistoryPage.vue'
import SettingsPage from './views/SettingsPage.vue'

// 路由配置
const routes = [
  { path: '/', redirect: '/upload' },
  { path: '/upload', component: UploadPage, meta: { title: '上传视频' } },
  { path: '/processing/:id', component: ProcessingPage, meta: { title: '处理中' } },
  { path: '/editor/:id', component: EditorPage, meta: { title: '编辑字幕' } },
  { path: '/history', component: HistoryPage, meta: { title: '历史记录' } },
  { path: '/settings', component: SettingsPage, meta: { title: '设置' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ArcoVue)

app.mount('#app')

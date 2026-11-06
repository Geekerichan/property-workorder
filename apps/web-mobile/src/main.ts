// 应用入口：装配 Pinia、路由与 Vant，挂载根组件
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import '@fontsource/noto-serif-sc/500.css'
import '@fontsource/noto-serif-sc/600.css'
import '@fontsource/jetbrains-mono/400.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(router).use(Vant).mount('#app')

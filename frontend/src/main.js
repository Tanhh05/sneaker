import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import vi from 'element-plus/es/locale/lang/vi'
import 'element-plus/dist/index.css'
import '@/assets/styles/main.scss'

import App from './App.vue'
import router from './router'
import QuickSelect from '@/components/common/QuickSelect.vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: vi })
app.component('QuickSelect', QuickSelect)

app.mount('#app')


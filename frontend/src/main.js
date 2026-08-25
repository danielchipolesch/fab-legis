import { createApp } from 'vue'
import { Quasar, Notify, Dialog } from 'quasar'
import iconSet from 'quasar/icon-set/mdi-v7'
import langPtBR from 'quasar/lang/pt-BR'
import '@quasar/extras/mdi-v7/mdi-v7.css'
import 'quasar/src/css/index.sass'
import './css/app.css'
import App from './App.vue'
import router from './router/index.js'
import { createPinia } from 'pinia'
import { useAuthStore } from './stores/auth.js'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
useAuthStore().inicializar()
app.use(router)
app.use(Quasar, {
  iconSet,
  lang: langPtBR,
  plugins: { Notify, Dialog },
  config: {
    brand: {
      primary: '#0B3D91',
      secondary: '#1565C0',
      accent: '#42A5F5',
      dark: '#1D1D1D',
      positive: '#21BA45',
      negative: '#C10015',
      info: '#31CCEC',
      warning: '#F2C037',
    },
    notify: {
      position: 'bottom-right',
      timeout: 4000,
    },
  },
})

app.mount('#app')

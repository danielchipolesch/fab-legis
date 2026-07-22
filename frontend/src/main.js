import { createApp } from 'vue'
import { Quasar, Notify, Dialog } from 'quasar'
import '@quasar/extras/mdi-v7/mdi-v7.css'
import 'quasar/src/css/index.sass'
import './css/app.css'
import App from './App.vue'
import router from './router/index.js'
import { createPinia } from 'pinia'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(Quasar, {
  plugins: { Notify, Dialog },
  config: {
    brand: {
      primary: '#1A2E5A',
      secondary: '#4A6FA5',
      accent: '#6D8CC4',
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

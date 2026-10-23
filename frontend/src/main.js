import { createApp } from 'vue'
<<<<<<< HEAD
import { createPinia } from 'pinia'
=======
>>>>>>> bloc2-bis
import App from './App.vue'
import router from './router'
import './assets/main.css'

<<<<<<< HEAD
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
=======
createApp(App).use(router).mount('#app')
>>>>>>> bloc2-bis

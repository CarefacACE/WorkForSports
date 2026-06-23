import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import './styles/tailwind.css';
import 'element-plus/dist/index.css';
import './styles/nike-theme.css';
import './styles/views/login.css';
import App from './App.vue';
import router from './router';
import './style.css';

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
  .mount('#app');

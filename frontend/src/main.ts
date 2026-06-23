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

// Initialize admin theme from localStorage
const savedTheme = localStorage.getItem('admin-theme') || 'light';
document.documentElement.setAttribute('data-admin-theme', savedTheme);

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus)
  .mount('#app');

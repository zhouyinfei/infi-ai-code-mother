import { createRouter, createWebHistory } from 'vue-router'

import HomePage from "@/pages/HomePage.vue";
import UserLoginPage from "@/pages/user/UserLoginPage.vue";
import UserRegisterPage from "@/pages/user/UserRegisterPage.vue";
import UserManagePage from "@/pages/admin/UserManagePage.vue";
import AppManagePage from "@/pages/admin/AppManagePage.vue";
import AppChatPage from "@/pages/app/AppChatPage.vue";
import AppUpdatePage from "@/pages/app/AppUpdatePage.vue";


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
    },
    {
      path: '/app/chat/:id',
      name: '应用生成对话',
      component: AppChatPage,
    },
    {
      path: '/app/update/:id',
      name: '应用信息修改',
      component: AppUpdatePage,
    },
  ],
})

export default router

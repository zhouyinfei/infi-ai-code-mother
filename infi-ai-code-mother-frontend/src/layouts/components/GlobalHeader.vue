<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {Layout, Menu, Button, Space, message} from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import logo from '@/assets/logo.ico'
import type { MenuItem } from '../types'
import {useLoginUserStore} from "@/stores/loginUser.ts";

import { LogoutOutlined } from '@ant-design/icons-vue'
import {userLogout} from "@/api/userController.ts";

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}


// 使用登录用户信息存储
const loginUserStore = useLoginUserStore();

const { Header } = Layout

const props = withDefaults(
  defineProps<{
    title?: string
    menus?: MenuItem[]
  }>(),
  {
    title: '水豚应用生成',
    menus: () => [
      { key: 'home', label: '首页', path: '/' },
      { key: '应用管理', label: '应用管理', path: '/admin/appManage' },
      { key: '用户管理', label: '用户管理', path: '/admin/userManage' },
    ],
  },
)

const route = useRoute()
const router = useRouter()

// 过滤菜单项：非 admin 用户隐藏 /admin 路径的菜单
const filterMenus = (menus: typeof props.menus) => {
  return menus.filter((menu) => {
    if (menu.path?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser?.id || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 实际渲染的菜单项
const menuItems = computed(() => filterMenus(props.menus))

// 根据当前路由路径高亮菜单项
const selectedKeys = computed(() => {
  const matched = menuItems.value.find((m) => route.path === m.path || route.path.startsWith(m.path + '/'))
  return matched ? [matched.key] : []
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  const target = props.menus.find((m) => m.key === key)
  if (target) {
    router.push(target.path)
  }
}

const handleLogin = () => {
  router.push('/user/login')
}
</script>

<template>
  <Header class="global-header">
    <div class="header-left">
      <img :src="logo" class="header-logo" alt="logo" />
      <span class="header-title">{{ title }}</span>
    </div>
    <Menu
      class="header-menu"
      theme="light"
      mode="horizontal"
      :selected-keys="selectedKeys"
      :items="menuItems.map((m) => ({ key: m.key, label: m.label }))"
      @click="handleMenuClick"
    />
    <div class="header-right">
      <Space>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" @click="handleLogin">登录</a-button>
          </div>
        </div>
      </Space>
    </div>
  </Header>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  line-height: 64px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.header-logo {
  width: 36px;
  height: 36px;
  border-radius: 6px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1677ff;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none !important;
  margin: 0 24px;
}

.header-right {
  flex-shrink: 0;
  margin-left: auto;
}
</style>

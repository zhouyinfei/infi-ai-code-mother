<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Layout, Menu, Button, Space } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import logo from '@/assets/logo.ico'
import type { MenuItem } from '../types'

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
      { key: 'code', label: '代码生成', path: '/code' },
      { key: 'about', label: '关于', path: '/about' },
    ],
  },
)

const route = useRoute()
const router = useRouter()

// 根据当前路由路径高亮菜单项
const selectedKeys = computed(() => {
  const matched = props.menus.find((m) => route.path === m.path || route.path.startsWith(m.path + '/'))
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
      :items="menus.map((m) => ({ key: m.key, label: m.label }))"
      @click="handleMenuClick"
    />
    <div class="header-right">
      <Space>
        <Button type="primary" @click="handleLogin">登录</Button>
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

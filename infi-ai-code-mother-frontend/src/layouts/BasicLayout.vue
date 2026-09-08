<script setup lang="ts">
import { Layout } from 'ant-design-vue'
import GlobalHeader from './components/GlobalHeader.vue'
import GlobalFooter from './components/GlobalFooter.vue'
import type { MenuItem } from './types'

const { Content } = Layout

// 菜单项配置，可通过 props 外部传入覆盖
const props = withDefaults(
  defineProps<{
    title?: string
    menus?: MenuItem[]
  }>(),
  {
    title: '水豚应用生成',
    menus: () => [
      { key: 'home', label: '首页', path: '/' },
      { key: '用户管理', label: '用户管理', path: '/admin/userManage' },
      { key: 'code', label: '代码生成', path: '/code' },
    ],
  },
)
</script>

<template>
  <Layout class="basic-layout">
    <GlobalHeader :title="title" :menus="menus" />
    <Content class="basic-content">
      <router-view />
    </Content>
    <GlobalFooter />
  </Layout>
</template>

<style scoped>
.basic-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.basic-content {
  flex: 1;
  padding: 24px;
  background: #f5f5f5;
}

/* 响应式：小屏幕下减少内边距 */
@media (max-width: 768px) {
  .basic-content {
    padding: 12px;
  }
}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

import { useLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const loginUserStore = useLoginUserStore()
// 尝试获取登录用户信息，失败则保持未登录状态
loginUserStore.fetchLoginUser().catch(() => {
  // 后端未启动或请求失败时静默处理
})

// 登录/注册页面使用独立布局，不包裹 BasicLayout
const standalonePages = ['/user/login', '/user/register']
const isStandalone = computed(() => standalonePages.includes(route.path))
</script>

<template>
  <router-view v-if="isStandalone" />
  <BasicLayout v-else />
</template>

<style scoped></style>

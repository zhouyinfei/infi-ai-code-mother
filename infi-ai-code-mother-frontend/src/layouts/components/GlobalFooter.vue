<script setup lang="ts">
import { Layout } from 'ant-design-vue'

const { Footer } = Layout
</script>

<template>
  <Footer class="global-footer">
    水豚应用生成原创项目 by 程序员infi
  </Footer>
</template>

<style scoped>
.global-footer {
  text-align: center;
  color: #999;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  padding: 16px 0;
  flex-shrink: 0;
}
</style>

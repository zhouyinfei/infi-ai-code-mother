<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  CodeOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import logo from '@/assets/logo.png'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 每页数量（后端限制最多 20 个）
const PAGE_SIZE = 12

/**
 * 将时间格式化为「x 前」的相对时间
 */
const formatRelativeTime = (time?: string) => {
  if (!time) return ''
  const diff = Date.now() - new Date(time).getTime()
  const minute = Math.floor(diff / 60000)
  if (minute < 1) return '刚刚'
  if (minute < 60) return `${minute} 分钟前`
  const hour = Math.floor(minute / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 7) return `${day} 天前`
  const week = Math.floor(day / 7)
  if (week < 5) return `${week} 周前`
  const month = Math.floor(day / 30)
  if (month < 12) return `${month} 个月前`
  return `${Math.floor(month / 12)} 年前`
}

// ==================== 提示词创建应用 ====================

const prompt = ref('')
const creating = ref(false)

// 推荐提示词：label 为按钮文字，prompt 为点击后填入的详细提示词
const suggestions = [
  { label: '波普风电商页面', prompt: '帮我创建一个波普风格的电商首页，包含商品展示、购物车图标和促销横幅' },
  { label: '企业网站', prompt: '帮我创建一个现代企业官网，包含公司介绍、团队展示、服务项目和联系方式' },
  { label: '电商运营后台', prompt: '帮我创建一个电商运营后台，包含数据看板、订单管理、商品管理和用户分析' },
  { label: '暗黑话题社区', prompt: '帮我创建一个暗黑风格的话题社区，包含话题列表、发布框、评论区和用户头像' },
]

const handleSuggestionClick = (item: { label: string; prompt: string }) => {
  prompt.value = item.prompt
}

// 输入提示词创建应用
const handleCreate = async () => {
  if (!prompt.value.trim()) {
    message.warning('请输入提示词')
    return
  }
  // 未登录则跳转到登录页
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后再创建应用')
    router.push(`/user/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt: prompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功，正在跳转...')
      // 跳转到对话页，并自动发送初始提示词
      router.push(`/app/chat/${appId}?initPrompt=${encodeURIComponent(prompt.value.trim())}`)
    } else {
      message.error('创建应用失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('创建应用失败，请检查网络或登录状态')
  } finally {
    creating.value = false
  }
}

// ==================== 我的应用列表 ====================

const myApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const myLoading = ref(false)
const myParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: PAGE_SIZE,
  appName: '',
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchMyApps = async () => {
  myLoading.value = true
  try {
    const res = await listMyAppVoByPage({ ...myParams })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('获取我的应用失败，请检查网络或登录状态')
  } finally {
    myLoading.value = false
  }
}

const handleMySearch = () => {
  myParams.pageNum = 1
  fetchMyApps()
}

const handleMyPageChange = (page: number) => {
  myParams.pageNum = page
  fetchMyApps()
}

// ==================== 精选应用列表 ====================

const goodApps = ref<API.AppVO[]>([])
const goodTotal = ref(0)
const goodLoading = ref(false)
const goodParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: PAGE_SIZE,
  appName: '',
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchGoodApps = async () => {
  goodLoading.value = true
  try {
    const res = await listGoodAppVoByPage({ ...goodParams })
    if (res.data.code === 0 && res.data.data) {
      goodApps.value = res.data.data.records ?? []
      goodTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('获取精选应用失败，请检查网络')
  } finally {
    goodLoading.value = false
  }
}

const handleGoodSearch = () => {
  goodParams.pageNum = 1
  fetchGoodApps()
}

const handleGoodPageChange = (page: number) => {
  goodParams.pageNum = page
  fetchGoodApps()
}

const STATIC_BASE = 'http://localhost:8123/api/static'

// 点击应用卡片，跳转到对话页查看详情 / 效果
const handleAppClick = (app: API.AppVO) => {
  if (!app.id) return
  router.push(`/app/chat/${app.id}`)
}

// 查看对话：跳转到对话页
const handleViewChat = (app: API.AppVO, e: Event) => {
  e.stopPropagation()
  if (!app.id) return
  router.push(`/app/chat/${app.id}`)
}

// 查看作品：打开部署后的地址
const handleViewWork = (app: API.AppVO, e: Event) => {
  e.stopPropagation()
  if (!app.id) return
  if (!app.deployKey) {
    message.warning('该应用尚未部署')
    return
  }
  window.open(`http://localhost/${app.deployKey}/`, '_blank')
}

onMounted(() => {
  // 登录后才查询我的应用
  if (loginUserStore.loginUser.id) {
    fetchMyApps()
  }
  fetchGoodApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 顶部标题和提示词输入区域 -->
    <section class="hero-section">
      <h1 class="hero-title">
        <img :src="logo" class="hero-logo" alt="logo" />
        AI应用生成平台
      </h1>
      <p class="hero-subtitle">与 AI 对话轻松创建应用和网站</p>

      <div class="prompt-card">
        <textarea
          v-model="prompt"
          class="prompt-input"
          placeholder="一句话轻松创建网站应用"
          rows="4"
          @keydown.enter.ctrl.prevent="handleCreate"
        />
        <div class="prompt-actions">
          <a-button
            type="primary"
            class="send-btn"
            shape="circle"
            :loading="creating"
            @click="handleCreate"
          >
            <template #icon><ArrowUpOutlined /></template>
          </a-button>
        </div>
      </div>

      <div class="suggestions">
        <a-button
          v-for="item in suggestions"
          :key="item.label"
          class="suggestion-btn"
          @click="handleSuggestionClick(item)"
        >
          {{ item.label }}
        </a-button>
      </div>
    </section>

    <!-- 应用列表区域 -->
    <section class="content-card">
      <!-- 我的作品（登录后可见） -->
      <template v-if="loginUserStore.loginUser.id">
        <div class="section-header">
          <h2 class="section-title">我的作品</h2>
          <a-input
            v-model:value="myParams.appName"
            class="section-search"
            placeholder="搜索我的应用名称"
            allow-clear
            @press-enter="handleMySearch"
          >
            <template #prefix><SearchOutlined /></template>
            <template #addonAfter>
              <a-button type="text" size="small" @click="handleMySearch">搜索</a-button>
            </template>
          </a-input>
        </div>
        <a-spin :spinning="myLoading">
          <a-empty v-if="myApps.length === 0" description="还没有作品，输入提示词创建第一个应用吧" />
          <div v-else class="app-grid">
            <div v-for="app in myApps" :key="app.id" class="app-card" @click="handleAppClick(app)">
              <div class="app-cover">
                <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
                <div v-else class="cover-placeholder">
                  <span class="robot-icon">🤖</span>
                </div>
                <div class="card-overlay">
                  <a-button type="primary" class="overlay-btn" @click="handleViewChat(app, $event)">查看对话</a-button>
                  <a-button class="overlay-btn" @click="handleViewWork(app, $event)">查看作品</a-button>
                </div>
              </div>
              <div class="app-info">
                <CodeOutlined class="app-icon" />
                <div class="app-meta">
                  <div class="app-name">{{ app.appName ?? '未命名应用' }}</div>
                  <div class="app-desc">创建于 {{ formatRelativeTime(app.createTime) }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="myTotal > PAGE_SIZE" class="pagination-wrap">
            <a-pagination
              :current="myParams.pageNum"
              :page-size="PAGE_SIZE"
              :total="myTotal"
              :show-size-changer="false"
              @change="handleMyPageChange"
            />
          </div>
        </a-spin>
      </template>

      <!-- 精选案例 -->
      <div class="section-header">
        <h2 class="section-title">精选案例</h2>
        <a-input
          v-model:value="goodParams.appName"
          class="section-search"
          placeholder="搜索精选应用名称"
          allow-clear
          @press-enter="handleGoodSearch"
        >
          <template #prefix><SearchOutlined /></template>
          <template #addonAfter>
            <a-button type="text" size="small" @click="handleGoodSearch">搜索</a-button>
          </template>
        </a-input>
      </div>
      <a-spin :spinning="goodLoading">
        <a-empty v-if="goodApps.length === 0" description="暂无精选应用" />
        <div v-else class="app-grid">
          <div v-for="app in goodApps" :key="app.id" class="app-card" @click="handleAppClick(app)">
            <div class="app-cover">
              <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
              <div v-else class="cover-placeholder">
                <span class="robot-icon">🤖</span>
              </div>
              <div class="card-overlay">
                <a-button type="primary" class="overlay-btn" @click="handleViewChat(app, $event)">查看对话</a-button>
                <a-button class="overlay-btn" @click="handleViewWork(app, $event)">查看作品</a-button>
              </div>
            </div>
            <div class="app-info">
              <CodeOutlined class="app-icon" />
              <div class="app-meta">
                <div class="app-name">{{ app.appName ?? '未命名应用' }}</div>
                <div class="app-desc">{{ app.user?.userName ?? '匿名用户' }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="goodTotal > PAGE_SIZE" class="pagination-wrap">
          <a-pagination
            :current="goodParams.pageNum"
            :page-size="PAGE_SIZE"
            :total="goodTotal"
            :show-size-changer="false"
            @change="handleGoodPageChange"
          />
        </div>
      </a-spin>
    </section>
  </div>
</template>

<script lang="ts">
import { h } from 'vue'

export default {
  name: 'HomePage',
}
</script>

<style scoped>
.home-page {
  margin: -24px;
  min-height: calc(100vh - 64px - 53px);
  background: linear-gradient(180deg, #f7fbf8 0%, #ddf4ec 30%, #a5e2ec 60%, #eef7f9 100%);
}

/* 顶部区域 */
.hero-section {
  padding: 64px 24px 64px;
  text-align: center;
}

.hero-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  font-size: 48px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.hero-logo {
  width: 56px;
  height: 56px;
  border-radius: 50%;
}

.hero-subtitle {
  font-size: 20px;
  color: #5a5a5a;
  margin: 0 0 36px;
}

/* 提示词卡片 */
.prompt-card {
  max-width: 760px;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px;
  padding: 20px 24px 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  text-align: left;
}

.prompt-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 17px;
  line-height: 1.6;
  color: #1a1a1a;
  background: transparent;
  box-sizing: border-box;
  font-family: inherit;
}

.prompt-input::placeholder {
  color: #b0b0b0;
}

.prompt-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 12px;
}

.send-btn {
  width: 44px;
  height: 44px;
  background: #9aa4b2;
}

.send-btn:not(:disabled):hover {
  background: #1677ff !important;
}

/* 推荐提示词 */
.suggestions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.suggestion-btn {
  border-radius: 12px;
  height: 40px;
  padding: 0 20px;
  font-size: 15px;
  color: #4a4a4a;
  background: rgba(255, 255, 255, 0.75);
}

/* 列表卡片区域：限宽居中，不再占满全屏 */
.content-card {
  background: #fff;
  border-radius: 24px;
  max-width: 1200px;
  margin: -40px auto 48px;
  padding: 40px 48px 48px;
  min-height: 400px;
  box-shadow: 0 12px 40px rgba(31, 62, 72, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.section-search {
  width: 280px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.app-card {
  cursor: pointer;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e8e8e8;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.app-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.25s ease;
  z-index: 1;
}

.app-card:hover .card-overlay {
  opacity: 1;
}

.overlay-btn {
  border-radius: 16px;
  font-size: 14px;
  padding: 0 20px;
  height: 36px;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.robot-icon {
  font-size: 48px;
  opacity: 0.6;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
}

.app-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1677ff;
  color: #fff;
  border-radius: 50%;
  font-size: 18px;
}

.app-meta {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-desc {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination-wrap {
  margin-top: 32px;
  text-align: center;
}

/* 响应式 */
@media (max-width: 992px) {
  .app-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-title {
    font-size: 36px;
  }
}

@media (max-width: 768px) {
  .home-page {
    /* 与 BasicContent 小屏内边距（12px）保持一致，避免横向溢出 */
    margin: -12px;
  }

  .content-card {
    margin-top: -24px;
  }
}

@media (max-width: 576px) {
  .app-grid {
    grid-template-columns: 1fr;
  }

  .hero-section {
    padding: 40px 16px 32px;
  }

  .content-card {
    padding: 28px 20px 32px;
  }

  .section-search {
    width: 100%;
  }
}
</style>

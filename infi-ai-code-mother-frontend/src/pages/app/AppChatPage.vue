<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  PaperClipOutlined,
  EditOutlined,
  ThunderboltOutlined,
  CloudUploadOutlined,
  HomeOutlined,
  FormOutlined,
  LoadingOutlined,
  GlobalOutlined,
  InfoCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, deployApp, deleteApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/layouts/components/GlobalHeader.vue'
import logo from '@/assets/logo.png'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 后端接口基础地址
const API_BASE_URL = 'http://localhost:8123/api'

// 应用 id（保持字符串类型，避免雪花ID精度丢失）
const appId = route.params.id as string

// 聊天消息
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

const app = ref<API.AppVO | null>(null)
const messages = ref<ChatMessage[]>([])
const input = ref('')
const streaming = ref(false)
const loading = ref(false)
const deploying = ref(false)

// 应用详情抽屉
const detailVisible = ref(false)

// 网页预览
const previewReady = ref(false)
const previewKey = ref(0)
const messagesRef = ref<HTMLElement | null>(null)
let eventSource: EventSource | null = null

// 是否为应用创建者（仅创建者可以对话和部署）
const isOwner = computed(
  () => !!app.value?.userId && app.value.userId === loginUserStore.loginUser.id,
)

// 部署地址：根据 deployKey 推导（与后端部署规则一致），部署成功后刷新应用信息即可更新
const deployUrl = computed(() =>
  app.value?.deployKey ? `http://localhost/${app.value.deployKey}/` : '',
)

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '暂无'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  return date.toLocaleString('zh-CN', { hour12: false })
}

// 本地预览地址：http://localhost:8123/api/static/{codeGenType}_{appId}/
// 附带 previewKey 作为版本号，生成完成后强制加载最新文件
const previewUrl = computed(() => {
  if (!app.value?.codeGenType) return ''
  return `${API_BASE_URL}/static/${app.value.codeGenType}_${appId}/?t=${previewKey.value}`
})

// 消息区域滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

// 获取应用详情
const fetchApp = async () => {
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
    } else {
      message.error('获取应用详情失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('获取应用详情失败，请检查网络或登录状态')
  } finally {
    loading.value = false
  }
}

// 检查生成的网站文件是否可访问
const checkPreview = async () => {
  if (!previewUrl.value) {
    previewReady.value = false
    return
  }
  try {
    const res = await fetch(previewUrl.value, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    })
    previewReady.value = res.ok
  } catch (error) {
    previewReady.value = false
  }
}

// 发送消息（SSE 流式接收 AI 回复）
const sendMessage = (text: string) => {
  const content = text.trim()
  if (!content || streaming.value || !appId) return
  if (!isOwner.value) {
    message.warning('仅应用创建者可以继续对话')
    return
  }
  // 追加用户消息和空的 AI 消息
  messages.value.push({ role: 'user', content })
  const aiMessage = reactive<ChatMessage>({
    role: 'assistant',
    content: '',
    streaming: true,
  })
  messages.value.push(aiMessage)
  input.value = ''
  streaming.value = true
  scrollToBottom()

  // 建立 SSE 连接
  const url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(content)}`
  eventSource = new EventSource(url, { withCredentials: true })

  // 接收流式内容，后端返回的数据格式为 {"d": "内容片段"}
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data?.d) {
        aiMessage.content += data.d
        scrollToBottom()
      }
    } catch (error) {
      // 忽略解析异常的片段
    }
  }

  // 流结束（后端发送 done 事件）
  const handleFinish = () => {
    eventSource?.close()
    eventSource = null
    aiMessage.streaming = false
    if (!aiMessage.content) {
      aiMessage.content = '（生成失败，请稍后重试）'
    }
    streaming.value = false
    // 生成完成后刷新应用信息并展示网站效果
    previewKey.value += 1
    checkPreview()
    fetchApp()
  }

  eventSource.addEventListener('done', handleFinish)

  eventSource.onerror = () => {
    handleFinish()
  }
}

// 点击发送按钮
const handleSend = () => {
  sendMessage(input.value)
}

// 工具按钮
const handleUpload = () => {
  message.info('演示版本暂未支持上传附件')
}

const handleEdit = () => {
  input.value = input.value ? `${input.value}，请支持在线编辑内容` : '请支持在线编辑内容'
}

const handleOptimize = () => {
  input.value = input.value
    ? `${input.value}，请优化页面的视觉设计和交互体验`
    : '请优化页面的视觉设计和交互体验'
}

// 部署应用
const handleDeploy = async () => {
  if (!isOwner.value) {
    message.warning('仅应用创建者可以部署应用')
    return
  }
  deploying.value = true
  try {
    const res = await deployApp({ appId: appId as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      message.success('部署成功')
      // 打开部署后的网站
      window.open(res.data.data, '_blank')
      // 刷新应用信息，deployUrl 会根据 deployKey 自动更新
      fetchApp()
    } else {
      message.error('部署失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('部署失败，请检查网络或登录状态')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开部署后的网站
const openDeployUrl = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// 删除应用
const deleting = ref(false)
const handleDelete = async () => {
  if (!isOwner.value) {
    message.warning('仅应用创建者可以删除应用')
    return
  }
  deleting.value = true
  try {
    const res = await deleteApp({ id: appId })
    if (res.data.code === 0) {
      message.success('删除成功')
      detailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('删除失败，请检查网络或登录状态')
  } finally {
    deleting.value = false
  }
}

// 顶部下拉菜单
const handleMenuClick = ({ key }: { key: string }) => {
  if (key === 'home') {
    router.push('/')
  } else if (key === 'update') {
    window.open(`/app/update/${appId}`, '_blank')
  }
}

onMounted(async () => {
  await fetchApp()
  await checkPreview()
  // 从主页跳转过来时，自动将初始提示词作为消息发送给 AI
  const initPrompt = route.query.initPrompt as string
  if (initPrompt) {
    sendMessage(initPrompt)
  }
})

onBeforeUnmount(() => {
  eventSource?.close()
  eventSource = null
})
</script>

<template>
  <div class="chat-page">
    <!-- 全局导航栏 -->
    <GlobalHeader />

    <!-- 核心内容区域 -->
    <div class="chat-body">
      <!-- 左侧对话区域 -->
      <div class="chat-panel">
        <div ref="messagesRef" class="messages-area">
          <a-spin v-if="loading && messages.length === 0" class="messages-loading" />
          <a-empty
            v-else-if="messages.length === 0"
            description="输入消息，和 AI 对话生成网站应用"
            class="messages-empty"
          />
          <template v-else>
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="msg-row"
              :class="msg.role"
            >
              <img v-if="msg.role === 'assistant'" :src="logo" class="msg-avatar" alt="AI" />
              <div class="msg-bubble">
                <span class="msg-content">{{ msg.content }}</span>
                <LoadingOutlined v-if="msg.streaming" class="msg-loading" />
              </div>
            </div>
          </template>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-area">
          <textarea
            v-model="input"
            class="msg-input"
            :placeholder="isOwner ? '描述越详细，页面越具体，可以一步一步完善生成效果' : '仅应用创建者可以对话'"
            :disabled="streaming || !isOwner"
            rows="2"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="input-actions">
            <div class="input-tools">
              <a-button
                size="small"
                type="text"
                :disabled="!isOwner || streaming"
                @click="handleUpload"
              >
                <template #icon><PaperClipOutlined /></template>
                上传
              </a-button>
              <a-button
                size="small"
                type="text"
                :disabled="!isOwner || streaming"
                @click="handleEdit"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                size="small"
                type="text"
                :disabled="!isOwner || streaming"
                @click="handleOptimize"
              >
                <template #icon><ThunderboltOutlined /></template>
                优化
              </a-button>
            </div>
            <a-button
              type="primary"
              shape="circle"
              class="send-btn"
              :disabled="!input.trim() || streaming || !isOwner"
              :loading="streaming"
              @click="handleSend"
            >
              <template #icon><ArrowUpOutlined /></template>
            </a-button>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-panel">
        <!-- 预览区工具栏：右上角为应用详情和部署按钮 -->
        <div class="preview-toolbar">
          <span class="preview-title">网站效果预览</span>
          <div class="preview-actions">
            <a-tooltip v-if="deployUrl" :title="`已部署：${deployUrl}`">
              <a-button size="small" class="preview-action-btn" @click="openDeployUrl">
                <template #icon><GlobalOutlined /></template>
                访问已部署
              </a-button>
            </a-tooltip>
            <a-button size="small" class="preview-action-btn" @click="detailVisible = true">
              <template #icon><InfoCircleOutlined /></template>
              应用详情
            </a-button>
            <a-button
              v-if="isOwner"
              size="small"
              type="primary"
              class="preview-action-btn preview-deploy-btn"
              :loading="deploying"
              @click="handleDeploy"
            >
              <template #icon><CloudUploadOutlined /></template>
              部署
            </a-button>
          </div>
        </div>
        <div class="preview-body">
          <div v-if="!previewReady" class="preview-placeholder">
            <img :src="logo" class="placeholder-logo" alt="logo" />
            <p class="placeholder-title">网站效果预览</p>
            <p class="placeholder-desc">
              {{ streaming ? 'AI 正在生成网站，请稍候...' : '网站文件生成完成后，将在这里展示效果' }}
            </p>
          </div>
          <iframe
            v-else
            :key="previewKey"
            :src="previewUrl"
            class="preview-iframe"
            title="网站效果预览"
          />
        </div>
      </div>
    </div>

    <!-- 应用详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="应用详情" :width="420">
      <a-spin :spinning="loading">
        <a-descriptions :column="1" bordered size="small" class="detail-descriptions">
          <a-descriptions-item label="应用名称">
            {{ app?.appName ?? '未命名应用' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建者">
            {{ app?.user?.userName ?? '匿名用户' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatTime(app?.createTime) }}
          </a-descriptions-item>
        </a-descriptions>
        <div v-if="isOwner" class="detail-actions">
          <a-space>
            <a-button type="primary" @click="handleMenuClick({ key: 'update' })">
              <template #icon><FormOutlined /></template>
              修改
            </a-button>
            <a-popconfirm
              title="确定要删除这个应用吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete"
            >
              <a-button danger :loading="deleting">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
  overflow: hidden;
}

/* 核心内容区域 */
.chat-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 左侧对话区域 */
.chat-panel {
  width: 40%;
  min-width: 360px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e8e8e8;
  background: #fff;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
}

.messages-loading,
.messages-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.msg-row {
  display: flex;
  margin-bottom: 20px;
  gap: 8px;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;
}

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.msg-row.assistant .msg-bubble {
  background: #f5f6f8;
  color: #1a1a1a;
  border-top-left-radius: 4px;
}

.msg-row.user .msg-bubble {
  background: #e6f4ff;
  color: #1a1a1a;
  border-top-right-radius: 4px;
}

.msg-content {
  white-space: pre-wrap;
}

.msg-loading {
  margin-left: 6px;
  color: #1677ff;
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px;
  flex-shrink: 0;
}

.msg-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  background: transparent;
  box-sizing: border-box;
  color: #1a1a1a;
}

.msg-input::placeholder {
  color: #b0b0b0;
}

.msg-input:disabled {
  background: transparent;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-tools {
  display: flex;
  gap: 4px;
}

.send-btn {
  width: 36px;
  height: 36px;
  background: #9aa4b2;
}

.send-btn:not(:disabled):hover {
  background: #1677ff !important;
}

/* 右侧预览区域 */
.preview-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 预览区工具栏 */
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-action-btn {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s ease;
}

/* 访问已部署 - 绿色 */
.preview-action-btn:has(.anticon-global) {
  color: #52c41a;
  border-color: #52c41a;
}
.preview-action-btn:has(.anticon-global):hover {
  color: #fff;
  background: #52c41a;
  border-color: #52c41a;
}

/* 应用详情 - 蓝色 */
.preview-action-btn:has(.anticon-info-circle) {
  color: #1677ff;
  border-color: #1677ff;
}
.preview-action-btn:has(.anticon-info-circle):hover {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

/* 部署 - 橙色渐变 */
.preview-deploy-btn {
  background: linear-gradient(135deg, #fa8c16, #faad14);
  border-color: #fa8c16;
  color: #fff;
}

.preview-deploy-btn:not(:disabled):hover {
  background: linear-gradient(135deg, #d46b08, #d48806) !important;
  border-color: #d46b08 !important;
  color: #fff;
}

.preview-body {
  flex: 1;
  min-height: 0;
  position: relative;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.preview-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  color: #999;
  text-align: center;
  padding: 24px;
}

.placeholder-logo {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  opacity: 0.6;
  margin-bottom: 16px;
}

.placeholder-title {
  font-size: 18px;
  font-weight: 600;
  color: #666;
  margin: 0 0 8px;
}

.placeholder-desc {
  font-size: 14px;
  color: #aaa;
  margin: 0;
}

/* 应用详情抽屉 */
.detail-descriptions {
  margin-top: 8px;
}

.detail-actions {
  margin-top: 24px;
}

.detail-prompt {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 响应式：窄屏时上下布局 */
@media (max-width: 768px) {
  .chat-body {
    flex-direction: column;
  }

  .chat-panel {
    width: 100%;
    min-width: 0;
    height: 55%;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }

  .preview-panel {
    height: 45%;
  }
}

/* 手机端隐藏预览工具栏标题，避免与操作按钮拥挤 */
@media (max-width: 576px) {
  .preview-title {
    display: none;
  }
}
</style>

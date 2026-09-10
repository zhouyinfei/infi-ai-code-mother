<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'

// 消息类型选项
const messageTypeOptions = [
  { label: '用户消息', value: 'user' },
  { label: 'AI 消息', value: 'assistant' },
]

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 160,
    ellipsis: true,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    ellipsis: true,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 120,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    width: 160,
    ellipsis: true,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    width: 160,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
]

// 数据
const data = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  message: '',
  messageType: undefined,
  appId: undefined,
  userId: undefined,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('获取数据失败，请检查网络或登录状态')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchParams.message = ''
  searchParams.messageType = undefined
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.pageNum = 1
  fetchData()
}

// 分页变化
const handleTableChange = (pag: { current: number; pageSize: number }) => {
  searchParams.pageNum = pag.current
  searchParams.pageSize = pag.pageSize
  fetchData()
}

// 格式化消息类型
const formatMessageType = (type?: string) => {
  if (type === 'user') return '用户消息'
  if (type === 'assistant') return 'AI 消息'
  return type ?? '未知'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="chat-history-manage-page">
    <!-- 搜索区域 -->
    <div class="search-bar">
      <a-input
        v-model:value="searchParams.message"
        placeholder="请输入消息内容"
        style="width: 200px; margin-right: 12px"
        allow-clear
      />
      <a-select
        v-model:value="searchParams.messageType"
        placeholder="消息类型"
        style="width: 140px; margin-right: 12px"
        allow-clear
        :options="messageTypeOptions"
      />
      <a-input-number
        v-model:value="searchParams.appId"
        placeholder="应用 ID"
        style="width: 160px; margin-right: 12px"
      />
      <a-input-number
        v-model:value="searchParams.userId"
        placeholder="用户 ID"
        style="width: 160px; margin-right: 12px"
      />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
    </div>

    <!-- 对话历史表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="{
        current: searchParams.pageNum,
        pageSize: searchParams.pageSize,
        total: total,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (t: number) => `共 ${t} 条`,
      }"
      :row-key="'id'"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 消息类型 -->
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
            {{ formatMessageType(record.messageType) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.chat-history-manage-page {
  padding: 24px;
  background: #fff;
  min-height: 100%;
}

.search-bar {
  margin-bottom: 16px;
}
</style>

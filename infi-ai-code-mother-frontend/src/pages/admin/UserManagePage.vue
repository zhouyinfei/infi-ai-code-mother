<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listUserVoByPage, deleteUser } from '@/api/userController'

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 180,
    ellipsis: true,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
  },
]

// 数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
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
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.pageNum = 1
  fetchData()
}

// 分页变化
const handleTableChange = (pag: { current: number; pageSize: number }) => {
  searchParams.pageNum = pag.current
  searchParams.pageSize = pag.pageSize
  fetchData()
}

// 删除用户
const handleDelete = async (record: API.UserVO) => {
  if (!record.id) return
  try {
    const res = await deleteUser({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="user-manage-page">
    <!-- 搜索区域 -->
    <div class="search-bar">
      <a-input
        v-model:value="searchParams.userAccount"
        placeholder="请输入账号"
        style="width: 180px; margin-right: 12px"
        allow-clear
      />
      <a-input
        v-model:value="searchParams.userName"
        placeholder="请输入用户名"
        style="width: 180px; margin-right: 12px"
        allow-clear
      />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
    </div>

    <!-- 用户表格 -->
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
        <!-- 头像 -->
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-avatar v-if="record.userAvatar" :src="record.userAvatar" />
          <a-avatar v-else>{{ record.userName?.charAt(0) || 'U' }}</a-avatar>
        </template>

        <!-- 用户角色 -->
        <template v-if="column.dataIndex === 'userRole'">
          <a-tag :color="record.userRole === 'admin' ? 'red' : 'blue'">
            {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>

        <!-- 操作列 -->
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除该用户？" @confirm="handleDelete(record)">
            <a-button type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.user-manage-page {
  padding: 24px;
  background: #fff;
  min-height: 100%;
}

.page-title {
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.search-bar {
  margin-bottom: 16px;
}
</style>

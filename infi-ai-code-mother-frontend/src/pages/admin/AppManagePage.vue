<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  listAppVoByPageByAdmin,
  deleteAppByAdmin,
  updateAppByAdmin,
} from '@/api/appController'
import logo from '@/assets/logo.png'

// 精选应用的优先级
const GOOD_APP_PRIORITY = 99

// 代码生成类型选项
const codeGenTypeOptions = [
  { label: '原生 HTML 模式', value: 'html' },
  { label: '原生多文件模式', value: 'multi_file' },
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
    title: '应用名称',
    dataIndex: 'appName',
    width: 160,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    ellipsis: true,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 130,
  },
  {
    title: '部署标识',
    dataIndex: 'deployKey',
    width: 110,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 90,
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
  {
    title: '操作',
    key: 'action',
    width: 260,
  },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  codeGenType: undefined,
  priority: undefined,
  userId: undefined,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
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
  searchParams.appName = ''
  searchParams.codeGenType = undefined
  searchParams.priority = undefined
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

// 编辑：新开页面跳转到应用信息修改页
const handleEdit = (record: API.AppVO) => {
  if (!record.id) return
  window.open(`/app/update/${record.id}?admin=1`, '_blank')
}

// 精选：设置优先级为 99（本质也是编辑）
const handleSetGood = async (record: API.AppVO) => {
  if (!record.id) return
  try {
    const res = await updateAppByAdmin({
      id: record.id,
      priority: GOOD_APP_PRIORITY,
    })
    if (res.data.code === 0) {
      message.success('已设为精选应用')
      fetchData()
    } else {
      message.error('设置失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('设置失败')
  }
}

// 取消精选：将优先级重置为 0
const handleCancelGood = async (record: API.AppVO) => {
  if (!record.id) return
  try {
    const res = await updateAppByAdmin({
      id: record.id,
      priority: 0,
    })
    if (res.data.code === 0) {
      message.success('已取消精选')
      fetchData()
    } else {
      message.error('操作失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 删除应用
const handleDelete = async (record: API.AppVO) => {
  if (!record.id) return
  try {
    const res = await deleteAppByAdmin({ id: record.id })
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
  <div class="app-manage-page">
    <!-- 搜索区域 -->
    <div class="search-bar">
      <a-input
        v-model:value="searchParams.appName"
        placeholder="请输入应用名称"
        style="width: 180px; margin-right: 12px"
        allow-clear
      />
      <a-select
        v-model:value="searchParams.codeGenType"
        placeholder="生成类型"
        style="width: 160px; margin-right: 12px"
        allow-clear
        :options="codeGenTypeOptions"
      />
      <a-input-number
        v-model:value="searchParams.priority"
        placeholder="优先级"
        style="width: 120px; margin-right: 12px"
      />
      <a-input-number
        v-model:value="searchParams.userId"
        placeholder="用户 ID"
        style="width: 160px; margin-right: 12px"
      />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
    </div>

    <!-- 应用表格 -->
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
        <!-- 封面 -->
        <template v-if="column.dataIndex === 'cover'">
          <div class="cover-thumb">
            <img v-if="record.cover" :src="record.cover" alt="cover" />
            <img v-else :src="logo" alt="default" class="default-cover" />
          </div>
        </template>

        <!-- 生成类型 -->
        <template v-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="record.codeGenType === 'html' ? 'green' : 'blue'">
            {{ record.codeGenType === 'html' ? '原生 HTML' : '原生多文件' }}
          </a-tag>
        </template>

        <!-- 部署标识 -->
        <template v-if="column.dataIndex === 'deployKey'">
          <span v-if="record.deployKey">{{ record.deployKey }}</span>
          <span v-else class="text-muted">未部署</span>
        </template>

        <!-- 优先级 -->
        <template v-if="column.dataIndex === 'priority'">
          <a-tag v-if="record.priority === 99" color="gold">精选</a-tag>
          <span v-else>{{ record.priority ?? 0 }}</span>
        </template>

        <!-- 操作列 -->
        <template v-if="column.key === 'action'">
          <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
          <template v-if="record.priority === 99">
            <a-button size="small" class="btn-cancel-good" @click="handleCancelGood(record)">取消精选</a-button>
          </template>
          <template v-else>
            <a-button size="small" class="btn-set-good" @click="handleSetGood(record)">精选</a-button>
          </template>
          <a-popconfirm title="确定删除该应用？" @confirm="handleDelete(record)">
            <a-button size="small" class="btn-delete">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.app-manage-page {
  padding: 24px;
  background: #fff;
  min-height: 100%;
}

.search-bar {
  margin-bottom: 16px;
}

.cover-thumb {
  width: 56px;
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-thumb .default-cover {
  width: 24px;
  height: 24px;
  object-fit: contain;
  opacity: 0.5;
}

.text-muted {
  color: #bbb;
}

.btn-cancel-good {
  margin-left: 8px;
  background: #fa8c16;
  color: #fff;
  border-color: #fa8c16;
}

.btn-cancel-good:hover {
  background: #d46b08;
  border-color: #d46b08;
  color: #fff;
}

.btn-set-good {
  margin-left: 8px;
  background: #fff;
  color: rgba(0, 0, 0, 0.88);
  border-color: #d9d9d9;
}

.btn-set-good:hover {
  color: #4096ff;
  border-color: #4096ff;
}

.btn-delete {
  margin-left: 8px;
  background: #fff;
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.btn-delete:hover {
  background: #fff1f0;
  border-color: #ff7875;
  color: #ff7875;
}
</style>

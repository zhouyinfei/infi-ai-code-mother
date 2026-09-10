<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用 id（保持字符串类型，避免雪花ID精度丢失）
const appId = route.params.id as string

const loading = ref(false)
const saving = ref(false)
const app = ref<API.AppVO | null>(null)

// 表单数据
const formState = reactive({
  appName: '',
  cover: '',
  priority: 0,
})

// 是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// 是否有编辑权限：管理员可以编辑任意应用，普通用户只能编辑自己的应用
const canEdit = computed(
  () => isAdmin.value || (!!app.value?.userId && app.value.userId === loginUserStore.loginUser.id),
)

// 获取应用详情
const fetchApp = async () => {
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
      formState.appName = res.data.data.appName ?? ''
      formState.cover = res.data.data.cover ?? ''
      formState.priority = res.data.data.priority ?? 0
    } else {
      message.error('获取应用详情失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('获取应用详情失败，请检查网络或登录状态')
  } finally {
    loading.value = false
  }
}

// 保存修改
const handleSave = async () => {
  if (!formState.appName.trim()) {
    message.warning('应用名称不能为空')
    return
  }
  saving.value = true
  try {
    // 管理员可以更新应用名称、封面、优先级；普通用户只能更新应用名称
    const res = isAdmin.value
      ? await updateAppByAdmin({
          id: appId,
          appName: formState.appName.trim(),
          cover: formState.cover.trim(),
          priority: formState.priority,
        })
      : await updateApp({
          id: appId,
          appName: formState.appName.trim(),
        })
    if (res.data.code === 0) {
      message.success('保存成功')
      fetchApp()
    } else {
      message.error('保存失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error) {
    message.error('保存失败，请检查网络或登录状态')
  } finally {
    saving.value = false
  }
}

// 返回
const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

onMounted(() => {
  fetchApp()
})
</script>

<template>
  <div class="app-update-page">
    <a-spin :spinning="loading">
      <!-- 无权限提示 -->
      <a-result
        v-if="app && !canEdit"
        status="403"
        title="无权限"
        sub-title="普通用户只能编辑自己创建的应用"
      >
        <template #extra>
          <a-button type="primary" @click="handleBack">返回</a-button>
        </template>
      </a-result>

      <!-- 编辑表单 -->
      <div v-else class="update-card">
        <div class="page-header">
          <a-button @click="handleBack">
            <template #icon><ArrowLeftOutlined /></template>
            返回
          </a-button>
          <h2 class="page-title">应用信息修改</h2>
        </div>

        <a-form :model="formState" layout="vertical" class="update-form">
          <a-form-item label="应用 ID">
            <a-input :value="appId" disabled />
          </a-form-item>

          <a-form-item label="应用名称" required>
            <a-input
              v-model:value="formState.appName"
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <!-- 管理员可编辑封面和优先级 -->
          <template v-if="isAdmin">
            <a-form-item label="应用封面（图片 URL）">
              <a-input
                v-model:value="formState.cover"
                placeholder="请输入封面图片地址"
                allow-clear
              />
            </a-form-item>
            <div v-if="formState.cover" class="cover-preview">
              <img :src="formState.cover" alt="封面预览" />
            </div>

            <a-form-item label="优先级（99 为精选应用）">
              <a-input-number v-model:value="formState.priority" :min="0" :max="99" class="priority-input" />
            </a-form-item>
          </template>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
              <a-button @click="handleBack">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.app-update-page {
  background: #fff;
  min-height: 100%;
  padding: 24px;
}

.update-card {
  max-width: 560px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.update-form {
  max-width: 480px;
}

.priority-input {
  width: 100%;
}

.cover-preview {
  margin: -8px 0 24px;
  width: 100%;
  max-width: 320px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.cover-preview img {
  width: 100%;
  display: block;
  aspect-ratio: 16 / 9;
  object-fit: cover;
}
</style>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const formState = reactive({
  userAccount: '',
  userPassword: '',
})

const handleSubmit = async () => {
  try {
    const res = await userLogin({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword,
    })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      message.error(res.data.message ?? '登录失败')
    }
  } catch (error) {
    message.error('登录失败，请检查网络连接')
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">水豚 AI 应用生成 - 用户登录</h2>
      <p class="desc">不写一行代码，生成完整应用</p>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit" layout="vertical">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" />
        </a-form-item>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large" />
        </a-form-item>
        <div class="tips">
          没有账号？
          <RouterLink to="/user/register">去注册</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" class="login-btn">登录</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
  padding: 20px;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
}

.title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.desc {
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 32px;
}

.tips {
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.tips a {
  color: #1677ff;
  text-decoration: none;
  margin-left: 4px;
}

.tips a:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  border-radius: 8px;
  height: 44px;
  font-size: 16px;
}

/* 覆盖 ant-design 的 form-item 默认 margin */
:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

:deep(.ant-input),
:deep(.ant-input-password .ant-input) {
  border-radius: 8px;
}

@media (max-width: 480px) {
  .login-card {
    padding: 36px 24px 24px;
  }

  .title {
    font-size: 18px;
  }
}
</style>

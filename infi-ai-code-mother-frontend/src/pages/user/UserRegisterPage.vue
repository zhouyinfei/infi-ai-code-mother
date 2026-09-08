<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userRegister } from '@/api/userController'

const router = useRouter()

const formState = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const handleSubmit = async () => {
  try {
    const res = await userRegister({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword,
      checkPassword: formState.checkPassword,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error(res.data.message ?? '注册失败')
    }
  } catch (error) {
    message.error('注册失败，请检查网络连接')
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h2 class="title">水豚 AI 应用生成 - 用户注册</h2>
      <p class="desc">不写一行代码，生成完整应用</p>

      <a-form :model="formState" name="register" autocomplete="off" @finish="handleSubmit" layout="vertical">
        <a-form-item
          name="userAccount"
          :rules="[
            { required: true, message: '请输入账号' },
            { min: 4, message: '账号不能小于 4 位' },
          ]"
        >
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
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('userPassword') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('两次输入的密码不一致'))
              },
            }),
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" size="large" />
        </a-form-item>
        <div class="tips">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" class="register-btn">注册</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
  padding: 20px;
  box-sizing: border-box;
}

.register-card {
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

.register-btn {
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
  .register-card {
    padding: 36px 24px 24px;
  }

  .title {
    font-size: 18px;
  }
}
</style>

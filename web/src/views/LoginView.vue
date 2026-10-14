<template>
  <NavBar fallback="/" />
  <div class="wrap">
    <h1>登录摄影来</h1>
    <p class="muted">约拍、接单都在这里</p>
    <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号" maxlength="11" :prefix-icon="'Iphone'" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="'Lock'" />
      </el-form-item>
      <el-button class="cta block" round :loading="loading" @click="submit">登录</el-button>
    </el-form>
    <p class="switch">还没有账号？<RouterLink :to="{ path: '/register', query: route.query }">注册</RouterLink></p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { type FormInstance, type FormRules } from 'element-plus'
import { toast } from '@/utils/message'
import NavBar from '@/components/NavBar.vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules: FormRules = {
  phone: [{ required: true, pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await user.login(form.phone, form.password)
    toast.success('登录成功')
    router.replace((route.query.redirect as string) || '/me')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.wrap { max-width: 400px; margin: 0 auto; padding: 24px 24px 0; }
h1 { margin: 0 0 6px; font-size: 26px; }
.muted { margin: 0 0 28px; }
.block { width: 100%; height: 46px; font-size: 16px; }
.switch { text-align: center; margin-top: 20px; font-size: 14px; color: #666; }
.switch a { color: var(--link); }
/* PC：居中白色卡片 */
@media (min-width: 768px) {
  .wrap { margin-top: 32px; padding: 36px 40px 28px; background: #fff; border-radius: var(--radius); box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06); }
}
</style>

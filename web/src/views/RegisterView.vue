<template>
  <NavBar fallback="/" />
  <div class="wrap">
    <h1>注册摄影来</h1>
    <p class="muted">注册后既可以约拍，也可以设置价格开始接单</p>
    <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号" maxlength="11" :prefix-icon="'Iphone'" />
      </el-form-item>
      <el-form-item prop="nickname">
        <el-input v-model="form.nickname" placeholder="昵称（选填）" maxlength="32" :prefix-icon="'User'" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码，6~32 位" show-password :prefix-icon="'Lock'" />
      </el-form-item>
      <el-form-item prop="confirm">
        <el-input v-model="form.confirm" type="password" placeholder="再次输入密码" show-password :prefix-icon="'Lock'" />
      </el-form-item>
      <el-form-item prop="agree">
        <el-checkbox v-model="form.agree">
          我已阅读并同意 <RouterLink to="/agreement" target="_blank">《用户协议》</RouterLink>和<RouterLink to="/privacy" target="_blank">《隐私政策》</RouterLink>
        </el-checkbox>
      </el-form-item>
      <el-button class="cta block" round :loading="loading" @click="submit">注册并登录</el-button>
    </el-form>
    <p class="switch">已有账号？<RouterLink :to="{ path: '/login', query: route.query }">登录</RouterLink></p>
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
const form = reactive({ phone: '', nickname: '', password: '', confirm: '', agree: false })
const rules: FormRules = {
  phone: [{ required: true, pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' }],
  confirm: [{
    validator: (_r, v, cb) => (v === form.password ? cb() : cb(new Error('两次输入的密码不一致'))),
    trigger: 'blur',
  }],
  agree: [{ validator: (_r, v, cb) => (v ? cb() : cb(new Error('请先阅读并同意用户协议和隐私政策'))) }],
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await user.register({ phone: form.phone, password: form.password, nickname: form.nickname || undefined, agree: form.agree })
    toast.success('注册成功')
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
.switch a, .el-checkbox a { color: var(--link); }
/* PC：居中白色卡片 */
@media (min-width: 768px) {
  .wrap { margin-top: 32px; padding: 36px 40px 28px; background: #fff; border-radius: var(--radius); box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06); }
}
</style>

<template>
  <NavBar fallback="/me" />
  <div class="page">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="card">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" maxlength="32" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="2">女</el-radio>
          <el-radio :value="1">男</el-radio>
          <el-radio :value="0">不展示</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="form.city" maxlength="32" placeholder="如：无锡市" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍一下你的拍摄风格" />
      </el-form-item>
      <el-form-item label="微信号">
        <el-input v-model="form.wechat" maxlength="64" placeholder="接单后展示给已确认预约的客户，不填则展示手机号" />
      </el-form-item>
      <el-button class="cta block" round :loading="saving" @click="save">保存</el-button>
    </el-form>

    <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-position="top" class="card">
      <h3>修改密码</h3>
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwd.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwd.newPassword" type="password" show-password placeholder="8~32 位，含字母和数字" />
      </el-form-item>
      <el-button class="block" round :loading="pwdSaving" @click="savePwd">修改密码</el-button>
    </el-form>

    <div class="card danger">
      <h3>注销账号</h3>
      <p class="muted">注销后手机号、昵称、头像等个人信息会被清除，客片会被删除，且无法恢复；历史订单和评价会显示为「已注销用户」。有进行中的预约时不能注销。</p>
      <el-button class="block" round type="danger" plain @click="removeAccount">注销账号</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { prompt, singleFlight, toast } from '@/utils/message'
import { passwordRules } from '@/utils/validate'
import NavBar from '@/components/NavBar.vue'
import { changePassword, deleteAccount, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()
const u = user.user!

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ nickname: u.nickname, gender: u.gender, city: u.city ?? '', bio: u.bio ?? '', wechat: u.wechat ?? '' })
const rules: FormRules = { nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }] }

async function save() {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    user.setUser(await updateProfile(form))
    toast.success('已保存')
    router.back()
  } finally {
    saving.value = false
  }
}

const pwdRef = ref<FormInstance>()
const pwdSaving = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: passwordRules,
}

async function savePwd() {
  if (!(await pwdRef.value?.validate().catch(() => false))) return
  pwdSaving.value = true
  try {
    await changePassword(pwd.oldPassword, pwd.newPassword)
    toast.success('密码已修改')
    pwd.oldPassword = ''
    pwd.newPassword = ''
  } finally {
    pwdSaving.value = false
  }
}

const removeAccount = singleFlight(async () => {
  const password = await prompt('注销后无法恢复。请输入登录密码确认：', { title: '注销账号', placeholder: '登录密码', confirmText: '确认注销', type: 'warning', inputType: 'password' })
  if (password === null) return
  if (!password) return void toast.error('请输入密码')
  await deleteAccount(password)
  await user.logout()
  toast.success('账号已注销')
  router.replace('/')
})
</script>

<style lang="scss" scoped>
h3 { margin: 0 0 12px; }
.block { width: 100%; }
.danger h3 { color: #e5483d; }
.danger p { margin: 0 0 12px; line-height: 1.6; }
@media (min-width: 768px) {
  .page { max-width: 880px; }
  .block { width: 200px; }
}
</style>

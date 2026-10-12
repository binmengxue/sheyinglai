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
        <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6~32 位" />
      </el-form-item>
      <el-button class="block" round :loading="pwdSaving" @click="savePwd">修改密码</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { changePassword, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()
const u = user.user!

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ nickname: u.nickname, gender: u.gender, city: u.city ?? '', bio: u.bio ?? '', wechat: u.wechat ?? '' })
const rules: FormRules = { nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }] }

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    user.setUser(await updateProfile(form))
    ElMessage.success('已保存')
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
  newPassword: [{ required: true, min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' }],
}

async function savePwd() {
  await pwdRef.value?.validate()
  pwdSaving.value = true
  try {
    await changePassword(pwd.oldPassword, pwd.newPassword)
    ElMessage.success('密码已修改')
    pwd.oldPassword = ''
    pwd.newPassword = ''
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style lang="scss" scoped>
h3 { margin: 0 0 12px; }
.block { width: 100%; }
@media (min-width: 768px) {
  .page { max-width: 880px; }
  .block { width: 200px; }
}
</style>

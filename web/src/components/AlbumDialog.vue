<template>
  <el-dialog v-model="visible" :title="album ? '编辑相册' : '新建相册'" width="460px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="名称" prop="title">
        <el-input v-model="form.title" maxlength="64" placeholder="如：2026 秋 · 太湖" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="公开">
        <el-switch v-model="form.visible" />
        <span class="muted" style="margin-left: 8px">关闭后只有登录后可见</span>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" />
        <span class="muted" style="margin-left: 8px">数字越小越靠前</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createAlbum, updateAlbum } from '@/api/album'
import type { Album, AlbumForm } from '@/types'

const props = defineProps<{ album?: Album | null }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [album: Album] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<AlbumForm>({ title: '', description: '', visible: true, sort: 0 })
const rules: FormRules = { title: [{ required: true, message: '请输入相册名称', trigger: 'blur' }] }

watch(visible, (v) => {
  if (!v) return
  Object.assign(form, props.album
    ? { title: props.album.title, description: props.album.description ?? '', visible: props.album.visible, sort: props.album.sort }
    : { title: '', description: '', visible: true, sort: 0 })
})

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const saved = props.album ? await updateAlbum(props.album.id, form) : await createAlbum(form)
    ElMessage.success('已保存')
    visible.value = false
    emit('saved', saved)
  } finally {
    saving.value = false
  }
}
</script>

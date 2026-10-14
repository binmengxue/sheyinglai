<template>
  <el-dialog v-model="visible" :title="album ? '编辑客片' : '新建客片'" width="460px" align-center>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="64" placeholder="如：晴天·咖啡店" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.style" filterable allow-create clearable placeholder="选择或输入拍摄类型" style="width: 100%">
          <el-option v-for="s in STYLES" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.location" maxlength="100" placeholder="如：无锡市·新吴区婚姻登记服务中心" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="公开">
        <el-switch v-model="form.visible" />
        <span class="muted" style="margin-left: 8px">关闭后只有自己可见</span>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" />
        <span class="muted" style="margin-left: 8px">数字越小越靠前</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="cta" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { toast } from '@/utils/message'
import { createAlbum, updateAlbum } from '@/api/album'
import { STYLES } from '@/utils/format'
import type { Album, AlbumForm } from '@/types'

const props = defineProps<{ album?: Album | null }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [album: Album] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const empty = (): AlbumForm => ({ title: '', description: '', style: '', location: '', visible: true, sort: 0 })
const form = reactive<AlbumForm>(empty())
const rules: FormRules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }

watch(visible, (v) => {
  if (!v) return
  const a = props.album
  Object.assign(form, a
    ? { title: a.title, description: a.description ?? '', style: a.style ?? '', location: a.location ?? '', visible: a.visible, sort: a.sort }
    : empty())
})

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const saved = props.album ? await updateAlbum(props.album.id, form) : await createAlbum(form)
    toast.success('已保存')
    visible.value = false
    emit('saved', saved)
  } finally {
    saving.value = false
  }
}
</script>

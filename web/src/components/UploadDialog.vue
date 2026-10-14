<template>
  <el-dialog v-model="visible" title="上传照片" width="600px" :close-on-click-modal="!running" :close-on-press-escape="!running" :show-close="!running">
    <el-upload
      drag
      multiple
      :auto-upload="false"
      :show-file-list="false"
      accept="image/jpeg,image/png,image/webp,image/gif"
      :on-change="onChange"
      :disabled="running"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">把照片拖到这里，或 <em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 jpg / png / webp / gif，单张不超过 {{ MAX_MB }}MB，可多选</div>
      </template>
    </el-upload>

    <div v-if="tasks.length" class="list">
      <div v-for="t in tasks" :key="t.uid" class="row">
        <span class="name" :title="t.name">{{ t.name }}</span>
        <el-progress
          class="bar"
          :percentage="t.progress"
          :status="t.status === 'success' ? 'success' : t.status === 'error' ? 'exception' : undefined"
        />
        <span class="state">{{ statusText[t.status] }}</span>
        <el-button v-if="t.status === 'waiting' && !running" link type="danger" @click="remove(t)">移除</el-button>
      </div>
    </div>

    <template #footer>
      <span class="muted" style="margin-right: 12px">已完成 {{ finished }} / {{ tasks.length }}</span>
      <el-button :disabled="running" @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="running" :disabled="!tasks.length" @click="start">开始上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { type UploadFile } from 'element-plus'
import { toast } from '@/utils/message'
import { uploadPhoto } from '@/api/photo'

const props = defineProps<{ albumId: number }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ done: [] }>()

const MAX_MB = 1
const CONCURRENCY = 3 // 同时上传张数，太多会拖慢服务器

type Status = 'waiting' | 'uploading' | 'success' | 'error'
interface Task { uid: number; file: File; name: string; progress: number; status: Status }

const tasks = ref<Task[]>([])
/** 最近一次失败原因，汇总提示里带上 */
let lastError = ''
const running = ref(false)
const finished = computed(() => tasks.value.filter((t) => t.status === 'success').length)

watch(visible, (v) => { if (v) tasks.value = [] })

function onChange(f: UploadFile) {
  if (!f.raw) return
  if (f.raw.size > MAX_MB * 1024 * 1024) {
    toast.error(`图片不可以超过 ${MAX_MB}M（${f.name}）`)
    return
  }
  tasks.value.push({ uid: f.uid, file: f.raw, name: f.name, progress: 0, status: 'waiting' })
}

function remove(t: Task) {
  tasks.value = tasks.value.filter((x) => x !== t)
}

async function runOne(t: Task) {
  t.status = 'uploading'
  try {
    await uploadPhoto(props.albumId, t.file, (p) => (t.progress = p))
    t.progress = 100
    t.status = 'success'
  } catch (e) {
    t.status = 'error'
    lastError = errorText(e)
  }
}

function errorText(e: unknown) {
  if (isAxiosError(e)) {
    if (e.response?.status === 413) return `图片超过 ${MAX_MB}M`
    return e.response?.data?.msg || (e.response ? '服务器错误' : '网络错误')
  }
  return e instanceof Error ? e.message : ''
}

/** 简单的并发队列：最多 CONCURRENCY 个同时上传，失败的可以再点一次重试 */
async function start() {
  const queue = tasks.value.filter((t) => t.status === 'waiting' || t.status === 'error')
  if (!queue.length) return
  running.value = true
  lastError = ''
  const worker = async () => {
    for (let t = queue.shift(); t; t = queue.shift()) await runOne(t)
  }
  await Promise.all(Array.from({ length: CONCURRENCY }, worker))
  running.value = false

  const failed = tasks.value.filter((t) => t.status === 'error').length
  if (failed) toast.warning(`${failed} 张上传失败${lastError ? `（${lastError}）` : ''}，可点击「开始上传」重试`)
  else toast.success('全部上传完成')
  emit('done')
}

const statusText: Record<Status, string> = { waiting: '等待中', uploading: '上传中', success: '完成', error: '失败' }
</script>

<style lang="scss" scoped>
.list { margin-top: 16px; max-height: 300px; overflow: auto; }
.row { display: flex; align-items: center; gap: 10px; padding: 6px 0; font-size: 13px; }
.name { width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar { flex: 1; }
.state { width: 48px; color: var(--text-muted); }
</style>

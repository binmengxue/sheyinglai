<template>
  <NavBar fallback="/me">
    <template #right>
      <a v-if="url" :href="url" :download="FILE_NAME">
        <el-button round size="small" :icon="'Download'">下载</el-button>
      </a>
    </template>
  </NavBar>

  <div class="page resume">
    <div ref="box" class="sheets">
      <div v-for="n in pages" :key="n" class="sheet">
        <canvas :ref="(el) => (canvases[n - 1] = el as HTMLCanvasElement)" />
      </div>
    </div>
    <div v-if="loading" class="sheet skeleton"><el-skeleton :rows="12" animated /></div>
    <el-empty v-if="failed" description="简历加载失败">
      <el-button round @click="load">重新加载</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { GlobalWorkerOptions, getDocument, type PDFDocumentProxy } from 'pdfjs-dist/legacy/build/pdf.mjs'
import PdfWorker from 'pdfjs-dist/legacy/build/pdf.worker.min.mjs?worker'
import NavBar from '@/components/NavBar.vue'
import { getResume } from '@/api/auth'
import { ApiError } from '@/utils/request'

// 用 ?worker 打包成 .js，避免线上 .mjs 的 MIME 类型问题
GlobalWorkerOptions.workerPort ??= new PdfWorker()

const FILE_NAME = '刘雪琴-高级前端工程师-简历.pdf'

const router = useRouter()
const box = ref<HTMLElement>()
const canvases: HTMLCanvasElement[] = []
const url = ref('')
const pages = ref(0)
const loading = ref(true)
const failed = ref(false)
let pdf: PDFDocumentProxy | null = null

async function load() {
  loading.value = true
  failed.value = false
  try {
    const blob = await getResume()
    if (url.value) URL.revokeObjectURL(url.value)
    url.value = URL.createObjectURL(blob)
    pdf = await getDocument({ data: new Uint8Array(await blob.arrayBuffer()) }).promise
    pages.value = pdf.numPages
    await nextTick()
    // 按 PC 宽度渲染，手机上缩小显示，高清屏也清晰
    const width = Math.max(box.value?.clientWidth ?? 0, 800) * Math.min(window.devicePixelRatio || 1, 2)
    for (let i = 1; i <= pdf.numPages; i++) {
      const page = await pdf.getPage(i)
      const viewport = page.getViewport({ scale: width / page.getViewport({ scale: 1 }).width })
      const canvas = canvases[i - 1]
      canvas.width = viewport.width
      canvas.height = viewport.height
      await page.render({ canvasContext: canvas.getContext('2d')!, viewport }).promise
      // 第一页出来就去掉占位
      loading.value = false
    }
  } catch (e) {
    // 非本人：接口已提示「无权查看」
    if (e instanceof ApiError && e.code === 403) return void router.replace('/me')
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)

onBeforeUnmount(() => {
  if (url.value) URL.revokeObjectURL(url.value)
  pdf?.destroy()
})
</script>

<style lang="scss" scoped>
.resume { max-width: 860px; margin: 0 auto; }
.sheets { display: flex; flex-direction: column; gap: 16px; }
.sheet { overflow: hidden; background: var(--card); border-radius: var(--radius); box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06); }
.sheet canvas { display: block; width: 100%; height: auto; }
.skeleton { aspect-ratio: 210 / 297; padding: 32px 24px; }
</style>

<template>
  <el-button link size="small" :icon="'CopyDocument'" aria-label="复制" @click="copy">复制</el-button>
</template>

<script setup lang="ts">
import { toast } from '@/utils/message'

const props = defineProps<{ text: string }>()

async function copy() {
  try {
    await navigator.clipboard.writeText(props.text)
  } catch {
    // 非 HTTPS 或旧浏览器没有 clipboard API，退回到选中文本复制
    const input = document.createElement('textarea')
    input.value = props.text
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    const ok = document.execCommand('copy')
    input.remove()
    if (!ok) return void toast.error('复制失败，请长按手动复制')
  }
  toast.success('已复制')
}
</script>

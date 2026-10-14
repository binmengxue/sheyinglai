<template>
  <el-button link size="small" class="report-btn" :class="{ dark }" :icon="'Warning'" @click="open">举报</el-button>
  <el-dialog v-model="visible" :title="`举报${TARGET_LABEL[targetType]}`" width="420px" align-center append-to-body>
    <el-radio-group v-model="reason" class="reasons">
      <el-radio v-for="r in REPORT_REASONS" :key="r" :value="r">{{ r }}</el-radio>
    </el-radio-group>
    <el-input v-model="content" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="补充说明（选填），方便我们核实" />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="cta" :loading="saving" :disabled="!reason" @click="submit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createReport, REPORT_REASONS } from '@/api/report'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import type { ReportTargetType } from '@/types'

const props = defineProps<{ targetType: ReportTargetType; targetId: number; dark?: boolean }>()
const TARGET_LABEL: Record<ReportTargetType, string> = { PHOTOGRAPHER: '摄影师', REVIEW: '评价', ALBUM: '客片' }

const user = useUserStore()
const router = useRouter()
const visible = ref(false)
const reason = ref('')
const content = ref('')
const saving = ref(false)

function open() {
  if (!user.loggedIn) {
    toast.info('请先登录')
    return void router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
  reason.value = ''
  content.value = ''
  visible.value = true
}

async function submit() {
  saving.value = true
  try {
    await createReport({ targetType: props.targetType, targetId: props.targetId, reason: reason.value, content: content.value || undefined })
    visible.value = false
    toast.success('已收到你的举报，我们会尽快核实处理')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.report-btn { color: var(--text-muted); }
.report-btn.dark { color: rgba(255, 255, 255, 0.75); }
.reasons { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px 0; margin-bottom: 12px; }
</style>

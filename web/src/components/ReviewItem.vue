<template>
  <div class="review">
    <component :is="linkUser ? RouterLink : 'div'" :to="linkUser ? `/p/${r.user.id}` : undefined" class="who">
      <el-avatar :size="32" :src="r.user.avatarUrl">{{ r.user.nickname.slice(0, 1) }}</el-avatar>
      <span class="name">{{ r.user.nickname }}</span>
      <el-rate :model-value="r.review.score" disabled size="small" />
    </component>
    <p class="content">{{ r.review.content || '用户没有填写评价内容' }}</p>
    <div class="foot">
      <span class="muted">{{ fmtDateTime(r.review.createdAt) }}</span>
      <slot>
        <span class="ops">
          <ReportButton v-if="user.user?.id !== r.review.customerId" target-type="REVIEW" :target-id="r.review.id" />
          <el-button v-if="user.isAdmin" link size="small" type="danger" @click="remove">删除</el-button>
        </span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ReportButton from '@/components/ReportButton.vue'
import { deleteReview } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { fmtDateTime } from '@/utils/format'
import { confirm, singleFlight, toast } from '@/utils/message'
import type { ReviewView } from '@/types'

const props = defineProps<{ r: ReviewView; linkUser?: boolean }>()
const emit = defineEmits<{ deleted: [id: number] }>()
const user = useUserStore()

/** 管理员删除违规评价，删除后摄影师评分会重新计算 */
const remove = singleFlight(async () => {
  if (!(await confirm('删除这条评价？删除后摄影师的评分会重新计算'))) return
  await deleteReview(props.r.review.id)
  toast.success('评价已删除')
  emit('deleted', props.r.review.id)
})
</script>

<style lang="scss" scoped>
.review { padding: 14px 0; border-bottom: 1px solid #f0f0f0; }
.review:last-child { border-bottom: none; }
.who { display: flex; align-items: center; gap: 8px; }
.name { font-weight: 600; font-size: 14px; }
.content { margin: 8px 0 6px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.foot { display: flex; align-items: center; justify-content: space-between; }
.ops { display: flex; align-items: center; gap: 4px; }
</style>

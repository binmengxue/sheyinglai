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
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { fmtDateTime } from '@/utils/format'
import type { ReviewView } from '@/types'

defineProps<{ r: ReviewView; linkUser?: boolean }>()
</script>

<style lang="scss" scoped>
.review { padding: 14px 0; border-bottom: 1px solid #f0f0f0; }
.review:last-child { border-bottom: none; }
.who { display: flex; align-items: center; gap: 8px; }
.name { font-weight: 600; font-size: 14px; }
.content { margin: 8px 0 6px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.foot { display: flex; align-items: center; justify-content: space-between; }
</style>

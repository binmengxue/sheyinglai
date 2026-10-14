<template>
  <RouterLink :to="`/albums/${a.id}`" class="acard">
    <div class="cover">
      <img v-if="a.coverUrl" :src="a.coverUrl" :alt="a.title" loading="lazy" />
      <el-icon v-else :size="36" color="#ccc"><Picture /></el-icon>
      <span v-if="a.location" class="loc ellipsis"><el-icon><Location /></el-icon>{{ a.location }}</span>
      <el-tag v-if="!a.visible" class="hidden" size="small" type="info">未公开</el-tag>
    </div>
    <div class="info">
      <div class="title ellipsis">{{ a.title }}</div>
      <div class="meta">
        <span class="muted">{{ a.style || '其它' }} · {{ a.photoCount ?? 0 }}张</span>
        <button class="like" :class="{ on: a.liked }" :aria-label="a.liked ? '取消喜欢' : '喜欢'" @click.prevent="toggle">
          <el-icon><component :is="a.liked ? 'StarFilled' : 'Star'" /></el-icon>{{ a.likeCount || '' }}
        </button>
      </div>
    </div>
  </RouterLink>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { likeAlbum, unlikeAlbum } from '@/api/album'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import type { Album } from '@/types'

const props = defineProps<{ a: Album }>()
const user = useUserStore()
const router = useRouter()

let pending = false

/** 喜欢 / 取消喜欢，直接改 props 里的对象让列表同步 */
async function toggle() {
  if (!user.loggedIn) {
    toast.info('请先登录')
    return router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
  if (pending) return
  pending = true
  const a = props.a
  try {
    if (a.liked) {
      await unlikeAlbum(a.id)
      a.likeCount = Math.max(0, a.likeCount - 1)
    } else {
      await likeAlbum(a.id)
      a.likeCount++
    }
    a.liked = !a.liked
    toast.success(a.liked ? '已喜欢' : '已取消喜欢')
  } finally {
    pending = false
  }
}
</script>

<style lang="scss" scoped>
.acard { display: block; background: var(--card); border-radius: 12px; overflow: hidden; }
.cover { position: relative; aspect-ratio: 3 / 4; background: #f0f0f0; display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.loc {
  position: absolute; left: 0; right: 0; bottom: 0; display: flex; align-items: center; gap: 3px;
  padding: 18px 8px 6px; font-size: 12px; color: #fff; background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
}
.hidden { position: absolute; top: 6px; left: 6px; }
.info { padding: 8px 10px 10px; }
.title { font-weight: 600; font-size: 14px; }
.meta { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
.like { display: flex; align-items: center; gap: 2px; border: none; background: none; color: #999; cursor: pointer; font-size: 13px; padding: 0; }
.like.on { color: #f5a623; }
</style>

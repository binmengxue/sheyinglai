<template>
  <div class="page-title">
    <h1>最新作品</h1>
    <span class="muted">共 {{ total }} 张</span>
  </div>
  <PhotoGrid :photos="photos" />
  <div class="more">
    <el-button v-if="photos.length < total" :loading="loading" @click="loadMore">加载更多</el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PhotoGrid from '@/components/PhotoGrid.vue'
import { listPhotos } from '@/api/photo'
import type { Photo } from '@/types'

const PAGE_SIZE = 30
const photos = ref<Photo[]>([])
const page = ref(0)
const total = ref(0)
const loading = ref(false)

async function loadMore() {
  loading.value = true
  try {
    const res = await listPhotos({ page: page.value + 1, size: PAGE_SIZE })
    photos.value.push(...res.records)
    total.value = res.total
    page.value++
  } finally {
    loading.value = false
  }
}

onMounted(loadMore)
</script>

<style lang="scss" scoped>
.more { text-align: center; margin-top: 24px; }
</style>

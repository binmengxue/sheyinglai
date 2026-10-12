<template>
  <NavBar fallback="/me">
    <template #right>
      <el-button class="cta" round size="small" :icon="'Plus'" @click="openDialog(null)">新建</el-button>
    </template>
  </NavBar>
  <div class="page">
    <p class="muted tip">一组客片就是一次拍摄的作品集，填好类型和地点，客户在你的主页可以按类型筛选</p>
    <div v-loading="loading" class="albums">
      <RouterLink v-for="a in albums" :key="a.id" :to="`/albums/${a.id}`" class="item">
        <div class="cover">
          <img v-if="a.coverUrl" :src="a.coverUrl" :alt="a.title" loading="lazy" />
          <el-icon v-else :size="40" color="#ccc"><Picture /></el-icon>
          <el-tag v-if="!a.visible" class="tag" size="small" type="info">未公开</el-tag>
        </div>
        <div class="info">
          <div class="title ellipsis">{{ a.title }}</div>
          <div class="muted">{{ a.style || '未分类' }} · {{ a.photoCount ?? 0 }} 张 · {{ a.likeCount }} 喜欢</div>
        </div>
        <div class="ops" @click.prevent>
          <el-button link :icon="'Edit'" @click="openDialog(a)">编辑</el-button>
          <el-button link type="danger" :icon="'Delete'" @click="remove(a)">删除</el-button>
        </div>
      </RouterLink>
    </div>
    <el-empty v-if="!loading && !albums.length" description="还没有客片，新建一组上传作品吧" />
  </div>

  <AlbumDialog v-model="dialog" :album="editing" @saved="load" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import AlbumDialog from '@/components/AlbumDialog.vue'
import { deleteAlbum, myAlbums } from '@/api/album'
import type { Album } from '@/types'

const albums = ref<Album[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<Album | null>(null)

async function load() {
  loading.value = true
  try {
    albums.value = await myAlbums()
  } finally {
    loading.value = false
  }
}

function openDialog(a: Album | null) {
  editing.value = a
  dialog.value = true
}

async function remove(a: Album) {
  const ok = await ElMessageBox.confirm(`删除「${a.title}」及其中 ${a.photoCount ?? 0} 张照片？此操作不可恢复`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
  }).catch(() => false)
  if (!ok) return
  await deleteAlbum(a.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style lang="scss" scoped>
.tip { margin: 0 0 12px; }
.albums { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; min-height: 100px; }
.item { background: #fff; border-radius: 12px; overflow: hidden; }
.cover { position: relative; aspect-ratio: 4 / 3; background: #f0f0f0; display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.tag { position: absolute; top: 8px; left: 8px; }
.info { padding: 10px 12px 4px; }
.title { font-weight: 600; margin-bottom: 4px; }
.ops { padding: 0 8px 8px; }
@media (min-width: 768px) { .albums { grid-template-columns: repeat(4, 1fr); gap: 14px; } }
</style>

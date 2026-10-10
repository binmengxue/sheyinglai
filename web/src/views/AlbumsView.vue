<template>
  <div class="page-title">
    <h1>相册</h1>
    <el-button v-if="user.loggedIn" type="primary" :icon="'Plus'" @click="openDialog(null)">新建相册</el-button>
  </div>

  <div v-loading="loading" class="albums">
    <RouterLink v-for="a in albums" :key="a.id" :to="`/albums/${a.id}`" class="card">
      <div class="cover">
        <img v-if="a.coverUrl" :src="a.coverUrl" :alt="a.title" loading="lazy" />
        <el-icon v-else :size="40" color="#ccc"><Picture /></el-icon>
        <el-tag v-if="!a.visible" class="tag" size="small" type="info">未公开</el-tag>
      </div>
      <div class="info">
        <div class="title">{{ a.title }}</div>
        <div class="muted">{{ a.photoCount ?? 0 }} 张</div>
      </div>
      <div v-if="user.loggedIn" class="ops" @click.prevent>
        <el-button link :icon="'Edit'" @click="openDialog(a)">编辑</el-button>
        <el-button link type="danger" :icon="'Delete'" @click="remove(a)">删除</el-button>
      </div>
    </RouterLink>
  </div>
  <el-empty v-if="!loading && !albums.length" description="还没有相册" />

  <AlbumDialog v-model="dialog" :album="editing" @saved="load" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import AlbumDialog from '@/components/AlbumDialog.vue'
import { deleteAlbum, listAlbums } from '@/api/album'
import { useUserStore } from '@/stores/user'
import type { Album } from '@/types'

const user = useUserStore()
const albums = ref<Album[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<Album | null>(null)

async function load() {
  loading.value = true
  try {
    albums.value = await listAlbums()
  } finally {
    loading.value = false
  }
}

function openDialog(a: Album | null) {
  editing.value = a
  dialog.value = true
}

async function remove(a: Album) {
  const ok = await ElMessageBox.confirm(`删除相册「${a.title}」及其中 ${a.photoCount ?? 0} 张照片？此操作不可恢复`, '确认删除', {
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
.albums { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; min-height: 100px; }
.card { background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06); transition: transform 0.2s, box-shadow 0.2s; }
.card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08); }
.cover { position: relative; aspect-ratio: 4 / 3; background: #f0f0f0; display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.tag { position: absolute; top: 8px; left: 8px; }
.info { padding: 12px 14px 4px; }
.title { font-weight: 600; margin-bottom: 4px; }
.ops { padding: 0 8px 8px; }
</style>

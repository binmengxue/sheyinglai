<template>
  <template v-if="album">
    <div class="page-title">
      <div>
        <el-page-header @back="$router.push('/albums')">
          <template #content>
            <span class="title">{{ album.title }}</span>
            <el-tag v-if="!album.visible" size="small" type="info" style="margin-left: 8px">未公开</el-tag>
          </template>
        </el-page-header>
        <p v-if="album.description" class="muted desc">{{ album.description }}</p>
      </div>
      <div v-if="user.loggedIn" class="actions">
        <el-button :icon="'Edit'" @click="editVisible = true">编辑</el-button>
        <el-button type="primary" :icon="'Upload'" @click="uploadVisible = true">上传照片</el-button>
      </div>
    </div>

    <PhotoGrid :photos="photos" :editable="user.loggedIn" @delete="onDelete" @cover="onCover" />
    <div class="more">
      <el-button v-if="photos.length < total" :loading="loading" @click="loadPhotos()">加载更多</el-button>
    </div>

    <template v-if="user.loggedIn">
      <UploadDialog v-model="uploadVisible" :album-id="album.id" @done="loadPhotos(true)" />
      <AlbumDialog v-model="editVisible" :album="album" @saved="(a) => (album = { ...album!, ...a })" />
    </template>
  </template>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PhotoGrid from '@/components/PhotoGrid.vue'
import UploadDialog from '@/components/UploadDialog.vue'
import AlbumDialog from '@/components/AlbumDialog.vue'
import { getAlbum, setAlbumCover } from '@/api/album'
import { deletePhoto, listPhotos } from '@/api/photo'
import { useUserStore } from '@/stores/user'
import type { Album, Photo } from '@/types'

const props = defineProps<{ id: string }>()
const user = useUserStore()

const PAGE_SIZE = 40
const album = ref<Album | null>(null)
const photos = ref<Photo[]>([])
const page = ref(0)
const total = ref(0)
const loading = ref(false)
const uploadVisible = ref(false)
const editVisible = ref(false)

async function loadPhotos(reset = false) {
  if (reset) {
    photos.value = []
    page.value = 0
  }
  loading.value = true
  try {
    const res = await listPhotos({ albumId: Number(props.id), page: page.value + 1, size: PAGE_SIZE })
    photos.value.push(...res.records)
    total.value = res.total
    page.value++
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  album.value = await getAlbum(Number(props.id))
  await loadPhotos(true)
}

async function onDelete(p: Photo) {
  const ok = await ElMessageBox.confirm('确定删除这张照片？', '提示', { type: 'warning' }).catch(() => false)
  if (!ok) return
  await deletePhoto(p.id)
  photos.value = photos.value.filter((x) => x.id !== p.id)
  total.value--
  ElMessage.success('已删除')
}

async function onCover(p: Photo) {
  await setAlbumCover(Number(props.id), p.id)
  ElMessage.success('已设为封面')
}

watch(() => props.id, loadAll, { immediate: true })
</script>

<style lang="scss" scoped>
.title { font-size: 20px; font-weight: 600; }
.desc { margin: 10px 0 0; }
.actions { display: flex; gap: 8px; flex-shrink: 0; }
.more { text-align: center; margin-top: 24px; }
</style>

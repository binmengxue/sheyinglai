<template>
  <NavBar :title="album?.title ?? ''" :fallback="album ? `/p/${album.userId}` : '/'" />
  <div v-if="album" class="page">
    <div class="card head">
      <div class="meta">
        <el-tag v-if="album.style" effect="plain" round>{{ album.style }}</el-tag>
        <el-tag v-if="!album.visible" size="small" type="info">未公开</el-tag>
        <span v-if="album.location" class="muted loc"><el-icon><Location /></el-icon>{{ album.location }}</span>
      </div>
      <p v-if="album.description" class="desc">{{ album.description }}</p>
      <div class="actions">
        <RouterLink :to="`/p/${album.userId}`" class="link">查看摄影师主页 <el-icon><ArrowRight /></el-icon></RouterLink>
        <ReportButton v-if="!canManage" target-type="ALBUM" :target-id="album.id" />
        <template v-if="canManage">
          <el-button round size="small" :icon="'Edit'" @click="editVisible = true">编辑</el-button>
          <el-button round size="small" class="cta" :icon="'Upload'" @click="uploadVisible = true">上传照片</el-button>
        </template>
      </div>
    </div>

    <PhotoGrid :photos="photos" :editable="canManage" @delete="onDelete" @cover="onCover" />
    <div class="more">
      <el-button v-if="photos.length < total" round :loading="loading" @click="loadPhotos()">加载更多</el-button>
    </div>

    <template v-if="canManage">
      <UploadDialog v-model="uploadVisible" :album-id="album.id" @done="loadPhotos(true)" />
      <AlbumDialog v-model="editVisible" :album="album" @saved="(a) => (album = { ...album!, ...a })" />
    </template>
  </div>
  <el-empty v-else-if="error" description="加载失败"><el-button round @click="loadAll">重试</el-button></el-empty>
  <div v-else v-loading="true" style="height: 60vh" />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { toast, confirm, singleFlight } from '@/utils/message'
import NavBar from '@/components/NavBar.vue'
import PhotoGrid from '@/components/PhotoGrid.vue'
import UploadDialog from '@/components/UploadDialog.vue'
import AlbumDialog from '@/components/AlbumDialog.vue'
import ReportButton from '@/components/ReportButton.vue'
import { getAlbum, setAlbumCover } from '@/api/album'
import { deletePhoto, listPhotos } from '@/api/photo'
import { useUserStore } from '@/stores/user'
import { usePagedList } from '@/composables/usePagedList'
import { setTitle } from '@/utils/title'
import type { Album, Photo } from '@/types'

const props = defineProps<{ id: string }>()
const user = useUserStore()

const PAGE_SIZE = 40
const album = ref<Album | null>(null)
const error = ref(false)
const uploadVisible = ref(false)
const editVisible = ref(false)
/** 本人或管理员可以管理 */
const canManage = computed(() => !!album.value && (user.user?.id === album.value.userId || user.isAdmin))

const { list: photos, total, loading, load: loadPhotos } = usePagedList<Photo>((page) =>
  listPhotos({ albumId: Number(props.id), page, size: PAGE_SIZE }))

async function loadAll() {
  album.value = null
  error.value = false
  try {
    album.value = await getAlbum(Number(props.id))
    setTitle(album.value.title)
  } catch {
    error.value = true
    return
  }
  await loadPhotos(true)
}

const onDelete = singleFlight(async (p: Photo) => {
  const ok = await confirm('确定删除这张照片？')
  if (!ok) return
  await deletePhoto(p.id)
  photos.value = photos.value.filter((x) => x.id !== p.id)
  total.value--
  toast.success('已删除')
})

const onCover = singleFlight(async (p: Photo) => {
  await setAlbumCover(Number(props.id), p.id)
  toast.success('已设为封面')
})

watch(() => props.id, loadAll, { immediate: true })
</script>

<style lang="scss" scoped>
.head { margin-bottom: 12px; }
.meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.loc { display: inline-flex; align-items: center; gap: 2px; }
.desc { margin: 10px 0 0; line-height: 1.6; white-space: pre-wrap; }
.actions { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.link { display: inline-flex; align-items: center; margin-right: auto; font-size: 14px; color: var(--link); }
.more { text-align: center; margin-top: 16px; }
</style>

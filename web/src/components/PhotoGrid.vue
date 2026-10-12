<template>
  <div v-if="photos.length" class="grid">
    <div v-for="(p, i) in photos" :key="p.id" class="item">
      <el-image
        :src="p.thumbUrl"
        :alt="p.title"
        :preview-src-list="previewList"
        :initial-index="i"
        preview-teleported
        hide-on-click-modal
        lazy
        fit="cover"
        class="img"
        :style="p.width && p.height ? { aspectRatio: `${p.width} / ${p.height}` } : undefined"
      />
      <div v-if="editable" class="ops">
        <el-tooltip content="设为封面"><el-button circle size="small" :icon="'Picture'" @click="emit('cover', p)" /></el-tooltip>
        <el-tooltip content="删除"><el-button circle size="small" type="danger" :icon="'Delete'" @click="emit('delete', p)" /></el-tooltip>
      </div>
    </div>
  </div>
  <el-empty v-else description="还没有照片" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Photo } from '@/types'

const props = defineProps<{ photos: Photo[]; editable?: boolean }>()
const emit = defineEmits<{ delete: [photo: Photo]; cover: [photo: Photo] }>()

// 点开预览时看大图，列表里只加载缩略图，省流量
const previewList = computed(() => props.photos.map((p) => p.url))
</script>

<style lang="scss" scoped>
/* CSS 多列实现瀑布流，保持照片原始比例；列数按容器宽度自动算（手机 1 列，PC 宽屏 4 列） */
.grid { column-width: 260px; column-gap: 12px; }
.item { position: relative; break-inside: avoid; margin-bottom: 12px; border-radius: 6px; overflow: hidden; background: #eee; }
.img { display: block; width: 100%; cursor: zoom-in; }
.ops { position: absolute; top: 8px; right: 8px; display: flex; gap: 6px; opacity: 0; transition: opacity 0.2s; }
.item:hover .ops { opacity: 1; }
@media (max-width: 500px) { .ops { opacity: 1; } }
</style>

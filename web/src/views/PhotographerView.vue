<template>
  <div v-if="d" class="profile" :class="{ 'has-bar': !d.self && ready }">
    <header class="hero">
      <NavBar dark title="" fallback="/">
        <template #right>
          <ReportButton v-if="!d.self" target-type="PHOTOGRAPHER" :target-id="Number(id)" dark />
        </template>
      </NavBar>
      <div class="head">
        <el-avatar :size="84" :src="c.avatarUrl" class="avatar">{{ c.nickname.slice(0, 1) }}</el-avatar>
        <div class="who">
          <h1 class="ellipsis">{{ c.nickname }}</h1>
          <div class="uid">UID：{{ c.id }}</div>
          <div class="line">
            <span v-if="genderMark(c.gender)" class="gender" :class="{ male: c.gender === 1 }">{{ genderMark(c.gender) }}</span>
            <span v-if="d.city">{{ d.city }}</span>
            <span v-if="c.refundPromise" class="promise"><el-icon><CircleCheckFilled /></el-icon> 不满意全退</span>
          </div>
        </div>
      </div>
      <div v-if="ready" class="ranks">
        <span class="rank"><el-icon><Camera /></el-icon> 拍照第{{ d.rank }}名</span>
        <span v-if="c.tag" class="rank">{{ c.tag }}</span>
      </div>
      <p class="bio" :class="{ open: bioOpen }" @click="bioOpen = !bioOpen">
        {{ d.bio || (d.self ? '介绍自己更容易获得关注哦' : '这个人很懒，还没有写简介') }}
      </p>

      <div class="entries">
        <button class="entry" @click="priceVisible = true" :disabled="!ready">
          <b><el-icon><Discount /></el-icon> 收费详情</b><span>费用、设备、精修</span>
        </button>
        <RouterLink :to="`/p/${id}/schedule`" class="entry">
          <b><el-icon><Calendar /></el-icon> {{ d.self ? '我的档期' : 'TA的档期' }}</b><span>查看接单时间</span>
        </RouterLink>
        <RouterLink v-if="d.self" to="/studio/stats" class="entry">
          <b><el-icon><DataLine /></el-icon> 我的数据</b><span>我的经营数据</span>
        </RouterLink>
      </div>

      <div class="stats">
        <div><b>{{ c.ratingCount ? Number(c.ratingAvg).toFixed(1) : '-' }}</b><span>评分</span></div>
        <RouterLink :to="`/p/${id}/follows?tab=fans`"><b>{{ d.fans }}</b><span>粉丝</span></RouterLink>
        <RouterLink :to="`/p/${id}/follows?tab=following`"><b>{{ d.following }}</b><span>关注</span></RouterLink>
        <div class="ops">
          <template v-if="d.self">
            <el-button round plain class="ghost" @click="$router.push('/studio/profile')">接单设置</el-button>
            <el-button round class="cta" @click="$router.push('/studio/albums')">管理客片</el-button>
          </template>
          <template v-else>
            <el-button round plain class="ghost" :icon="d.followed ? 'Check' : 'Plus'" :loading="following" @click="toggleFollow">
              {{ d.followed ? '已关注' : '关注' }}
            </el-button>
            <!-- PC：预约入口放在资料区，不用底部浮动栏 -->
            <template v-if="ready">
              <span class="desk-price desktop-only">¥<b>{{ c.pricePerHour }}</b>/小时 · {{ c.minHours }}小时起</span>
              <el-button class="cta desktop-only" round @click="$router.push(`/p/${id}/schedule`)">立即预约</el-button>
            </template>
          </template>
        </div>
      </div>
    </header>

    <section class="sheet">
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="客片" name="albums" />
        <el-tab-pane :label="`评价(${c.ratingCount})`" name="reviews" />
      </el-tabs>

      <template v-if="tab === 'albums'">
        <div class="chips">
          <button class="chip" :class="{ active: !style }" @click="setStyle('')">全部{{ allCount }}</button>
          <button v-for="s in d.styleCounts" :key="s.style" class="chip" :class="{ active: style === s.style }" @click="setStyle(s.style)">
            {{ s.style }}{{ s.count }}
          </button>
        </div>
        <div v-loading="albumsLoading" class="grid">
          <AlbumCard v-for="a in albums" :key="a.id" :a="a" />
        </div>
        <el-empty v-if="!albumsLoading && !albums.length" description="还没有客片" />
      </template>

      <template v-else>
        <ReviewItem v-for="r in reviews" :key="r.review.id" :r="r" @deleted="onReviewDeleted" />
        <el-empty v-if="!reviews.length" description="还没有评价" />
        <div v-if="reviews.length < reviewTotal" class="more"><el-button round @click="loadReviews()">加载更多</el-button></div>
      </template>
    </section>

    <div v-if="!d.self && ready" class="bar mobile-only">
      <div class="p">
        <span class="price">¥<b>{{ c.pricePerHour }}</b><small>/小时</small></span>
        <span class="min">{{ c.minHours }}小时起</span>
      </div>
      <el-button class="cta book" round size="large" @click="$router.push(`/p/${id}/schedule`)">立即预约</el-button>
    </div>
    <el-alert v-if="!ready" class="notice" type="info" :closable="false" show-icon
      :title="d.self ? '你还没有设置价格，设置后才会出现在首页' : 'TA 还没有开放预约'" />

    <PriceDetailDrawer v-if="ready" v-model="priceVisible" :p="d.profile" />
  </div>
  <template v-else-if="error">
    <NavBar title="" fallback="/" />
    <el-empty description="加载失败"><el-button round @click="load">重试</el-button></el-empty>
  </template>
  <div v-else v-loading="true" style="height: 60vh" />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import AlbumCard from '@/components/AlbumCard.vue'
import ReviewItem from '@/components/ReviewItem.vue'
import ReportButton from '@/components/ReportButton.vue'
import PriceDetailDrawer from '@/components/PriceDetailDrawer.vue'
import { follow, getPhotographer, listReviews, unfollow } from '@/api/photographer'
import { listAlbums } from '@/api/album'
import { useUserStore } from '@/stores/user'
import { genderMark, peekLocation } from '@/utils/format'
import { usePagedList } from '@/composables/usePagedList'
import { setTitle } from '@/utils/title'
import { toast } from '@/utils/message'
import type { Album, PhotographerDetail, ReviewView } from '@/types'

const props = defineProps<{ id: string }>()
const user = useUserStore()
const router = useRouter()

const d = ref<PhotographerDetail | null>(null)
const error = ref(false)
const c = computed(() => d.value!.card)
const ready = computed(() => !!d.value?.profile.pricePerHour)
const allCount = computed(() => d.value?.styleCounts.reduce((s, x) => s + x.count, 0) ?? 0)
const tab = ref<'albums' | 'reviews'>('albums')
const bioOpen = ref(false)
const priceVisible = ref(false)

const style = ref('')
const albums = ref<Album[]>([])
const albumsLoading = ref(false)
const { list: reviews, total: reviewTotal, load: loadReviews } = usePagedList<ReviewView>((page) =>
  listReviews(Number(props.id), page))

function onReviewDeleted(id: number) {
  reviews.value = reviews.value.filter((x) => x.review.id !== id)
  reviewTotal.value--
}

async function load() {
  const id = Number(props.id)
  d.value = null
  error.value = false
  tab.value = 'albums'
  style.value = ''
  try {
    // 主页不显示距离，不等定位（以前最多会卡 6 秒）；有缓存的定位就顺便带上
    d.value = await getPhotographer(id, peekLocation() ?? null)
    setTitle(`${d.value.card.nickname}的主页`)
  } catch {
    error.value = true
    return
  }
  loadAlbums()
  loadReviews(true)
}

let albumsSeq = 0
async function loadAlbums() {
  // 快速切换类型时只认最后一次请求
  const mine = ++albumsSeq
  albumsLoading.value = true
  try {
    const res = await listAlbums(Number(props.id), style.value || undefined)
    if (mine === albumsSeq) albums.value = res
  } finally {
    if (mine === albumsSeq) albumsLoading.value = false
  }
}

function setStyle(s: string) {
  style.value = s
  loadAlbums()
}

const following = ref(false)

async function toggleFollow() {
  if (!user.loggedIn) {
    toast.info('请先登录')
    return router.push({ path: '/login', query: { redirect: `/p/${props.id}` } })
  }
  if (following.value) return
  const v = d.value!
  following.value = true
  try {
    if (v.followed) {
      await unfollow(v.card.id)
      v.fans--
    } else {
      await follow(v.card.id)
      v.fans++
    }
    v.followed = !v.followed
    toast.success(v.followed ? '已关注' : '已取消关注')
  } finally {
    following.value = false
  }
}

watch(() => props.id, load, { immediate: true })
</script>

<style lang="scss" scoped>
.profile.has-bar { padding-bottom: 90px; }
.hero { padding: 0 0 36px; background: radial-gradient(circle at 80% 0, #3b3f46, #121315 70%); color: #fff; }
.head { display: flex; align-items: center; gap: 16px; padding: 6px 20px 0; }
.avatar { border: 3px solid #fff; flex-shrink: 0; font-size: 28px; }
.who { min-width: 0; }
h1 { margin: 0; font-size: 24px; }
.uid { margin-top: 4px; font-size: 13px; opacity: 0.75; letter-spacing: 1px; }
.line { display: flex; align-items: center; gap: 10px; margin-top: 6px; font-size: 14px; }
.gender { color: #ff7aa2; }
.gender.male { color: #7aa8ff; }
.promise { display: inline-flex; align-items: center; gap: 2px; color: #4cd69a; }
.ranks { display: flex; gap: 8px; padding: 12px 20px 0; }
.rank { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; padding: 3px 10px; border-radius: 8px; background: rgba(255, 255, 255, 0.12); }
.bio { margin: 12px 20px 0; font-size: 15px; line-height: 1.6; opacity: 0.92; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; cursor: pointer; }
.bio.open { -webkit-line-clamp: unset; }
.entries { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 16px 16px 0; }
.entry { display: flex; flex-direction: column; gap: 4px; padding: 12px; border-radius: 12px; border: none; text-align: left; color: #fff; background: rgba(255, 255, 255, 0.1); cursor: pointer; font: inherit; }
.entry b { display: flex; align-items: center; gap: 4px; font-size: 15px; }
.entry span { font-size: 12px; opacity: 0.6; }
.entry:disabled { opacity: 0.5; cursor: default; }
.stats { display: flex; align-items: center; gap: 22px; padding: 18px 20px 0; }
.stats > :not(.ops) { display: flex; flex-direction: column; }
.stats b { font-size: 22px; }
.stats span { font-size: 12px; opacity: 0.6; }
.ops { margin-left: auto; display: flex; gap: 8px; }
.ghost.el-button { --el-button-bg-color: transparent; --el-button-text-color: #fff; --el-button-border-color: #fff; --el-button-hover-bg-color: rgba(255, 255, 255, 0.1); --el-button-hover-text-color: #fff; --el-button-hover-border-color: #fff; }
.sheet { position: relative; margin-top: -20px; min-height: 50vh; padding: 4px 12px 20px; background: #fff; border-radius: 20px 20px 0 0; }
.sheet .chips { margin-bottom: 12px; }
.sheet .chip { background: #f4f5f7; }
.sheet .chip.active { background: var(--brand); }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; min-height: 80px; }
.more { text-align: center; padding: 12px; }
.bar {
  position: fixed; left: 50%; bottom: calc(12px + env(safe-area-inset-bottom)); transform: translateX(-50%); z-index: 20;
  width: calc(100% - 24px); max-width: calc(var(--app-width) - 24px);
  display: flex; align-items: center; gap: 12px; padding: 10px 10px 10px 18px; border-radius: 18px; background: #1b1c1e; color: #fff;
}
.p { display: flex; flex-direction: column; }
.p .price { color: #ff5a4f; font-size: 14px; }
.p .price b { font-size: 26px; }
.min { font-size: 12px; opacity: 0.7; }
.book { flex: 1; height: 46px; font-size: 18px; }
.notice { margin: 12px; width: auto; }
.desk-price { align-self: center; margin: 0 4px 0 12px; color: #ffb0a8; font-size: 13px; }
.desk-price b { font-size: 22px; color: #ff6a5f; }

/* PC：资料区圆角卡片，客片 4 列，不需要给底部浮动栏留位置 */
@media (min-width: 768px) {
  .profile.has-bar { padding-bottom: 0; }
  .hero { border-radius: var(--radius) var(--radius) 0 0; padding: 16px 12px 36px; }
  .entries { grid-template-columns: repeat(3, 220px); }
  .sheet { padding: 4px 20px 24px; }
  .grid { grid-template-columns: repeat(4, 1fr); gap: 14px; }
}
</style>

<template>
  <div v-if="u" class="me">
    <header class="hero">
      <div class="head">
        <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png,image/webp" :on-change="onAvatar">
          <el-avatar :size="84" :src="u.avatarUrl" class="avatar">{{ u.nickname.slice(0, 1) }}</el-avatar>
        </el-upload>
        <div class="who">
          <h1 class="ellipsis">{{ u.nickname }} <span v-if="user.isAdmin" class="admin">管理员</span></h1>
          <div class="uid">UID：{{ u.id }}</div>
          <div v-if="genderMark(u.gender)" class="gender" :class="{ male: u.gender === 1 }">{{ genderMark(u.gender) }}</div>
        </div>
      </div>
      <p class="bio">{{ u.bio || '介绍自己更容易获得关注哦' }}</p>

      <div class="stats">
        <div><b>{{ fans }}</b><span>粉丝</span></div>
        <div><b>{{ following }}</b><span>关注</span></div>
        <div class="ops">
          <el-dropdown trigger="click" @command="onMore">
            <el-button round plain class="ghost" :icon="'Setting'" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button round plain class="ghost" @click="$router.push('/me/edit')">编辑资料</el-button>
        </div>
      </div>

      <!-- PC 上左侧菜单已有这些入口 -->
      <div class="entries mobile-only">
        <RouterLink to="/messages" class="entry">
          <b>
            <el-icon><Bell /></el-icon> 消息
            <el-badge v-if="notification.unread" :value="notification.unread" :max="99" class="count" />
          </b>
          <span>预约、评价、关注通知</span>
        </RouterLink>
        <RouterLink to="/studio/profile" class="entry">
          <b><el-icon><Camera /></el-icon> {{ user.isPhotographerReady ? '接单设置' : '我要接单' }}</b>
          <span>{{ user.isPhotographerReady ? '收费、档期、规则' : '设置价格后出现在首页' }}</span>
        </RouterLink>
        <RouterLink to="/studio/albums" class="entry">
          <b><el-icon><Picture /></el-icon> 我的客片</b><span>上传作品吸引客户</span>
        </RouterLink>
        <RouterLink :to="`/p/${u.id}`" class="entry">
          <b><el-icon><User /></el-icon> 我的主页</b><span>看看客户眼中的我</span>
        </RouterLink>
        <RouterLink to="/studio/stats" class="entry">
          <b><el-icon><DataLine /></el-icon> 经营数据</b><span>曝光、浏览、咨询</span>
        </RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" class="entry">
          <b><el-icon><Management /></el-icon> 管理后台</b><span>用户和内容管理</span>
        </RouterLink>
      </div>
    </header>

    <section class="sheet">
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="喜欢" name="likes" />
        <el-tab-pane :label="`点评(${reviewTotal})`" name="reviews" />
      </el-tabs>
      <template v-if="tab === 'likes'">
        <div class="grid"><AlbumCard v-for="a in likes" :key="a.id" :a="a" /></div>
        <el-empty v-if="!likes.length" description="Oops！还没有喜欢的客片" />
      </template>
      <template v-else>
        <ReviewItem v-for="r in reviews" :key="r.review.id" :r="r" link-user />
        <el-empty v-if="!reviews.length" description="还没有写过点评" />
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type UploadFile } from 'element-plus'
import { confirm, toast } from '@/utils/message'
import AlbumCard from '@/components/AlbumCard.vue'
import ReviewItem from '@/components/ReviewItem.vue'
import { myLikes } from '@/api/album'
import { myReviews } from '@/api/booking'
import { uploadAvatar } from '@/api/auth'
import { getPhotographer } from '@/api/photographer'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { genderMark } from '@/utils/format'
import type { Album, ReviewView } from '@/types'

const user = useUserStore()
const notification = useNotificationStore()
const router = useRouter()
const u = computed(() => user.user)

const tab = ref<'likes' | 'reviews'>('likes')
const fans = ref(0)
const following = ref(0)
const likes = ref<Album[]>([])
const reviews = ref<ReviewView[]>([])
const reviewTotal = ref(0)

async function onAvatar(f: UploadFile) {
  if (!f.raw) return
  if (f.raw.size > 1024 * 1024) return void toast.error('图片不可以超过 1M')
  user.setUser(await uploadAvatar(f.raw))
  toast.success('头像已更新')
}

async function onMore(cmd: string) {
  if (cmd === 'logout') {
    if (!(await confirm('确定退出登录？', { type: 'info' }))) return
    await user.logout()
    toast.success('已退出登录')
    router.replace('/')
  }
}

onMounted(async () => {
  const id = u.value!.id
  // 各块独立加载，一块失败不影响其它（错误已由请求拦截器提示）
  const [d, l, r] = await Promise.allSettled([getPhotographer(id, null, false), myLikes(), myReviews()])
  if (d.status === 'fulfilled') {
    fans.value = d.value.fans
    following.value = d.value.following
  }
  if (l.status === 'fulfilled') likes.value = l.value
  if (r.status === 'fulfilled') {
    reviews.value = r.value.records
    reviewTotal.value = r.value.total
  }
})
</script>

<style lang="scss" scoped>
.hero { padding: 28px 0 36px; background: radial-gradient(circle at 80% 0, #3b3f46, #121315 70%); color: #fff; }
.head { display: flex; align-items: center; gap: 16px; padding: 0 20px; }
.avatar { border: 3px solid #fff; font-size: 28px; cursor: pointer; }
.who { min-width: 0; }
h1 { margin: 0; font-size: 24px; }
.admin { font-size: 12px; padding: 1px 6px; border-radius: 4px; background: var(--brand); color: var(--text); vertical-align: middle; }
.uid { margin-top: 4px; font-size: 13px; opacity: 0.75; letter-spacing: 1px; }
.gender { margin-top: 4px; color: #ff7aa2; }
.gender.male { color: #7aa8ff; }
.bio { margin: 14px 20px 0; font-size: 15px; opacity: 0.9; }
.stats { display: flex; align-items: center; gap: 28px; padding: 16px 20px 0; }
.stats > div:not(.ops) { display: flex; flex-direction: column; }
.stats b { font-size: 22px; }
.stats span { font-size: 12px; opacity: 0.6; }
.ops { margin-left: auto; display: flex; gap: 8px; }
.ghost.el-button { --el-button-bg-color: transparent; --el-button-text-color: #fff; --el-button-border-color: #fff; --el-button-hover-bg-color: rgba(255, 255, 255, 0.1); --el-button-hover-text-color: #fff; --el-button-hover-border-color: #fff; }
.entries { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 16px 16px 0; }
.entry { display: flex; flex-direction: column; gap: 4px; padding: 12px; border-radius: 12px; background: rgba(255, 255, 255, 0.1); }
.entry b { display: flex; align-items: center; gap: 4px; font-size: 15px; }
.entry span { font-size: 12px; opacity: 0.6; }
.entry .count { margin-left: 4px; display: flex; }
.sheet { position: relative; margin-top: -20px; min-height: 40vh; padding: 4px 12px 20px; background: #fff; border-radius: 20px 20px 0 0; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }

/* PC：深色头部变成圆角卡片，喜欢 4 列 */
@media (min-width: 768px) {
  .hero { border-radius: var(--radius) var(--radius) 0 0; padding: 28px 8px 40px; }
  .sheet { padding: 4px 20px 24px; }
  .grid { grid-template-columns: repeat(4, 1fr); gap: 14px; }
}
</style>

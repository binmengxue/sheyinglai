<template>
  <div class="home">
    <header class="hero">
      <div class="brand">摄影来</div>
      <div class="slogan">找到附近的摄影师，约一场好看的拍摄</div>
    </header>

    <div class="filters">
      <div class="search">
        <el-input v-model="keyword" placeholder="搜索摄影师昵称" clearable :prefix-icon="'Search'" @change="load(true)" />
        <el-input v-model="city" placeholder="城市" clearable class="city" @change="load(true)" />
        <el-select v-model="price" class="price" aria-label="价格" @change="load(true)">
          <el-option v-for="(p, i) in PRICES" :key="i" :label="p.label" :value="i" />
        </el-select>
      </div>
      <div class="chips">
        <button class="chip" :class="{ active: !style }" @click="setStyle('')">全部</button>
        <button v-for="s in STYLES" :key="s" class="chip" :class="{ active: style === s }" @click="setStyle(s)">{{ s }}</button>
      </div>
      <div class="sorts">
        <button v-for="s in sorts" :key="s.key" type="button" class="sort" :class="{ active: sort === s.key }" @click="setSort(s.key)">{{ s.label }}</button>
      </div>
    </div>

    <div class="list">
      <PhotographerCard v-for="c in list" :key="c.id" :c="c" />
    </div>
    <div v-if="loading" v-loading="true" class="loading" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length && filtered" description="没有找到符合条件的摄影师">
      <el-button round @click="clearFilters">清除筛选</el-button>
    </el-empty>
    <el-empty v-else-if="!list.length" description="附近还没有摄影师，快来成为第一个吧">
      <el-button class="cta" round @click="$router.push('/studio/profile')">我是摄影师，去入驻</el-button>
    </el-empty>
    <div v-else class="more">
      <el-button v-if="list.length < total" round @click="load()">加载更多</el-button>
      <span v-else class="muted">没有更多了</span>
    </div>

    <footer class="footer">
      <span>© {{ new Date().getFullYear() }} 摄影来</span>
      <!-- 备案通过后必须在网站底部展示备案号并链接到工信部网站，把下面的文字换成你的备案号 -->
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">苏ICP备XXXXXXXX号</a>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PhotographerCard from '@/components/PhotographerCard.vue'
import { listPhotographers, type SortKey } from '@/api/photographer'
import { usePagedList } from '@/composables/usePagedList'
import { getLocation, peekLocation, STYLES } from '@/utils/format'
import { toast } from '@/utils/message'
import type { PhotographerCard as Card } from '@/types'

const PAGE_SIZE = 10
const sorts: { key: SortKey; label: string }[] = [
  { key: 'default', label: '综合' },
  { key: 'distance', label: '距离最近' },
  { key: 'rating', label: '评分最高' },
  { key: 'price', label: '价格最低' },
]

/** 每小时价格区间 */
const PRICES: { label: string; min?: number; max?: number }[] = [
  { label: '价格不限' },
  { label: '200 元以下', max: 199 },
  { label: '200~500 元', min: 200, max: 500 },
  { label: '500 元以上', min: 501 },
]

const style = ref('')
const sort = ref<SortKey>('default')
const keyword = ref('')
const city = ref('')
const price = ref(0)
const filtered = computed(() => !!(style.value || keyword.value.trim() || city.value.trim() || price.value))
let loc = peekLocation() ?? null

const { list, total, loading, error, load } = usePagedList<Card>((page) => listPhotographers({
  style: style.value || undefined, sort: sort.value, ...(loc ?? {}), page, size: PAGE_SIZE,
  keyword: keyword.value.trim() || undefined, city: city.value.trim() || undefined,
  minPrice: PRICES[price.value].min, maxPrice: PRICES[price.value].max,
}))

function clearFilters() {
  style.value = ''
  keyword.value = ''
  city.value = ''
  price.value = 0
  load(true)
}

function setStyle(s: string) {
  style.value = s
  load(true)
}

async function setSort(s: SortKey) {
  sort.value = s
  if (s === 'distance' && !loc) {
    loc = await getLocation()
    if (!loc) toast.info('未获取到定位，暂时无法按距离排序，请检查浏览器定位权限')
  }
  load(true)
}

onMounted(async () => {
  // 先出列表；第一次打开时定位可能要等几秒，拿到后再刷新一次补上距离
  load(true)
  if (peekLocation() !== undefined) return
  const got = await getLocation()
  if (got && !loc) {
    loc = got
    load(true)
  }
})
</script>

<style lang="scss" scoped>
.hero { padding: 28px 16px 56px; background: linear-gradient(160deg, #1f2329 0%, #3a3f47 100%); color: #fff; }
.brand { font-size: 26px; font-weight: 800; letter-spacing: 3px; }
.slogan { margin-top: 6px; font-size: 14px; opacity: 0.8; }
.filters {
  position: sticky; top: 0; z-index: 5; margin-top: -36px; padding: 12px 12px 8px;
  background: var(--bg); border-radius: 18px 18px 0 0;
}
.search { display: flex; gap: 8px; margin-bottom: 10px; }
.search .city { width: 88px; flex-shrink: 0; }
.search .price { width: 120px; flex-shrink: 0; }
.sorts { display: flex; gap: 18px; margin-top: 10px; font-size: 14px; color: #666; }
.sort { cursor: pointer; border: none; background: none; padding: 0; font: inherit; color: inherit; }
.sort.active { color: var(--text); font-weight: 700; }
.list { display: flex; flex-direction: column; gap: 10px; padding: 4px 12px; }
.loading { height: 120px; }
.more { text-align: center; padding: 16px; }
.footer { display: flex; justify-content: center; gap: 12px; padding: 16px; font-size: 12px; color: var(--text-muted); }

/* PC：矮横幅 + 两列卡片 */
@media (min-width: 768px) {
  .hero { padding: 28px 32px; border-radius: var(--radius); }
  .brand { font-size: 28px; }
  .filters { position: static; margin-top: 0; padding: 16px 0 8px; border-radius: 0; display: flex; flex-wrap: wrap; align-items: center; gap: 16px; }
  /* 搜索单独占一行，筛选和排序在下一行 */
  .search { flex: 0 0 100%; margin-bottom: 0; }
  .search > .el-input:first-child { max-width: 360px; }
  .search .city { width: 140px; }
  .search .price { width: 140px; }
  .filters .chips { flex: 1; }
  .sorts { margin-top: 0; flex-shrink: 0; }
  .list { display: grid; grid-template-columns: repeat(auto-fill, minmax(520px, 1fr)); gap: 16px; padding: 8px 0; }
}
</style>

<template>
  <div class="chat-page">
    <NavBar :title="conv?.peer.nickname ?? '私信'" fallback="/chats">
      <template #right>
        <el-button v-if="conv?.peer.id" link :icon="'User'" @click="$router.push(`/p/${conv.peer.id}`)">主页</el-button>
      </template>
    </NavBar>

    <el-empty v-if="error" description="会话不存在或加载失败">
      <el-button round @click="$router.replace('/chats')">返回私信列表</el-button>
    </el-empty>
    <div v-else class="box">
      <div ref="scroller" v-loading="loading && !messages.length" class="msgs">
        <div v-if="hasMore" class="more">
          <el-button link :loading="loadingMore" @click="loadMore">查看更早的消息</el-button>
        </div>
        <template v-for="(m, i) in messages" :key="m.id">
          <div v-if="showTime(i)" class="time">{{ fmtChatTime(m.createdAt) }}</div>
          <div class="row" :class="{ mine: m.senderId === myId }">
            <RouterLink :to="`/p/${m.senderId}`" class="avatar">
              <el-avatar :size="36" :src="avatarOf(m)">{{ nameOf(m).slice(0, 1) }}</el-avatar>
            </RouterLink>
            <div v-if="m.type === 'TEXT'" class="bubble">{{ m.content }}</div>
            <el-image v-else class="pic" :src="m.content" :preview-src-list="[m.content]" preview-teleported fit="cover"
              @load="m.id === lastId && scrollToBottom()" />
          </div>
        </template>
        <p v-if="!loading && !messages.length" class="muted hint">打个招呼吧，可以问问档期、价格和拍摄风格</p>
      </div>

      <div class="composer">
        <button class="pick" type="button" aria-label="发送图片" :disabled="sending" @click="fileInput?.click()">
          <el-icon :size="24"><Picture /></el-icon>
        </button>
        <input ref="fileInput" type="file" accept="image/*" hidden @change="onPick" />
        <el-input v-model="text" class="input" type="textarea" resize="none" :autosize="{ minRows: 1, maxRows: 4 }"
          :maxlength="MAX_TEXT" placeholder="说点什么…" @keydown.enter.exact="onEnter" />
        <el-button class="cta" round :disabled="!text.trim()" :loading="sending" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import NavBar from '@/components/NavBar.vue'
import { getChat, listChatMessages, readChat, sendChatImage, sendChatText } from '@/api/chat'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { compressImage } from '@/utils/image'
import { fmtChatTime } from '@/utils/format'
import { setTitle } from '@/utils/title'
import { toast } from '@/utils/message'
import type { ChatConversation, ChatMessage } from '@/types'

const PAGE_SIZE = 30
const MAX_TEXT = 500
/** 两条消息间隔超过 5 分钟时中间显示时间 */
const TIME_GAP_MS = 5 * 60 * 1000

const props = defineProps<{ id: string }>()
const chat = useChatStore()
const user = useUserStore()
const myId = computed(() => user.user?.id)

const conv = ref<ChatConversation | null>(null)
const messages = ref<ChatMessage[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const error = ref(false)
const text = ref('')
const sending = ref(false)
const scroller = ref<HTMLElement>()
const fileInput = ref<HTMLInputElement>()
const lastId = computed(() => messages.value[messages.value.length - 1]?.id)

const convId = () => Number(props.id)
const avatarOf = (m: ChatMessage) => (m.senderId === myId.value ? user.user?.avatarUrl : conv.value?.peer.avatarUrl)
const nameOf = (m: ChatMessage) => (m.senderId === myId.value ? user.user?.nickname : conv.value?.peer.nickname) ?? ''

function showTime(i: number) {
  if (i === 0) return true
  const gap = new Date(messages.value[i].createdAt).getTime() - new Date(messages.value[i - 1].createdAt).getTime()
  return gap > TIME_GAP_MS
}

/** 按 id 去重、保持顺序：自己发的消息 HTTP 返回和 WebSocket 推送都会到 */
function addMessages(list: ChatMessage[]) {
  const ids = new Set(messages.value.map((m) => m.id))
  const fresh = list.filter((m) => !ids.has(m.id))
  if (!fresh.length) return false
  messages.value = [...messages.value, ...fresh].sort((a, b) => a.id - b.id)
  return true
}

function nearBottom() {
  const el = scroller.value
  return !el || el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

async function scrollToBottom() {
  await nextTick()
  const el = scroller.value
  if (el) el.scrollTop = el.scrollHeight
}

async function markRead() {
  await readChat(convId()).catch(() => undefined)
  chat.refresh()
}

async function load() {
  const id = convId()
  chat.activeId = id
  conv.value = null
  messages.value = []
  error.value = false
  loading.value = true
  try {
    const [c, list] = await Promise.all([getChat(id), listChatMessages(id, undefined, PAGE_SIZE)])
    if (id !== convId()) return
    conv.value = c
    setTitle(`和${c.peer.nickname}的私信`)
    messages.value = list
    hasMore.value = list.length === PAGE_SIZE
    scrollToBottom()
    if (c.unread) markRead()
  } catch {
    if (id === convId()) error.value = true
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  const first = messages.value[0]
  if (!first || loadingMore.value) return
  loadingMore.value = true
  const el = scroller.value
  const oldHeight = el?.scrollHeight ?? 0
  try {
    const list = await listChatMessages(convId(), first.id, PAGE_SIZE)
    hasMore.value = list.length === PAGE_SIZE
    addMessages(list)
    // 保持当前看到的位置，不跳到顶部
    await nextTick()
    if (el) el.scrollTop += el.scrollHeight - oldHeight
  } finally {
    loadingMore.value = false
  }
}

async function send() {
  const content = text.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    addMessages([await sendChatText(convId(), content)])
    text.value = ''
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

/** 回车发送，Shift+回车换行；输入法选字时的回车不算 */
function onEnter(e: Event | KeyboardEvent) {
  if ((e as KeyboardEvent).isComposing) return
  e.preventDefault()
  send()
}

async function onPick(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || sending.value) return
  sending.value = true
  try {
    const small = await compressImage(file).catch((err: Error) => {
      toast.error(err.message)
      return null
    })
    if (!small) return
    addMessages([await sendChatImage(convId(), small)])
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

const off = chat.subscribe((p) => {
  if (p.conversationId !== convId()) return
  const stick = nearBottom() || p.message.senderId === myId.value
  if (!addMessages([p.message])) return
  if (stick) scrollToBottom()
  if (p.message.senderId !== myId.value) markRead()
})

// 断线重连后补拉最新的消息
watch(() => chat.connectedTimes, async () => {
  if (!conv.value) return
  const id = convId()
  const list = await listChatMessages(id, undefined, PAGE_SIZE).catch(() => [])
  if (id === convId() && addMessages(list)) {
    scrollToBottom()
    markRead()
  }
})

watch(() => props.id, load, { immediate: true })

onUnmounted(() => {
  off()
  chat.activeId = null
})
</script>

<style lang="scss" scoped>
.chat-page { display: flex; flex-direction: column; height: 100vh; height: 100dvh; }
.box { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.msgs { flex: 1; min-height: 0; overflow-y: auto; padding: 8px 12px 16px; }
.more { text-align: center; padding: 4px 0 8px; }
.time { text-align: center; margin: 12px 0 8px; font-size: 12px; color: var(--text-muted); }
.hint { text-align: center; margin-top: 40px; }
.row { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 12px; }
.row.mine { flex-direction: row-reverse; }
.avatar { flex-shrink: 0; }
.bubble {
  max-width: 70%; padding: 9px 12px; border-radius: 4px 14px 14px 14px; background: #fff;
  font-size: 15px; line-height: 1.5; white-space: pre-wrap; word-break: break-word;
}
.mine .bubble { border-radius: 14px 4px 14px 14px; background: var(--brand); }
.pic { max-width: 180px; max-height: 240px; border-radius: 10px; cursor: zoom-in; }
.composer {
  display: flex; align-items: flex-end; gap: 8px;
  padding: 8px 12px calc(8px + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee;
}
.pick { flex-shrink: 0; width: 36px; height: 36px; border: none; background: none; color: #555; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.pick:disabled { opacity: 0.4; cursor: default; }
.input { flex: 1; }
.composer .el-button { flex-shrink: 0; height: 36px; }

/* PC 工作台：右侧内容区里的一个聊天卡片 */
@media (min-width: 768px) {
  .chat-page { height: calc(100vh - var(--header-height) - 40px); }
  .box { overflow: hidden; border-radius: var(--radius); background: var(--bg); border: 1px solid #eceef1; }
  .msgs { padding: 16px 20px; }
  .bubble { max-width: 60%; }
  .pic { max-width: 240px; max-height: 300px; }
  .composer { padding: 12px 16px; }
}
</style>

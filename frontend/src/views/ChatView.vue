<template>
  <div class="page chat-page">
    <div class="container chat-layout">
      <aside class="side card">
        <div class="side-brand">
          <span class="side-avatar">🤖</span>
          <div>
            <strong>校园 AI 助手</strong>
            <span class="status"><i></i> 在线</span>
          </div>
        </div>

        <p class="side-desc">
          可以帮你找店铺、挑优惠、安排今天吃什么，也能看懂你拍的店招照片。
        </p>

        <div class="side-block">
          <h4>试试这样问</h4>
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            class="quick-prompt"
            :disabled="loading"
            @click="askQuick(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <div class="side-block">
          <h4>它能做什么</h4>
          <ul class="abilities">
            <li>🍚 按口味推荐校园店铺</li>
            <li>🎟️ 告诉你哪些优惠值得领</li>
            <li>📷 拍照识别店招并介绍</li>
          </ul>
        </div>

        <el-button class="clear-btn" round :disabled="!messages.length" @click="clearHistory">
          <el-icon><Delete /></el-icon>
          <span style="margin-left: 6px">清空对话</span>
        </el-button>
      </aside>

      <section class="chat-card card">
        <header class="chat-head">
          <div class="chat-title">
            <span class="dot"></span>
            <strong>和 AI 聊聊</strong>
            <span class="muted">回答由 AI 生成，仅供参考</span>
          </div>
          <el-button text circle :disabled="!messages.length" title="清空对话" @click="clearHistory">
            <el-icon><Delete /></el-icon>
          </el-button>
        </header>

        <div ref="listRef" class="messages">
          <div class="msg assistant">
            <span class="avatar">🤖</span>
            <div class="bubble">
              嗨，我是校园优购的 AI 助手 👋
              <br />
              想吃什么、想找哪家店、有哪些优惠，都可以问我。也可以发一张店招照片让我看看。
            </div>
          </div>

          <div
            v-for="(message, index) in messages"
            :key="index"
            class="msg"
            :class="message.role"
          >
            <span v-if="message.role === 'assistant'" class="avatar">🤖</span>
            <div class="bubble" :class="{ error: message.error }">
              <img
                v-if="message.image"
                :src="message.image"
                alt="发送的图片"
                class="msg-image"
              />
              <template v-else>{{ message.content }}</template>
            </div>
            <span v-if="message.role === 'user'" class="avatar user-avatar">
              <img :src="auth.user?.icon || '/imgs/icons/default-icon.png'" alt="我" />
            </span>
          </div>

          <div v-if="loading" class="msg assistant">
            <span class="avatar">🤖</span>
            <div class="bubble typing">
              <i></i><i></i><i></i>
            </div>
          </div>
        </div>

        <footer class="composer">
          <button class="image-btn" type="button" :disabled="loading" title="发送图片" @click="pickImage">
            <el-icon><Picture /></el-icon>
          </button>
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="问问 AI 助手…（Enter 发送，Shift + Enter 换行）"
            :disabled="loading"
            @keydown.enter.exact.prevent="send"
          />
          <el-button class="send-btn" type="primary" :loading="loading" @click="send">
            <el-icon v-if="!loading"><Promotion /></el-icon>
            <span style="margin-left: 6px">发送</span>
          </el-button>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onPickImage" />
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Picture, Promotion } from '@element-plus/icons-vue'
import { askChat, askChatImage } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import { compressImage, dataUrlToBlob } from '@/composables/useImageCompress'

const STORAGE_KEY = 'chat:history'
const quickPrompts = [
  '今天食堂有什么好吃的推荐？',
  '帮我找一家适合自习的咖啡店',
  '校园里最近有什么优惠券？'
]

const auth = useAuthStore()
const input = ref('')
const messages = ref([])
const loading = ref(false)
const listRef = ref(null)
const fileInput = ref(null)

onMounted(() => {
  restoreHistory()
  auth.fetchMe()
  scrollBottom()
})

watch(messages, scrollBottom, { deep: true })

function scrollBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function restoreHistory() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return
  try {
    const list = JSON.parse(raw)
    if (Array.isArray(list)) {
      messages.value = list.filter(
        (item) => item && (item.role === 'user' || item.role === 'assistant') && item.content
      )
    }
  } catch (error) {
    sessionStorage.removeItem(STORAGE_KEY)
  }
}

function saveHistory() {
  const textMessages = messages.value
    .filter((item) => !item.image && !item.error && item.content)
    .slice(-20)
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(textMessages))
}

function buildHistory() {
  return messages.value
    .filter((item) => !item.image && !item.error && item.content)
    .slice(-6)
    .map((item) => ({ role: item.role, content: item.content }))
}

async function send() {
  const question = input.value.trim()
  if (!question || loading.value) return

  const history = buildHistory()
  input.value = ''
  messages.value.push({ role: 'user', content: question })
  saveHistory()
  scrollBottom()

  loading.value = true
  try {
    const answer = await askChat({ question, history })
    messages.value.push({
      role: 'assistant',
      content: answer || '我暂时没有想好怎么回答，换个问法试试？'
    })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: error?.message || '服务器开小差了，请稍后再试',
      error: true
    })
  } finally {
    loading.value = false
    saveHistory()
    scrollBottom()
  }
}

function askQuick(prompt) {
  if (loading.value) return
  input.value = prompt
  send()
}

function pickImage() {
  if (loading.value) return
  fileInput.value?.click()
}

async function onPickImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能发送图片文件')
    return
  }

  loading.value = true
  try {
    const dataUrl = await compressImage(file)
    messages.value.push({ role: 'user', image: dataUrl, content: '（图片）' })
    scrollBottom()

    const answer = await askChatImage(dataUrlToBlob(dataUrl))
    messages.value.push({
      role: 'assistant',
      content: answer || '这张图片我暂时没看懂，换一张试试？'
    })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: error?.message || '识图失败，请稍后再试',
      error: true
    })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

async function clearHistory() {
  if (!messages.value.length) return
  try {
    await ElMessageBox.confirm('清空后这段对话就找不回来了，确定吗？', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (error) {
    return
  }
  messages.value = []
  sessionStorage.removeItem(STORAGE_KEY)
  ElMessage.success('已清空对话')
}
</script>

<style scoped>
.chat-page {
  padding: 24px 0 40px;
}

.chat-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  height: calc(100vh - var(--nav-h) - 120px);
  min-height: 560px;
}

.side {
  padding: 24px 22px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.side-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.side-avatar {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #ded4ff, #b8a7ff);
  font-size: 24px;
}

.side-brand strong {
  display: block;
  font-size: 16px;
  font-weight: 800;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #2f9b7d;
  font-weight: 600;
}

.status i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--mint);
  box-shadow: 0 0 0 3px rgba(111, 211, 181, 0.25);
}

.side-desc {
  font-size: 12px;
  line-height: 1.8;
  color: var(--ink-soft);
}

.side-block h4 {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-faint);
  letter-spacing: 0.06em;
}

.quick-prompt {
  width: 100%;
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fffdfa;
  color: var(--ink-soft);
  font-size: 12px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.quick-prompt:hover:not(:disabled) {
  border-color: var(--lavender);
  background: var(--lavender-soft);
  color: #6f58cf;
}

.abilities {
  display: flex;
  flex-direction: column;
  gap: 7px;
  font-size: 12px;
  color: var(--ink-soft);
}

.clear-btn {
  margin-top: auto;
}

.chat-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.chat-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 253, 250, 0.9);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
}

.chat-title strong {
  font-size: 15px;
  font-weight: 800;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--mint);
  box-shadow: 0 0 0 4px rgba(111, 211, 181, 0.18);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background:
    radial-gradient(420px 220px at 92% 6%, rgba(184, 167, 255, 0.1), transparent 65%),
    transparent;
}

.msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 82%;
}

.msg.user {
  align-self: flex-end;
  flex-direction: row;
}

.msg.assistant {
  align-self: flex-start;
}

.avatar {
  width: 34px;
  height: 34px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: var(--lavender-soft);
  font-size: 17px;
}

.user-avatar {
  background: var(--coral-soft);
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bubble {
  padding: 12px 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  font-size: 14px;
  line-height: 1.75;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg.assistant .bubble {
  border-top-left-radius: 6px;
}

.msg.user .bubble {
  background: linear-gradient(135deg, var(--coral), #ff9a72);
  border-color: transparent;
  color: #fff;
  border-top-right-radius: 6px;
}

.bubble.error {
  background: #fff0ee;
  border-color: #ffc9c0;
  color: #a33b2e;
}

.msg-image {
  max-width: 240px;
  max-height: 240px;
  border-radius: 12px;
}

.typing {
  display: flex;
  gap: 5px;
  padding: 16px 18px;
}

.typing i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--lavender);
  animation: blink 1.2s infinite;
}

.typing i:nth-child(2) {
  animation-delay: 0.2s;
}

.typing i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-4px);
  }
}

.composer {
  flex: none;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 18px 18px;
  border-top: 1px solid var(--line);
  background: rgba(255, 253, 250, 0.95);
}

.image-btn {
  width: 44px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  color: var(--ink-soft);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.image-btn:hover:not(:disabled) {
  border-color: var(--lavender);
  color: #6f58cf;
}

.send-btn {
  height: 44px;
  flex: none;
  padding: 0 22px;
}

@media (max-width: 900px) {
  .chat-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .side {
    display: none;
  }

  .chat-card {
    height: calc(100vh - var(--nav-h) - 150px);
    min-height: 520px;
  }

  .msg {
    max-width: 92%;
  }
}
</style>

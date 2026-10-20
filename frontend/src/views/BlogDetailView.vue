<template>
  <div class="page blog-detail">
    <div class="container">
      <PageHeader
        title="校园动态"
        :subtitle="blog.name ? `来自 ${blog.name}` : '看看同学在分享什么'"
        crumb="校园动态 / 详情"
        back
      />

      <div v-if="loading" class="loading card">正在加载动态…</div>

      <div v-else-if="blog.id" class="detail-layout">
        <div class="main">
          <section v-if="images.length" class="carousel card" @keydown.left.prevent="prev" @keydown.right.prevent="next">
            <div
              class="stage"
              @pointerdown="onPointerDown"
              @pointerup="onPointerUp"
              @pointercancel="onPointerCancel"
            >
              <transition name="fade" mode="out-in">
                <img :key="index" :src="images[index]" alt="动态图片" />
              </transition>

              <button v-if="images.length > 1" class="arrow left" type="button" @click="prev">
                <el-icon><ArrowLeft /></el-icon>
              </button>
              <button v-if="images.length > 1" class="arrow right" type="button" @click="next">
                <el-icon><ArrowRight /></el-icon>
              </button>

              <span v-if="images.length > 1" class="counter">{{ index + 1 }} / {{ images.length }}</span>
            </div>

            <div v-if="images.length > 1" class="dots">
              <button
                v-for="(image, i) in images"
                :key="image + i"
                type="button"
                :class="{ active: i === index }"
                @click="index = i"
              ></button>
            </div>
          </section>

          <article class="article card">
            <h1 v-if="blog.title" class="article-title">{{ blog.title }}</h1>
            <div class="article-content" v-html="safeContent"></div>
            <time class="article-time">{{ formatDateTime(blog.createTime) }}</time>
          </article>

          <section v-if="shop.id" class="linked-shop card" @click="router.push(`/shop/${shop.id}`)">
            <div class="shop-thumb">
              <img :src="shopCover" :alt="shop.name" />
            </div>
            <div class="shop-copy">
              <span class="pill pill--mint">关联店铺</span>
              <h3>{{ shop.name }}</h3>
              <p class="muted">{{ shop.area }} · 人均 ￥{{ shop.avgPrice || 0 }}</p>
            </div>
            <el-icon class="shop-go"><ArrowRight /></el-icon>
          </section>

          <section class="likes card">
            <div class="like-avatars">
              <img
                v-for="user in likes"
                :key="user.id"
                :src="user.icon || '/imgs/icons/default-icon.png'"
                :alt="user.nickName || '同学'"
              />
              <span v-if="!likes.length && !blog.liked" class="muted">还没有人点赞，来当第一个吧</span>
            </div>
            <span class="like-count">♥ {{ blog.liked || 0 }} 人点赞</span>
          </section>
        </div>

        <aside class="aside">
          <section class="author-card card">
            <div class="author-head" @click="goAuthor">
              <img :src="blog.icon || '/imgs/icons/default-icon.png'" alt="avatar" />
              <div class="author-copy">
                <strong>{{ blog.name || '校园同学' }}</strong>
                <span class="muted">{{ formatTime(blog.createTime) }}</span>
              </div>
            </div>
            <el-button
              v-if="blog.userId && auth.user && auth.user.id !== blog.userId"
              class="follow-btn"
              :type="followed ? 'default' : 'primary'"
              round
              @click="toggleFollow"
            >
              {{ followed ? '已关注' : '+ 关注' }}
            </el-button>
          </section>

          <section class="action-card card">
            <button class="action like" :class="{ active: blog.isLike }" type="button" @click="toggleLike">
              <span class="action-icon">♥</span>
              <span>{{ blog.isLike ? '已点赞' : '点赞' }}</span>
              <em>{{ blog.liked || 0 }}</em>
            </button>
            <button class="action" type="button" @click="share">
              <span class="action-icon">🔗</span>
              <span>复制链接</span>
            </button>
            <el-button class="ai-btn" type="primary" round @click="router.push('/chat')">
              <el-icon><MagicStick /></el-icon>
              <span style="margin-left: 6px">问问 AI</span>
            </el-button>
          </section>
        </aside>
      </div>

      <EmptyState
        v-else
        icon="🍂"
        title="这条动态找不到了"
        description="可能已经被作者删除"
      >
        <el-button round @click="router.push('/')">回到首页</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, MagicStick } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getBlog, getBlogLikes, likeBlog } from '@/api/blog'
import { getShop } from '@/api/shop'
import { followUser, isFollowing } from '@/api/follow'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, formatTime, splitImages } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const blog = ref({})
const shop = ref({})
const likes = ref([])
const followed = ref(false)
const loading = ref(true)
const index = ref(0)

const images = computed(() => splitImages(blog.value.images))
const shopCover = computed(() => splitImages(shop.value.images)[0] || '/imgs/shops/shop01.jpg')
const safeContent = computed(() => DOMPurify.sanitize(blog.value.content || ''))

let dragStartX = null

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})

watch(() => route.params.id, load)

async function load() {
  if (!route.params.id) return
  loading.value = true
  index.value = 0
  try {
    const [blogData, likeUsers] = await Promise.all([
      getBlog(route.params.id),
      getBlogLikes(route.params.id)
    ])
    blog.value = blogData || {}
    likes.value = likeUsers || []

    if (blog.value.shopId) {
      getShop(blog.value.shopId)
        .then((data) => {
          shop.value = data || {}
        })
        .catch(() => {
          shop.value = {}
        })
    }

    await loadFollowState()
  } catch (error) {
    blog.value = {}
  } finally {
    loading.value = false
  }
}

async function loadFollowState() {
  if (!auth.user) await auth.fetchMe()
  if (!auth.user || auth.user.id === blog.value.userId) {
    followed.value = false
    return
  }
  try {
    followed.value = Boolean(await isFollowing(blog.value.userId, { silent: true }))
  } catch (error) {
    followed.value = false
  }
}

function goAuthor() {
  if (!blog.value.userId) return
  if (auth.user?.id === blog.value.userId) router.push('/me')
  else router.push(`/user/${blog.value.userId}`)
}

async function toggleFollow() {
  try {
    await followUser(blog.value.userId, !followed.value)
    followed.value = !followed.value
    ElMessage.success(followed.value ? '已关注' : '已取消关注')
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleLike() {
  try {
    await likeBlog(blog.value.id)
    const [blogData, likeUsers] = await Promise.all([
      getBlog(blog.value.id),
      getBlogLikes(blog.value.id)
    ])
    blog.value = blogData || blog.value
    likes.value = likeUsers || []
  } catch (error) {
    // 拦截器已提示
  }
}

async function share() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制')
  } catch (error) {
    ElMessage.info(window.location.href)
  }
}

function next() {
  if (images.value.length < 2) return
  index.value = (index.value + 1) % images.value.length
}

function prev() {
  if (images.value.length < 2) return
  index.value = (index.value - 1 + images.value.length) % images.value.length
}

function onPointerDown(event) {
  dragStartX = event.clientX
}

function onPointerUp(event) {
  if (dragStartX === null) return
  const delta = event.clientX - dragStartX
  dragStartX = null
  if (Math.abs(delta) < 40) return
  if (delta < 0) next()
  else prev()
}

function onPointerCancel() {
  dragStartX = null
}

function onKeydown(event) {
  if (event.key === 'ArrowLeft') prev()
  if (event.key === 'ArrowRight') next()
}
</script>

<style scoped>
.loading {
  padding: 60px;
  text-align: center;
  color: var(--ink-soft);
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.carousel {
  padding: 12px;
  outline: none;
}

.stage {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 14px;
  background: var(--cream-100);
  touch-action: pan-y;
}

.stage img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
}

.arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 999px;
  background: rgba(255, 251, 245, 0.88);
  color: var(--ink);
  font-size: 16px;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: all 0.2s var(--ease);
}

.arrow:hover {
  background: #fff;
  color: var(--coral-dark);
  transform: translateY(-50%) scale(1.06);
}

.arrow.left {
  left: 14px;
}

.arrow.right {
  right: 14px;
}

.counter {
  position: absolute;
  right: 14px;
  bottom: 14px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(59, 47, 42, 0.55);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.dots {
  display: flex;
  justify-content: center;
  gap: 7px;
  padding-top: 12px;
}

.dots button {
  width: 7px;
  height: 7px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: var(--cream-300);
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.dots button.active {
  width: 20px;
  background: var(--coral);
}

.article {
  padding: 32px 36px;
}

.article-title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.article-content {
  margin-top: 18px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--ink);
  word-break: break-word;
}

.article-content :deep(img) {
  border-radius: 14px;
  margin: 12px 0;
}

.article-time {
  display: block;
  margin-top: 24px;
  font-size: 12px;
  color: var(--ink-faint);
}

.linked-shop {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  cursor: pointer;
  transition: all 0.25s var(--ease);
}

.linked-shop:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.shop-thumb {
  width: 78px;
  height: 78px;
  flex: none;
  border-radius: 14px;
  overflow: hidden;
  background: var(--cream-100);
}

.shop-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-copy {
  flex: 1;
  min-width: 0;
}

.shop-copy h3 {
  margin-top: 6px;
  font-size: 17px;
  font-weight: 800;
}

.shop-copy p {
  margin-top: 4px;
  font-size: 12px;
}

.shop-go {
  color: var(--ink-faint);
}

.likes {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
}

.like-avatars {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.like-avatars img {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  object-fit: cover;
  border: 2px solid #fff;
  margin-left: -10px;
  box-shadow: 0 2px 6px rgba(59, 47, 42, 0.1);
}

.like-avatars img:first-child {
  margin-left: 0;
}

.like-count {
  flex: none;
  font-size: 12px;
  font-weight: 700;
  color: var(--coral);
}

.aside {
  position: sticky;
  top: calc(var(--nav-h) + 20px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.author-card {
  padding: 22px;
}

.author-head {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-head img {
  width: 52px;
  height: 52px;
  border-radius: 999px;
  object-fit: cover;
}

.author-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.author-copy strong {
  font-size: 15px;
  font-weight: 800;
}

.author-copy span {
  font-size: 12px;
}

.follow-btn {
  width: 100%;
  margin-top: 16px;
}

.action-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdfa;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.action:hover {
  border-color: var(--coral);
  color: var(--coral-dark);
}

.action.active {
  border-color: var(--coral);
  background: var(--coral-soft);
  color: var(--coral-dark);
}

.action-icon {
  font-size: 16px;
}

.action em {
  margin-left: auto;
  font-style: normal;
  font-size: 12px;
}

.ai-btn {
  width: 100%;
  background: linear-gradient(135deg, #a68cff, #7f66e8);
  border: none;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s var(--ease);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .aside {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .author-card,
  .action-card {
    flex: 1;
    min-width: 260px;
  }
}

@media (max-width: 640px) {
  .article {
    padding: 22px 20px;
  }

  .article-title {
    font-size: 22px;
  }

  .like-avatars img {
    width: 26px;
    height: 26px;
  }
}
</style>

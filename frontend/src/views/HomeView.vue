<template>
  <div class="page home">
    <section class="hero">
      <div class="hero-inner container">
        <div class="hero-copy">
          <span class="pill pill--coral">✨ 星城大学 · 校园生活指南</span>
          <h1>
            校园生活
            <span class="grad">一站逛明白</span>
          </h1>
          <p class="hero-sub">
            搜店铺、领优惠、看同学动态，还有 AI 助手帮你决定今天吃什么。
          </p>

          <div class="hero-search">
            <el-input
              v-model="keyword"
              size="large"
              placeholder="搜食堂、咖啡、打印店…"
              clearable
              @keyup.enter="search"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" size="large" @click="search">搜一搜</el-button>
          </div>

          <div class="quick">
            <button
              v-for="type in quickTypes"
              :key="type.id"
              type="button"
              class="quick-item"
              @click="goType(type)"
            >
              <span>{{ type.emoji }}</span>
              {{ type.name }}
            </button>
          </div>
        </div>

        <div class="hero-art" aria-hidden="true">
          <div class="blob blob-a"></div>
          <div class="blob blob-b"></div>
          <div class="float-card card-a">
            <span class="emoji">☕</span>
            <div>
              <strong>咖啡奶茶</strong>
              <em>下课来一杯</em>
            </div>
          </div>
          <div class="float-card card-b">
            <span class="emoji">🍚</span>
            <div>
              <strong>食堂餐厅</strong>
              <em>今天吃什么</em>
            </div>
          </div>
          <div class="float-card card-c">
            <span class="emoji">🤖</span>
            <div>
              <strong>问问 AI</strong>
              <em>让选择简单点</em>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="container">
      <section class="section">
        <div class="section-head">
          <h2 class="section-title">逛分类</h2>
          <span class="section-more" @click="router.push({ path: '/shops' })">
            全部店铺 <el-icon><ArrowRight /></el-icon>
          </span>
        </div>

        <div class="type-grid">
          <button
            v-for="type in types"
            :key="type.id"
            type="button"
            class="type-card hover-lift"
            @click="goType(type)"
          >
            <span class="type-emoji">{{ type.emoji }}</span>
            <span class="type-name">{{ type.name }}</span>
          </button>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">今日优惠</h2>
          <span class="section-more" @click="router.push({ path: '/shops' })">
            去逛逛 <el-icon><ArrowRight /></el-icon>
          </span>
        </div>

        <div v-if="promos.length" class="promo-grid">
          <button
            v-for="promo in promos"
            :key="promo.id"
            type="button"
            class="promo-card hover-lift"
            @click="router.push(`/shop/${promo.shopId}`)"
          >
            <div class="promo-top">
              <span class="promo-shop">{{ promo.shopName || '校园店铺' }}</span>
              <span v-if="discountText(promo)" class="promo-discount">
                {{ discountText(promo) }} 折
              </span>
            </div>
            <div class="promo-title clamp-2">{{ promo.title }}</div>
            <div class="promo-bottom">
              <span class="price"><small>￥</small>{{ formatPrice(promo.payValue) }}</span>
              <span class="promo-origin">原价 ￥{{ formatPrice(promo.actualValue) }}</span>
            </div>
          </button>
        </div>

        <EmptyState v-else icon="🎟️" title="今天还没有优惠" description="晚点再来看看，店铺正在准备中" />
      </section>

      <section class="section">
        <div class="section-head">
          <h2 class="section-title">校园动态</h2>
          <span class="section-more" @click="goPublish">
            <el-icon><Plus /></el-icon> 发布动态
          </span>
        </div>

        <div class="blog-grid">
          <BlogCard
            v-for="blog in blogs"
            :key="blog.id"
            :blog="blog"
            @like="handleLike"
          />
        </div>

        <div ref="sentinel" class="sentinel">
          <span v-if="loading" class="loading-text">正在加载更多动态…</span>
          <span v-else-if="finished && blogs.length" class="end-text">— 已经到底啦 —</span>
        </div>

        <EmptyState
          v-if="finished && !blogs.length"
          icon="📝"
          title="还没有校园动态"
          description="来发布第一条，记录你的校园生活"
        >
          <el-button type="primary" round @click="goPublish">去发布</el-button>
        </EmptyState>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Plus, Search } from '@element-plus/icons-vue'
import BlogCard from '@/components/BlogCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getShopTypes } from '@/api/shop'
import { getVouchersByShop } from '@/api/voucher'
import { getHotBlogs, likeBlog } from '@/api/blog'
import { useAuthStore } from '@/stores/auth'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { discount, formatPrice } from '@/utils/format'

const TYPE_EMOJI = {
  1: '🍚',
  2: '☕',
  3: '🛒',
  4: '🖨️',
  5: '📦',
  6: '📚',
  7: '🏋️',
  8: '💇',
  9: '🏥',
  10: '🔁'
}
const PROMO_SHOP_IDS = [1, 4, 8]
const PAGE_SIZE = 10

const router = useRouter()
const auth = useAuthStore()

const keyword = ref('')
const types = ref([])
const promos = ref([])
const blogs = ref([])
let current = 1

const quickTypes = computed(() => types.value.slice(0, 4))

const { sentinel, loading, finished } = useInfiniteScroll(async () => {
  const list = await getHotBlogs(current)
  current += 1
  if (!list || !list.length) return false
  blogs.value.push(...list)
  return list.length >= PAGE_SIZE
})

onMounted(() => {
  loadTypes()
  loadPromos()
})

async function loadTypes() {
  try {
    const list = (await getShopTypes()) || []
    types.value = list.slice(0, 10).map((type) => ({
      ...type,
      emoji: TYPE_EMOJI[type.id] || '🏫'
    }))
  } catch (error) {
    types.value = []
  }
}

async function loadPromos() {
  try {
    const results = await Promise.all(PROMO_SHOP_IDS.map((id) => getVouchersByShop(id)))
    promos.value = results.flatMap((list) => list || []).slice(0, 5)
  } catch (error) {
    promos.value = []
  }
}

function search() {
  const value = keyword.value.trim()
  if (value) router.push({ path: '/shops', query: { keyword: value } })
  else router.push({ path: '/shops', query: { type: '1', name: '食堂餐厅' } })
}

function goType(type) {
  router.push({ path: '/shops', query: { type: String(type.id), name: type.name } })
}

function goPublish() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/blog/new' } })
    return
  }
  router.push('/blog/new')
}

function discountText(voucher) {
  return discount(voucher.payValue, voucher.actualValue)
}

async function handleLike(blog) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('登录后才能点赞')
    router.push({ path: '/login', query: { redirect: '/' } })
    return
  }
  const wasLiked = blog.isLike
  try {
    await likeBlog(blog.id)
    blog.isLike = !wasLiked
    blog.liked = Math.max(0, (blog.liked || 0) + (wasLiked ? -1 : 1))
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.home {
  padding-top: 0;
}

.hero {
  position: relative;
  padding: 56px 0 48px;
  overflow: hidden;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  align-items: center;
  gap: 40px;
}

.hero-copy h1 {
  margin-top: 18px;
  font-size: 52px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.hero-copy h1 .grad {
  display: block;
  background: linear-gradient(110deg, var(--coral) 10%, #ff9f5a 55%, var(--butter));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-sub {
  max-width: 480px;
  margin-top: 18px;
  font-size: 15px;
  color: var(--ink-soft);
  line-height: 1.8;
}

.hero-search {
  display: flex;
  gap: 10px;
  margin-top: 26px;
  padding: 8px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-card);
}

.hero-search :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
}

.hero-search :deep(.el-button) {
  flex: none;
  padding: 0 30px;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.quick-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.quick-item:hover {
  border-color: var(--coral);
  color: var(--coral-dark);
  background: var(--coral-soft);
  transform: translateY(-1px);
}

.hero-art {
  position: relative;
  height: 340px;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(6px);
  opacity: 0.7;
}

.blob-a {
  width: 280px;
  height: 280px;
  right: 40px;
  top: 20px;
  background: radial-gradient(circle at 30% 30%, #ffe0c2, #ffd0b0);
}

.blob-b {
  width: 210px;
  height: 210px;
  left: 20px;
  bottom: 10px;
  background: radial-gradient(circle at 30% 30%, #e6f8f1, #c8efe2);
}

.float-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(8px);
  animation: float 5s ease-in-out infinite;
}

.float-card .emoji {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--cream-100);
  font-size: 22px;
}

.float-card strong {
  display: block;
  font-size: 14px;
  font-weight: 800;
}

.float-card em {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-soft);
}

.card-a {
  top: 34px;
  left: 18px;
}

.card-b {
  top: 138px;
  right: 6px;
  animation-delay: 0.8s;
}

.card-c {
  bottom: 30px;
  left: 76px;
  animation-delay: 1.6s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-9px);
  }
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 12px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 8px;
  border: 1px solid rgba(240, 226, 211, 0.8);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition: all 0.25s var(--ease);
}

.type-emoji {
  font-size: 26px;
  transition: transform 0.25s var(--ease);
}

.type-card:hover .type-emoji {
  transform: scale(1.15) rotate(-4deg);
}

.type-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-soft);
  white-space: nowrap;
}

.promo-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.promo-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  text-align: left;
  border: 1px dashed #ffc9b5;
  border-radius: var(--radius);
  background: linear-gradient(150deg, #fffaf3, #ffeadf);
  cursor: pointer;
}

.promo-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.promo-shop {
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-soft);
}

.promo-discount {
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff;
  color: var(--coral-dark);
  font-size: 11px;
  font-weight: 800;
}

.promo-title {
  font-size: 14px;
  font-weight: 800;
  line-height: 1.5;
  min-height: 42px;
}

.promo-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.promo-bottom .price {
  font-size: 22px;
}

.promo-origin {
  font-size: 11px;
  color: var(--ink-faint);
  text-decoration: line-through;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.sentinel {
  padding: 28px 0 8px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-faint);
}

.loading-text {
  color: var(--coral-dark);
}

@media (max-width: 1080px) {
  .type-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .promo-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero {
    padding: 32px 0 28px;
  }

  .hero-inner {
    grid-template-columns: 1fr;
  }

  .hero-art {
    display: none;
  }

  .hero-copy h1 {
    font-size: 38px;
  }

  .promo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .blog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    font-size: 32px;
  }

  .hero-search {
    flex-direction: column;
    border-radius: 22px;
  }

  .hero-search :deep(.el-button) {
    width: 100%;
  }

  .type-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .promo-grid,
  .blog-grid {
    grid-template-columns: 1fr;
  }
}
</style>

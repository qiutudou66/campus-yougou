<template>
  <div class="page shops">
    <div class="container">
      <PageHeader
        :title="pageTitle"
        :subtitle="pageSubtitle"
      >
        <template #actions>
          <div class="search-box">
            <el-input
              v-model="searchInput"
              placeholder="搜店铺、地址"
              clearable
              @keyup.enter="applySearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" round @click="applySearch">搜索</el-button>
          </div>
        </template>
      </PageHeader>

      <div class="layout">
        <aside class="sidebar card">
          <h3 class="side-title">店铺分类</h3>
          <button
            v-for="type in types"
            :key="type.id"
            type="button"
            class="side-item"
            :class="{ active: !isSearchMode && Number(route.query.type || 1) === type.id }"
            @click="selectType(type)"
          >
            <span class="side-emoji">{{ typeEmoji(type.id) }}</span>
            <span>{{ type.name }}</span>
          </button>

        </aside>

        <section class="content">
          <div class="toolbar">
            <div class="result-info">
              <strong>{{ isSearchMode ? '搜索结果' : currentTypeName }}</strong>
              <span class="muted">已加载 {{ shops.length }} 家</span>
            </div>
          </div>

          <div v-if="shops.length" class="shop-grid">
            <ShopCard v-for="shop in shops" :key="shop.id" :shop="shop" />
          </div>

          <div ref="sentinel" class="sentinel">
            <span v-if="loading">正在加载更多店铺…</span>
            <span v-else-if="finished && shops.length">— 已经到底啦 —</span>
          </div>

          <EmptyState
            v-if="finished && !shops.length"
            icon="🔍"
            title="没有找到相关店铺"
            description="换个关键词或者分类试试"
          />
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ShopCard from '@/components/ShopCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getShopTypes, getShopsByName, getShopsByType } from '@/api/shop'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

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

const CAMPUS_X = 112.9901
const CAMPUS_Y = 28.1801

const route = useRoute()
const router = useRouter()

const types = ref([])
const shops = ref([])
const searchInput = ref(String(route.query.keyword || ''))
const generation = ref(0)
let current = 1

const isSearchMode = computed(() => Boolean(route.query.keyword))
const keyword = computed(() => String(route.query.keyword || ''))
const pageSize = computed(() => (isSearchMode.value ? 10 : 5))

const currentTypeName = computed(() => {
  const id = Number(route.query.type || 1)
  return route.query.name || types.value.find((type) => type.id === id)?.name || '全部店铺'
})

const pageTitle = computed(() =>
  isSearchMode.value ? `搜索“${keyword.value}”` : currentTypeName.value
)

const pageSubtitle = computed(() =>
  isSearchMode.value
    ? '按店名、地址搜索校园店铺'
    : '按校区距离就近推荐，校园好店一网打尽'
)

const { sentinel, loading, finished, loadMore, reset } = useInfiniteScroll(loadPage)

onMounted(loadTypes)

watch(
  () => `${route.query.type || ''}|${route.query.name || ''}|${route.query.keyword || ''}`,
  () => {
    searchInput.value = String(route.query.keyword || '')
    reload()
  }
)

async function loadTypes() {
  try {
    types.value = (await getShopTypes()) || []
  } catch (error) {
    types.value = []
  }
}

async function loadPage() {
  const gen = generation.value
  const page = current

  const list = isSearchMode.value
    ? await getShopsByName({ name: keyword.value, current: page })
    : await getShopsByType({
        typeId: Number(route.query.type || 1),
        current: page,
        x: CAMPUS_X,
        y: CAMPUS_Y
      })

  if (gen !== generation.value) return true

  current = page + 1
  if (!list || !list.length) return false
  shops.value.push(...list)
  return list.length >= pageSize.value
}

async function reload() {
  generation.value += 1
  current = 1
  shops.value = []
  reset()
  await nextTick()
  while (loading.value) {
    await new Promise((resolve) => setTimeout(resolve, 40))
  }
  loadMore()
}

function selectType(type) {
  router.replace({
    path: '/shops',
    query: { type: String(type.id), name: type.name }
  })
}

function applySearch() {
  const value = searchInput.value.trim()
  if (value) {
    router.replace({ path: '/shops', query: { keyword: value } })
  } else {
    const typeId = String(route.query.type || 1)
    const name = route.query.name || currentTypeName.value
    router.replace({ path: '/shops', query: { type: typeId, name } })
  }
}

function typeEmoji(id) {
  return TYPE_EMOJI[id] || '🏫'
}
</script>

<style scoped>
.search-box {
  display: flex;
  gap: 8px;
  width: 340px;
}

.layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: calc(var(--nav-h) + 20px);
  padding: 18px 14px;
}

.side-title {
  padding: 0 10px 8px;
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}

.side-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s var(--ease);
}

.side-item:hover {
  background: var(--cream-100);
  color: var(--ink);
}

.side-item.active {
  background: var(--coral-soft);
  color: var(--coral-dark);
  font-weight: 800;
}

.side-emoji {
  width: 20px;
  text-align: center;
}

.content {
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 0 4px;
}

.result-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 14px;
}

.result-info strong {
  font-size: 17px;
  font-weight: 800;
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.sentinel {
  padding: 30px 0 8px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-faint);
}

@media (max-width: 1000px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 12px;
  }

  .side-title {
    display: none;
  }

  .side-item {
    width: auto;
  }
}

@media (max-width: 900px) {
  .search-box {
    width: 100%;
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .shop-grid {
    grid-template-columns: 1fr;
  }
}
</style>

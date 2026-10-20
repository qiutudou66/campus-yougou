<template>
  <div class="page user-profile">
    <div class="container">
      <PageHeader
        :title="user.nickName || '同学主页'"
        subtitle="看看这位同学最近在校园里发现了什么"
        crumb="校园动态 / 同学主页"
        back
      />

      <div v-if="notFound" class="card missing">
        <EmptyState
          icon="🫥"
          title="找不到这位同学"
          description="TA 可能换了账号，或者链接里的 ID 不对"
        >
          <el-button type="primary" round @click="router.push('/shops')">去逛逛</el-button>
        </EmptyState>
      </div>

      <div v-else class="profile-layout">
        <aside class="side">
          <ProfileCard
            :user="user"
            :info="info"
            :blog-count="blogs.length"
            :followed="followed"
            @follow="toggleFollow"
          />
        </aside>

        <section class="content card">
          <el-tabs v-model="activeTab" class="tabs">
            <el-tab-pane label="TA 的动态" name="blogs">
              <div v-if="blogs.length" class="blog-grid">
                <BlogCard
                  v-for="blog in blogs"
                  :key="blog.id"
                  :blog="blog"
                  :author-name="user.nickName"
                  :author-icon="user.icon"
                />
              </div>

              <div ref="sentinel" class="sentinel">
                <span v-if="loading">正在加载…</span>
                <span v-else-if="finished && blogs.length">— 已经到底啦 —</span>
              </div>

              <EmptyState
                v-if="finished && !blogs.length"
                icon="🍂"
                title="TA 还没有发布动态"
                description="等 TA 分享校园生活吧"
              />
            </el-tab-pane>

            <el-tab-pane label="共同关注" name="common">
              <div v-if="commonFollows.length" class="follow-list">
                <div v-for="item in commonFollows" :key="item.id" class="follow-item">
                  <img :src="item.icon || '/imgs/icons/default-icon.png'" alt="avatar" />
                  <div class="follow-copy">
                    <strong>{{ item.nickName }}</strong>
                    <span class="muted">你们都关注了 TA</span>
                  </div>
                  <el-button round @click="router.push(`/user/${item.id}`)">去主页</el-button>
                </div>
              </div>

              <EmptyState
                v-else
                icon="🤝"
                title="还没有共同关注"
                description="多逛逛校园动态，认识更多同学"
              />
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ProfileCard from '@/components/ProfileCard.vue'
import BlogCard from '@/components/BlogCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getUser, getUserInfo } from '@/api/user'
import { getBlogsOfUser } from '@/api/blog'
import { followUser, getCommonFollows, isFollowing } from '@/api/follow'
import { useAuthStore } from '@/stores/auth'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const user = ref({})
const info = ref({})
const blogs = ref([])
const commonFollows = ref([])
const followed = ref(false)
const notFound = ref(false)
const activeTab = ref('blogs')
const generation = ref(0)
let currentPage = 1
let commonLoadedFlag = false

const isSelf = computed(
  () => auth.user?.id && String(auth.user.id) === String(route.params.id)
)

const { sentinel, loading, finished, loadMore, reset } = useInfiniteScroll(async () => {
  const gen = generation.value
  const page = currentPage
  const list = await getBlogsOfUser({ userId: route.params.id, current: page })
  if (gen !== generation.value) return true
  currentPage = page + 1
  if (!list || !list.length) return false
  blogs.value.push(...list)
  return list.length >= 10
})

onMounted(load)
watch(() => route.params.id, load)

async function load() {
  const id = route.params.id
  if (!id) return
  if (!auth.user) await auth.fetchMe()
  if (auth.user && String(auth.user.id) === String(id)) {
    router.replace('/me')
    return
  }

  generation.value += 1
  reset()
  currentPage = 1
  blogs.value = []
  commonFollows.value = []
  commonLoadedFlag = false
  notFound.value = false
  activeTab.value = 'blogs'

  await nextTick()
  while (loading.value) {
    await new Promise((resolve) => setTimeout(resolve, 40))
  }
  loadMore()

  try {
    const [userData, userInfo, followState] = await Promise.all([
      getUser(id),
      getUserInfo(id, { silent: true }).catch(() => ({})),
      isFollowing(id, { silent: true }).catch(() => false)
    ])
    user.value = userData || {}
    info.value = userInfo || {}
    followed.value = Boolean(followState)
    notFound.value = !user.value.id
  } catch (error) {
    user.value = {}
    notFound.value = true
  }
}

watch(activeTab, (tab) => {
  if (tab === 'common' && !commonLoadedFlag) loadCommonFollows()
})

async function loadCommonFollows() {
  try {
    commonFollows.value = (await getCommonFollows(route.params.id)) || []
  } catch (error) {
    commonFollows.value = []
  } finally {
    commonLoadedFlag = true
  }
}

async function toggleFollow() {
  try {
    await followUser(route.params.id, !followed.value)
    followed.value = !followed.value
    ElMessage.success(followed.value ? '已关注' : '已取消关注')
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.missing {
  padding: 48px 24px;
}

.side {
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}

.content {
  padding: 12px 24px 26px;
  min-height: 420px;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.tabs :deep(.el-tabs__item) {
  font-size: 14px;
  height: 52px;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.sentinel {
  padding: 26px 0 6px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-faint);
}

.follow-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdfa;
}

.follow-item img {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  object-fit: cover;
}

.follow-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.follow-copy strong {
  font-size: 14px;
  font-weight: 800;
}

.follow-copy span {
  font-size: 12px;
}

@media (max-width: 1000px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
  }

  .blog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .blog-grid {
    grid-template-columns: 1fr;
  }
}
</style>

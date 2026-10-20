<template>
  <div class="page profile">
    <div class="container">
      <div class="profile-layout">
        <aside class="side">
          <ProfileCard
            :user="auth.user || {}"
            :info="info"
            :blog-count="myBlogs.length"
            is-self
            @edit="router.push('/me/edit')"
            @logout="handleLogout"
          />
        </aside>

        <section class="content card">
          <el-tabs v-model="activeTab" class="tabs">
            <el-tab-pane label="我的动态" name="blogs">
              <div v-if="myBlogs.length" class="blog-grid">
                <BlogCard
                  v-for="blog in myBlogs"
                  :key="blog.id"
                  :blog="blog"
                  :author-name="auth.user?.nickName"
                  :author-icon="auth.user?.icon"
                />
              </div>

              <div ref="blogsSentinel" class="sentinel">
                <span v-if="blogsLoading">正在加载…</span>
                <span v-else-if="blogsFinished && myBlogs.length">— 已经到底啦 —</span>
              </div>

              <EmptyState
                v-if="blogsFinished && !myBlogs.length"
                icon="📝"
                title="还没有发布动态"
                description="记录一下今天的校园生活吧"
              >
                <el-button type="primary" round @click="router.push('/blog/new')">去发布</el-button>
              </EmptyState>
            </el-tab-pane>

            <el-tab-pane label="关注动态" name="follow">
              <div v-if="feed.length" class="blog-grid">
                <BlogCard v-for="blog in feed" :key="blog.id" :blog="blog" />
              </div>

              <div ref="feedSentinel" class="sentinel">
                <span v-if="feedLoading">正在加载…</span>
                <span v-else-if="feedFinished && feed.length">— 已经到底啦 —</span>
              </div>

              <EmptyState
                v-if="feedFinished && !feed.length"
                icon="🌱"
                title="关注的同学还没有新动态"
                description="去发现页认识更多同学吧"
              >
                <el-button round @click="router.push('/')">去逛逛</el-button>
              </EmptyState>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProfileCard from '@/components/ProfileCard.vue'
import BlogCard from '@/components/BlogCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMyBlogs, getFollowFeed } from '@/api/blog'
import { getUserInfo } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('blogs')
const info = ref({})
const myBlogs = ref([])
const feed = ref([])

let blogPage = 1
let feedLoaded = false
let feedLastId = Date.now()
let feedOffset = 0

const {
  sentinel: blogsSentinel,
  loading: blogsLoading,
  finished: blogsFinished,
  loadMore: loadBlogs
} = useInfiniteScroll(async () => {
  const page = blogPage
  const list = await getMyBlogs(page)
  blogPage += 1
  if (!list || !list.length) return false
  myBlogs.value.push(...list)
  return list.length >= 10
})

const {
  sentinel: feedSentinel,
  loading: feedLoading,
  finished: feedFinished,
  loadMore: loadFeed
} = useInfiniteScroll(async () => {
  const result = await getFollowFeed({ lastId: feedLastId, offset: feedOffset })
  if (!result || !result.list || !result.list.length) return false
  feedLoaded = true
  feed.value.push(...result.list)
  feedLastId = result.minTime ?? feedLastId
  feedOffset = result.offset ?? 0
  return true
})

onMounted(async () => {
  const user = await auth.fetchMe(true)
  if (!user) return
  try {
    info.value = (await getUserInfo(user.id, { silent: true })) || {}
  } catch (error) {
    info.value = {}
  }
})

watch(activeTab, (tab) => {
  if (tab === 'follow' && !feedLoaded) loadFeed()
  if (tab === 'blogs' && !myBlogs.value.length) loadBlogs()
})

async function handleLogout() {
  await auth.logout()
  sessionStorage.removeItem('userInfo')
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
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

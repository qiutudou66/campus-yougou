<template>
  <header class="nav">
    <div class="nav-inner container">
      <router-link to="/" class="brand">
        <span class="brand-mark">🛍️</span>
        <span class="brand-text">
          <strong>校园优购</strong>
          <em>星城大学生活指南</em>
        </span>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-link" exact-active-class="is-active">首页</router-link>
        <router-link to="/shops" class="nav-link" active-class="is-active">逛店铺</router-link>
        <router-link to="/chat" class="nav-link nav-link--ai" active-class="is-active">
          AI 助手
          <span class="ai-dot"></span>
        </router-link>
        <router-link to="/me" class="nav-link" active-class="is-active">我的</router-link>
      </nav>

      <div class="nav-actions">
        <div class="nav-search">
          <el-input
            v-model="keyword"
            placeholder="搜店铺、地点"
            clearable
            @keyup.enter="goSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <span class="campus-pill">🏫 星城大学</span>

        <el-dropdown v-if="auth.isLoggedIn" trigger="click" @command="handleCommand">
          <button class="user-btn" type="button">
            <img :src="auth.user?.icon || '/imgs/icons/default-icon.png'" alt="avatar" />
            <span>{{ auth.user?.nickName || '同学' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="me">个人主页</el-dropdown-item>
              <el-dropdown-item command="new">发布动态</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button v-else type="primary" round @click="router.push('/login')">登录</el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')

onMounted(() => {
  auth.fetchMe()
})

function goSearch() {
  const value = keyword.value.trim()
  if (value) {
    router.push({ path: '/shops', query: { keyword: value } })
  } else {
    router.push({ path: '/shops', query: { type: '1', name: '食堂餐厅' } })
  }
}

async function handleCommand(command) {
  if (command === 'me') router.push('/me')
  if (command === 'new') router.push('/blog/new')
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--nav-h);
  background: rgba(255, 251, 245, 0.86);
  border-bottom: 1px solid rgba(240, 226, 211, 0.7);
  backdrop-filter: blur(14px);
}

.nav-inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  font-size: 21px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--coral), var(--butter));
  box-shadow: 0 8px 18px rgba(255, 122, 89, 0.28);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-text em {
  font-style: normal;
  font-size: 10px;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.nav-link {
  position: relative;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink-soft);
  transition: all 0.2s var(--ease);
}

.nav-link:hover {
  color: var(--ink);
  background: var(--cream-100);
}

.nav-link.is-active {
  color: var(--coral-dark);
  background: var(--coral-soft);
}

.nav-link--ai {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--lavender);
  box-shadow: 0 0 0 3px rgba(184, 167, 255, 0.22);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.nav-search {
  width: 190px;
}

.campus-pill {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--cream-100);
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.user-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.user-btn:hover {
  border-color: var(--coral);
  box-shadow: var(--shadow-card);
}

.user-btn img {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  object-fit: cover;
}

@media (max-width: 1080px) {
  .campus-pill,
  .nav-search {
    display: none;
  }
}

@media (max-width: 820px) {
  .nav-inner {
    gap: 12px;
  }

  .brand-text em {
    display: none;
  }

  .nav-links {
    gap: 0;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .nav-link {
    padding: 8px 10px;
    font-size: 13px;
    white-space: nowrap;
  }

  .user-btn span {
    display: none;
  }
}
</style>

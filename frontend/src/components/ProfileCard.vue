<template>
  <section class="profile-card card">
    <div class="cover">
      <span class="cover-emoji">🌿</span>
    </div>

    <div class="avatar-wrap">
      <img :src="user.icon || '/imgs/icons/default-icon.png'" alt="avatar" />
    </div>

    <div class="body">
      <h2>{{ user.nickName || '校园同学' }}</h2>
      <span class="school">🏫 星城大学 · 在读</span>
      <p class="bio">{{ info.introduce || '还没有简介，先打个招呼吧～' }}</p>

      <div class="stats">
        <div class="stat">
          <strong>{{ blogCount }}</strong>
          <span>动态</span>
        </div>
        <div class="stat">
          <strong>{{ genderText }}</strong>
          <span>性别</span>
        </div>
        <div class="stat">
          <strong>{{ info.city || '星城' }}</strong>
          <span>校区</span>
        </div>
      </div>

      <div class="actions">
        <template v-if="isSelf">
          <el-button type="primary" round class="main-btn" @click="$emit('edit')">编辑资料</el-button>
          <el-button round class="logout-btn" @click="$emit('logout')">退出登录</el-button>
        </template>
        <el-button
          v-else
          type="primary"
          round
          class="main-btn"
          :class="{ followed }"
          @click="$emit('follow')"
        >
          {{ followed ? '已关注' : '+ 关注' }}
        </el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: { type: Object, default: () => ({}) },
  info: { type: Object, default: () => ({}) },
  blogCount: { type: Number, default: 0 },
  isSelf: { type: Boolean, default: false },
  followed: { type: Boolean, default: false }
})

defineEmits(['edit', 'logout', 'follow'])

// 后端 gender 是布尔值：true 男 / false 女 / null 未设置
const genderText = computed(() => {
  if (props.info.gender === true) return '男'
  if (props.info.gender === false) return '女'
  return '—'
})
</script>

<style scoped>
.profile-card {
  overflow: hidden;
  padding-bottom: 24px;
}

.cover {
  height: 96px;
  background:
    radial-gradient(220px 130px at 20% 20%, rgba(255, 200, 87, 0.5), transparent 70%),
    radial-gradient(220px 140px at 85% 70%, rgba(111, 211, 181, 0.45), transparent 70%),
    linear-gradient(135deg, #fff1dc, #ffe4d4);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 24px;
}

.cover-emoji {
  font-size: 34px;
  opacity: 0.75;
  transform: rotate(10deg);
}

.avatar-wrap {
  width: 92px;
  height: 92px;
  margin: -46px 0 0 24px;
  border-radius: 28px;
  padding: 4px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(59, 47, 42, 0.12);
}

.avatar-wrap img {
  width: 100%;
  height: 100%;
  border-radius: 24px;
  object-fit: cover;
}

.body {
  padding: 14px 24px 0;
}

h2 {
  font-size: 21px;
  font-weight: 800;
}

.school {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-soft);
}

.bio {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--ink-soft);
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 18px;
  padding: 14px 0;
  border-top: 1px dashed var(--line);
  border-bottom: 1px dashed var(--line);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat strong {
  font-size: 16px;
  font-weight: 800;
  color: var(--ink);
}

.stat span {
  font-size: 11px;
  color: var(--ink-faint);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 18px;
}

.main-btn {
  width: 100%;
}

.main-btn.followed {
  background: var(--cream-100);
  border-color: var(--cream-200);
  color: var(--ink-soft);
}

.logout-btn {
  width: 100%;
  color: var(--ink-soft);
}
</style>

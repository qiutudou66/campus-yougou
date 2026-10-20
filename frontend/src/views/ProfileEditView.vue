<template>
  <div class="page profile-edit">
    <div class="container narrow">
      <PageHeader
        title="编辑资料"
        subtitle="展示你在校园优购里的公开信息"
        crumb="我的 / 编辑资料"
        back
      />

      <section class="notice card">
        <span class="notice-icon">🔒</span>
        <div>
          <strong>资料编辑暂未开放</strong>
          <p>后端目前没有提供资料更新接口，这一页先展示你的现有资料，等接口补齐后就能直接修改啦。</p>
        </div>
      </section>

      <section class="form-card card">
        <div class="row avatar-row">
          <span class="label">头像</span>
          <div class="value">
            <img :src="user.icon || '/imgs/icons/default-icon.png'" alt="avatar" />
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="row" @click="notAvailable">
          <span class="label">昵称</span>
          <div class="value">
            <span>{{ user.nickName || '未设置' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="row" @click="notAvailable">
          <span class="label">个人介绍</span>
          <div class="value">
            <span class="clamp-1">{{ info.introduce || '介绍一下自己' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="row" @click="notAvailable">
          <span class="label">性别</span>
          <div class="value">
            <span>{{ genderText }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="row" @click="notAvailable">
          <span class="label">校区</span>
          <div class="value">
            <span>{{ info.city || '星城大学' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="row" @click="notAvailable">
          <span class="label">生日</span>
          <div class="value">
            <span>{{ info.birthday || '未设置' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getUserInfo } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const info = ref({})

onMounted(async () => {
  const user = await auth.fetchMe(true)
  if (!user) return
  try {
    info.value = (await getUserInfo(user.id, { silent: true })) || {}
  } catch (error) {
    info.value = {}
  }
})

const user = auth.user || {}

const genderText = computed(() => {
  if (info.value.gender === true) return '男'
  if (info.value.gender === false) return '女'
  return '未选择'
})

function notAvailable() {
  ElMessage.info('资料编辑功能暂未开放，后端还没有更新接口')
}
</script>

<style scoped>
.narrow {
  width: min(760px, calc(100% - 48px));
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 22px;
  margin-bottom: 18px;
  background: linear-gradient(135deg, #fff8e8, #fff1d6);
  border: 1px solid #ffe0a8;
}

.notice-icon {
  font-size: 22px;
}

.notice strong {
  font-size: 14px;
  font-weight: 800;
}

.notice p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-soft);
}

.form-card {
  padding: 4px 22px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 4px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: opacity 0.2s var(--ease);
}

.row:last-child {
  border-bottom: none;
}

.row:hover {
  opacity: 0.72;
}

.label {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-soft);
}

.value {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 70%;
  font-size: 13px;
  color: var(--ink);
}

.value .clamp-1 {
  max-width: 320px;
}

.avatar-row {
  cursor: default;
}

.avatar-row:hover {
  opacity: 1;
}

.avatar-row img {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  object-fit: cover;
}

.value .el-icon {
  color: var(--ink-faint);
}
</style>

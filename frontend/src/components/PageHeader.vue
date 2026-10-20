<template>
  <div class="page-header">
    <div class="left">
      <button v-if="back" class="back" type="button" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div>
        <div v-if="crumb" class="crumb">{{ crumb }}</div>
        <h1 class="title">{{ title }}</h1>
        <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  crumb: { type: String, default: '' },
  back: { type: Boolean, default: false }
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 26px;
}

.left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back {
  width: 42px;
  height: 42px;
  flex: none;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--ink);
  font-size: 17px;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.back:hover {
  border-color: var(--coral);
  color: var(--coral-dark);
  transform: translateX(-2px);
}

.crumb {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-faint);
  letter-spacing: 0.04em;
}

.title {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink-soft);
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 900px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .title {
    font-size: 24px;
  }
}
</style>

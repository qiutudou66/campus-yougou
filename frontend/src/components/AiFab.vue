<template>
  <router-link v-if="route.path !== '/chat'" to="/chat" class="ai-fab">
    <span class="spark">✨</span>
    <span class="label">问 AI 助手</span>
  </router-link>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.ai-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 90;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 50px;
  padding: 0 20px;
  border-radius: 999px;
  background: linear-gradient(135deg, #a68cff, #7f66e8);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 14px 30px rgba(127, 102, 232, 0.34);
  transition:
    transform 0.25s var(--ease),
    box-shadow 0.25s var(--ease);
}

.ai-fab:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 18px 38px rgba(127, 102, 232, 0.42);
}

.spark {
  font-size: 16px;
}

@media (max-width: 640px) {
  .ai-fab {
    right: 16px;
    bottom: 16px;
    height: 46px;
    padding: 0 16px;
  }

  .label {
    display: none;
  }
}
</style>

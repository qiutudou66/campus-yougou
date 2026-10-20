<template>
  <div class="empty">
    <div class="icon">{{ icon }}</div>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="action">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '🍃' },
  title: { type: String, default: '这里还空空的' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.empty {
  padding: 54px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.icon {
  width: 74px;
  height: 74px;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: var(--cream-100);
  font-size: 32px;
  margin-bottom: 16px;
}

h3 {
  font-size: 16px;
  font-weight: 800;
}

p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ink-soft);
}

.action {
  margin-top: 18px;
}
</style>

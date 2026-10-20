<template>
  <AppNav />
  <main class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
  <AppFooter />
  <AiFab />
</template>

<script setup>
import AppNav from '@/components/AppNav.vue'
import AppFooter from '@/components/AppFooter.vue'
import AiFab from '@/components/AiFab.vue'
</script>

<style scoped>
.app-main {
  flex: 1;
  width: 100%;
}
</style>

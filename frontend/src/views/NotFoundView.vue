<template>
  <div class="page not-found">
    <div class="container content">
      <div class="emoji">🧭</div>
      <h1>404</h1>
      <h2>这条校园小路走不通</h2>
      <p>页面可能被搬走了，或者链接输错了。</p>
      <div class="actions">
        <el-button type="primary" size="large" round @click="router.push('/')">回到首页</el-button>
        <el-button size="large" round @click="router.push('/shops')">去逛店铺</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
}

.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 0 80px;
}

.emoji {
  width: 110px;
  height: 110px;
  display: grid;
  place-items: center;
  border-radius: 36px;
  background: var(--cream-100);
  font-size: 52px;
  margin-bottom: 24px;
}

h1 {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(120deg, var(--coral), var(--butter));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h2 {
  margin-top: 12px;
  font-size: 24px;
  font-weight: 800;
}

p {
  margin-top: 8px;
  color: var(--ink-soft);
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}
</style>

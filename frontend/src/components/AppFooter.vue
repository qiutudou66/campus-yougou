<template>
  <footer class="footer">
    <div class="footer-inner container">
      <div class="footer-brand">
        <span class="mark">🛍️</span>
        <div>
          <strong>校园优购</strong>
          <p>发现校园里的每一份小确幸</p>
        </div>
      </div>

      <div class="footer-links">
        <router-link to="/">首页</router-link>
        <router-link to="/shops">逛店铺</router-link>
        <router-link to="/chat">AI 助手</router-link>
        <router-link to="/me">我的</router-link>
      </div>

      <p class="footer-copy">© {{ year }} 校园优购 · 星城大学</p>
    </div>
  </footer>
</template>

<script setup>
const year = new Date().getFullYear()
</script>

<style scoped>
.footer {
  margin-top: auto;
  border-top: 1px solid rgba(240, 226, 211, 0.8);
  background: rgba(255, 244, 230, 0.55);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 0;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-brand .mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--coral), var(--butter));
  font-size: 18px;
}

.footer-brand strong {
  font-size: 15px;
  font-weight: 800;
}

.footer-brand p {
  font-size: 12px;
  color: var(--ink-soft);
}

.footer-links {
  display: flex;
  gap: 22px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
}

.footer-links a:hover {
  color: var(--coral-dark);
}

.footer-copy {
  font-size: 12px;
  color: var(--ink-faint);
}

@media (max-width: 900px) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>

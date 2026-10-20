<template>
  <div class="page login-page">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-panel-copy">
          <span class="pill pill--coral">✨ 星城大学 · 校园生活入口</span>
          <h1>
            发现校园里的
            <span>每一份小确幸</span>
          </h1>
          <p>逛店铺、抢优惠、记录生活，还有一个随时在线的 AI 助手。</p>

          <div class="brand-points">
            <div class="point">
              <span class="point-icon">🏫</span>
              <div>
                <strong>校园好店</strong>
                <em>食堂、咖啡、打印店一网打尽</em>
              </div>
            </div>
            <div class="point">
              <span class="point-icon">🎟️</span>
              <div>
                <strong>限时优惠</strong>
                <em>校园专属券，抢到就是赚到</em>
              </div>
            </div>
            <div class="point">
              <span class="point-icon">🤖</span>
              <div>
                <strong>AI 助手</strong>
                <em>今天吃什么，交给它决定</em>
              </div>
            </div>
          </div>
        </div>

        <div class="brand-deco" aria-hidden="true">
          <span class="deco deco-a">☕</span>
          <span class="deco deco-b">🍚</span>
          <span class="deco deco-c">📚</span>
          <span class="deco deco-d">🎟️</span>
        </div>
      </section>

      <section class="form-panel">
        <h2>欢迎回来 👋</h2>
        <p class="form-sub">手机号验证后即可登录，未注册会自动创建账号。</p>

        <div class="field">
          <label>手机号</label>
          <el-input v-model="form.phone" size="large" maxlength="11" placeholder="请输入手机号">
            <template #prefix>
              <el-icon><Iphone /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="field">
          <label>验证码</label>
          <div class="code-row">
            <el-input v-model="form.code" size="large" maxlength="6" placeholder="请输入 6 位验证码" />
            <el-button class="code-btn" size="large" :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </div>

        <el-checkbox v-model="agreed" class="agree">
          我已阅读并同意
          <span class="link">《校园优购用户服务协议》</span>
          和
          <span class="link">《隐私政策》</span>
        </el-checkbox>

        <el-button class="submit" type="primary" size="large" :loading="submitting" @click="handleLogin">
          登录 / 注册
        </el-button>

        <p class="tip">提示：当前为校园课程项目，验证码由后端生成，请查看后端日志获取。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone } from '@element-plus/icons-vue'
import { loginByCode, sendCode } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ phone: '', code: '' })
const agreed = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let timer = null

onBeforeUnmount(() => window.clearInterval(timer))

function validPhone() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return false
  }
  return true
}

async function handleSendCode() {
  if (!validPhone()) return
  try {
    await sendCode(form.phone)
    ElMessage.success('验证码已发送，请查看后端日志')
    startCountdown()
  } catch (error) {
    // 拦截器已提示
  }
}

function startCountdown() {
  countdown.value = 60
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) window.clearInterval(timer)
  }, 1000)
}

async function handleLogin() {
  if (!validPhone()) return
  if (!form.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意用户协议')
    return
  }

  submitting.value = true
  try {
    const token = await loginByCode({ phone: form.phone, code: form.code.trim() })
    auth.setToken(token)
    await auth.fetchMe(true)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 0 72px;
}

.login-shell {
  width: min(1040px, calc(100% - 48px));
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: #fff;
  box-shadow: 0 30px 80px rgba(120, 84, 50, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.brand-panel {
  position: relative;
  padding: 54px 48px;
  background:
    radial-gradient(420px 300px at 85% 10%, rgba(255, 200, 87, 0.35), transparent 65%),
    radial-gradient(380px 300px at 10% 90%, rgba(111, 211, 181, 0.28), transparent 65%),
    linear-gradient(145deg, #fff4e4, #ffe6d8 60%, #ffdccb);
  overflow: hidden;
}

.brand-panel-copy h1 {
  margin-top: 20px;
  font-size: 40px;
  font-weight: 800;
  line-height: 1.22;
  letter-spacing: -0.03em;
}

.brand-panel-copy h1 span {
  display: block;
  background: linear-gradient(110deg, var(--coral), #ff9f5a);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-panel-copy > p {
  margin-top: 16px;
  max-width: 340px;
  font-size: 14px;
  line-height: 1.8;
  color: #7c6556;
}

.brand-points {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 34px;
}

.point {
  display: flex;
  align-items: center;
  gap: 12px;
}

.point-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
  font-size: 20px;
  box-shadow: 0 6px 16px rgba(120, 84, 50, 0.08);
}

.point strong {
  display: block;
  font-size: 13px;
  font-weight: 800;
}

.point em {
  font-style: normal;
  font-size: 11px;
  color: #8a7364;
}

.deco {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 34px rgba(120, 84, 50, 0.12);
  animation: bob 5s ease-in-out infinite;
}

.deco-a {
  width: 66px;
  height: 66px;
  font-size: 30px;
  right: 26px;
  top: 22px;
}

.deco-b {
  width: 54px;
  height: 54px;
  font-size: 24px;
  right: 76px;
  bottom: 118px;
  animation-delay: 0.7s;
}

.deco-c {
  width: 46px;
  height: 46px;
  font-size: 20px;
  right: 22px;
  bottom: 34px;
  animation-delay: 1.4s;
}

.deco-d {
  width: 40px;
  height: 40px;
  font-size: 18px;
  left: -8px;
  bottom: 150px;
  animation-delay: 2.1s;
}

@keyframes bob {
  0%,
  100% {
    transform: translateY(0) rotate(-3deg);
  }
  50% {
    transform: translateY(-8px) rotate(3deg);
  }
}

.form-panel {
  padding: 58px 52px;
  display: flex;
  flex-direction: column;
}

.form-panel h2 {
  font-size: 26px;
  font-weight: 800;
}

.form-sub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink-soft);
}

.field {
  margin-top: 24px;
}

.field label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-soft);
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-btn {
  flex: none;
  width: 132px;
}

.agree {
  margin-top: 18px;
  font-size: 12px;
  color: var(--ink-soft);
}

.link {
  color: var(--coral-dark);
  cursor: pointer;
}

.submit {
  width: 100%;
  height: 48px;
  margin-top: 22px;
  font-size: 15px;
}

.tip {
  margin-top: 16px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-faint);
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 36px 30px;
  }

  .brand-panel-copy h1 {
    font-size: 30px;
  }

  .brand-points {
    display: none;
  }

  .form-panel {
    padding: 36px 30px;
  }
}
</style>

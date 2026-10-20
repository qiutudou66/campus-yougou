<template>
  <div class="voucher" :class="{ 'is-ended': status === 'ended' }">
    <div class="left">
      <div class="title">{{ voucher.title }}</div>
      <div v-if="voucher.subTitle" class="subtitle clamp-1">{{ voucher.subTitle }}</div>
      <div class="price-row">
        <span class="price"><small>￥</small>{{ formatPrice(voucher.payValue) }}</span>
        <span v-if="discountText" class="discount">{{ discountText }} 折</span>
      </div>
    </div>

    <div class="right">
      <template v-if="voucher.type">
        <button
          class="seckill-btn"
          :class="{ disabled: !canSeckill }"
          type="button"
          @click="canSeckill && $emit('seckill', voucher)"
        >
          {{ buttonText }}
        </button>
        <span class="stock">剩 {{ Math.max(voucher.stock || 0, 0) }} 张</span>
        <span class="countdown">{{ countdownText }}</span>
      </template>
      <div v-else class="normal-tag">到店可用</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { discount as getDiscount, formatPrice } from '@/utils/format'

const props = defineProps({
  voucher: { type: Object, required: true }
})

defineEmits(['seckill'])

const now = ref(Date.now())
let timer = null

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => window.clearInterval(timer))

function parseTime(value) {
  if (!value) return null
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date.getTime()
}

const beginTime = computed(() => parseTime(props.voucher.beginTime))
const endTime = computed(() => parseTime(props.voucher.endTime))

const status = computed(() => {
  if (beginTime.value && now.value < beginTime.value) return 'soon'
  if (endTime.value && now.value > endTime.value) return 'ended'
  return 'active'
})

const canSeckill = computed(
  () => status.value === 'active' && Number(props.voucher.stock) > 0
)

const discountText = computed(() =>
  getDiscount(props.voucher.payValue, props.voucher.actualValue)
)

const buttonText = computed(() => {
  if (status.value === 'soon') return '即将开始'
  if (status.value === 'ended') return '已结束'
  if (Number(props.voucher.stock) < 1) return '已抢光'
  return '限时抢'
})

function formatRemain(ms) {
  if (ms <= 0) return ''
  const total = Math.floor(ms / 1000)
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}时${minutes}分`
  return `${minutes}分${String(seconds).padStart(2, '0')}秒`
}

const countdownText = computed(() => {
  if (status.value === 'soon' && beginTime.value) {
    return `${formatRemain(beginTime.value - now.value)}后开始`
  }
  if (status.value === 'active' && endTime.value) {
    return `剩 ${formatRemain(endTime.value - now.value)}`
  }
  return ''
})
</script>

<style scoped>
.voucher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #fff8ef, #ffeadf);
  border: 1px dashed #ffc9b5;
}

.voucher.is-ended {
  opacity: 0.58;
}

.left {
  min-width: 0;
}

.title {
  font-size: 15px;
  font-weight: 800;
}

.subtitle {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ink-soft);
}

.price-row {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price {
  font-size: 24px;
  font-weight: 800;
  color: var(--coral-dark);
}

.price small {
  font-size: 13px;
}

.discount {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--coral-soft);
  color: var(--coral-dark);
  font-size: 11px;
  font-weight: 800;
}

.right {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 100px;
}

.seckill-btn {
  width: 100%;
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--coral), var(--coral-dark));
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.seckill-btn:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(244, 99, 63, 0.3);
}

.seckill-btn.disabled {
  background: #e8dcd0;
  color: #a5968a;
  cursor: not-allowed;
}

.stock {
  font-size: 11px;
  color: var(--ink-soft);
}

.countdown {
  font-size: 11px;
  color: var(--coral-dark);
  font-weight: 700;
}

.normal-tag {
  padding: 6px 14px;
  border-radius: 999px;
  background: #fff;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 700;
  border: 1px solid var(--line);
}
</style>

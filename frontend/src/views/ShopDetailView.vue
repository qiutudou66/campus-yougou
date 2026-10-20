<template>
  <div class="page shop-detail">
    <div class="container">
      <PageHeader
        :title="shop.name || '店铺详情'"
        :subtitle="shop.area ? `${shop.area} · ${shop.comments || 0} 条评价` : '校园好店'"
        crumb="逛店铺 / 店铺详情"
        back
      >
        <template #actions>
          <el-button round @click="router.push('/chat')">
            <el-icon><ChatDotRound /></el-icon>
            <span style="margin-left: 6px">问问 AI</span>
          </el-button>
        </template>
      </PageHeader>

      <div class="detail-layout">
        <div class="main">
          <section class="gallery card">
            <div class="hero-image">
              <img :src="activeImage || fallbackImage" :alt="shop.name" />
            </div>
            <div v-if="images.length > 1" class="thumbs">
              <button
                v-for="image in images"
                :key="image"
                type="button"
                :class="{ active: image === activeImage }"
                @click="activeImage = image"
              >
                <img :src="image" alt="店铺图片" />
              </button>
            </div>
          </section>

          <section class="info card">
            <div class="info-head">
              <div>
                <h2 class="shop-name">{{ shop.name || '加载中…' }}</h2>
                <div class="rating">
                  <el-rate
                    :model-value="Number(shop.score || 0) / 10"
                    disabled
                    show-score
                    text-color="#f4a018"
                    score-template="{value}"
                  />
                  <span class="muted">{{ shop.comments || 0 }} 条评价</span>
                </div>
              </div>
              <div class="tags">
                <span v-if="shop.area" class="pill pill--mint">{{ shop.area }}</span>
                <span v-if="shop.avgPrice > 0" class="pill">人均 ￥{{ shop.avgPrice }}</span>
                <span v-else class="pill">免费开放</span>
                <span v-if="shop.sold" class="pill pill--coral">已售 {{ shop.sold }}</span>
              </div>
            </div>

            <div class="meta-list">
              <div class="meta-row">
                <el-icon><Location /></el-icon>
                <span>{{ shop.address || '地址待补充' }}</span>
              </div>
              <div class="meta-row">
                <el-icon><Clock /></el-icon>
                <span>营业时间 {{ shop.openHours || '以店内公告为准' }}</span>
              </div>
            </div>
          </section>

          <section class="comments card">
            <div class="card-head">
              <h3>店铺评价</h3>
              <span class="muted">{{ shop.comments || 0 }} 条</span>
            </div>
            <EmptyState
              icon="💬"
              title="评价功能正在路上"
              description="先去校园动态看看同学们最近在聊什么"
            >
              <el-button round @click="router.push('/')">看看校园动态</el-button>
            </EmptyState>
          </section>
        </div>

        <aside class="aside">
          <section class="sidebar-card card">
            <div class="card-head">
              <h3>校园优惠</h3>
              <span class="pill pill--coral">{{ activeVouchers.length }} 张可用</span>
            </div>

            <div v-if="activeVouchers.length" class="voucher-list">
              <VoucherCard
                v-for="voucher in activeVouchers"
                :key="voucher.id"
                :voucher="voucher"
                @seckill="handleSeckill"
              />
            </div>
            <EmptyState
              v-else
              icon="🎟️"
              title="暂无优惠"
              description="这家店还没有上架优惠券"
            />

            <el-button class="ai-btn" type="primary" size="large" @click="router.push('/chat')">
              <el-icon><MagicStick /></el-icon>
              <span style="margin-left: 8px">让 AI 推荐怎么点</span>
            </el-button>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Clock, Location, MagicStick } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import VoucherCard from '@/components/VoucherCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getShop } from '@/api/shop'
import { getVouchersByShop, seckillVoucher } from '@/api/voucher'
import { useAuthStore } from '@/stores/auth'
import { splitImages } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const shop = ref({})
const vouchers = ref([])
const activeImage = ref('')
const fallbackImage = '/imgs/shops/shop01.jpg'

const images = computed(() => splitImages(shop.value.images))
const activeVouchers = computed(() => {
  const now = Date.now()
  return vouchers.value.filter((voucher) => {
    if (!voucher.endTime) return true
    return new Date(String(voucher.endTime).replace(' ', 'T')).getTime() > now
  })
})

onMounted(load)
watch(() => route.params.id, load)

async function load() {
  if (!route.params.id) return
  activeImage.value = ''
  try {
    const [shopData, voucherList] = await Promise.all([
      getShop(route.params.id),
      getVouchersByShop(route.params.id)
    ])
    shop.value = shopData || {}
    vouchers.value = voucherList || []
    activeImage.value = images.value[0] || ''
  } catch (error) {
    shop.value = {}
  }
}

async function refreshVouchers() {
  try {
    vouchers.value = (await getVouchersByShop(route.params.id)) || []
  } catch (error) {
    // 忽略刷新失败
  }
}

async function handleSeckill(voucher) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('登录后才能抢购')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  try {
    const orderId = await seckillVoucher(voucher.id)
    ElMessage.success(`抢购成功，订单号 ${orderId}`)
    refreshVouchers()
  } catch (error) {
    // 拦截器已提示库存不足/重复下单等
  }
}
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  align-items: start;
}

.main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.gallery {
  padding: 12px;
}

.hero-image {
  aspect-ratio: 16 / 8;
  overflow: hidden;
  border-radius: 14px;
  background: var(--cream-100);
}

.hero-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.thumbs button {
  flex: none;
  width: 86px;
  height: 64px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  background: var(--cream-100);
  cursor: pointer;
  transition: border-color 0.2s var(--ease);
}

.thumbs button.active {
  border-color: var(--coral);
}

.thumbs img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  padding: 26px 28px;
}

.info-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.shop-name {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.rating {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  font-size: 12px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ink-soft);
}

.meta-row .el-icon {
  color: var(--coral);
}

.comments {
  padding: 24px 28px 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.card-head h3 {
  font-size: 17px;
  font-weight: 800;
}

.aside {
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}

.sidebar-card {
  padding: 22px;
}

.voucher-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-btn {
  width: 100%;
  margin-top: 18px;
  background: linear-gradient(135deg, #a68cff, #7f66e8);
  border: none;
}

.ai-btn:hover {
  background: linear-gradient(135deg, #9a7eff, #7057de);
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .aside {
    position: static;
  }
}

@media (max-width: 640px) {
  .info-head {
    flex-direction: column;
  }

  .tags {
    justify-content: flex-start;
  }

  .info,
  .comments {
    padding: 20px;
  }
}
</style>

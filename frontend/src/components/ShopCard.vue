<template>
  <router-link :to="`/shop/${shop.id}`" class="shop-card hover-lift">
    <div class="cover">
      <img :src="cover" :alt="shop.name" />
      <span v-if="shop.distance" class="distance">{{ formatDistance(shop.distance) }}</span>
      <span v-if="shop.avgPrice > 0" class="price-tag">￥{{ shop.avgPrice }}/人</span>
      <span v-else class="price-tag">免费</span>
    </div>

    <div class="body">
      <div class="title-row">
        <h3 class="name clamp-1">{{ shop.name }}</h3>
        <span class="score">★ {{ scoreText }}</span>
      </div>
      <p class="area">{{ shop.area }} · {{ shop.comments || 0 }} 条评价</p>
      <p class="address clamp-1">
        <el-icon><Location /></el-icon>
        {{ shop.address }}
      </p>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { Location } from '@element-plus/icons-vue'
import { formatDistance, splitImages } from '@/utils/format'

const props = defineProps({
  shop: { type: Object, required: true }
})

const cover = computed(() => splitImages(props.shop.images)[0] || '/imgs/shops/shop01.jpg')
const scoreText = computed(() => ((Number(props.shop.score) || 0) / 10).toFixed(1))
</script>

<style scoped>
.shop-card {
  display: block;
  overflow: hidden;
  border-radius: var(--radius);
  background: #fff;
  border: 1px solid rgba(240, 226, 211, 0.8);
  box-shadow: var(--shadow-card);
}

.cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--cream-100);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease);
}

.shop-card:hover .cover img {
  transform: scale(1.05);
}

.distance,
.price-tag {
  position: absolute;
  bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 251, 245, 0.92);
  color: var(--ink);
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(6px);
}

.distance {
  left: 10px;
}

.price-tag {
  right: 10px;
  color: var(--coral-dark);
}

.body {
  padding: 16px 18px 18px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-size: 17px;
  font-weight: 800;
}

.score {
  flex: none;
  color: var(--butter);
  font-size: 13px;
  font-weight: 800;
  -webkit-text-stroke: 0.2px #e8a018;
}

.area {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-soft);
}

.address {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-faint);
}
</style>

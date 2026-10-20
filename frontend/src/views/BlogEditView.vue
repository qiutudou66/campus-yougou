<template>
  <div class="page blog-edit">
    <div class="container">
      <PageHeader title="发布动态" subtitle="记录校园里的小发现，分享给同学" crumb="校园动态 / 发布" back>
        <template #actions>
          <el-button round @click="router.back()">取消</el-button>
          <el-button type="primary" round :loading="submitting" @click="submit">发布</el-button>
        </template>
      </PageHeader>

      <div class="edit-layout">
        <section class="composer card">
          <div class="block">
            <label class="block-label">照片</label>
            <ImageUploader v-model="images" :max="9" />
            <p class="hint">最多 9 张，单张不超过 10MB</p>
          </div>

          <div class="block">
            <label class="block-label">标题</label>
            <el-input
              v-model="title"
              size="large"
              maxlength="40"
              show-word-limit
              placeholder="加个标题，让更多同学看到"
            />
          </div>

          <div class="block">
            <label class="block-label">正文</label>
            <el-input
              v-model="content"
              type="textarea"
              :rows="9"
              maxlength="1000"
              show-word-limit
              resize="vertical"
              placeholder="最近在校园里发现了什么好吃的、好用的？分享给同学吧～"
            />
          </div>
        </section>

        <aside class="aside">
          <section class="side-card card">
            <h3>关联商户</h3>
            <p class="muted side-desc">关联店铺后，同学可以直接从动态跳过去。</p>
            <button class="shop-select" type="button" @click="openShopDialog">
              <template v-if="selectedShop">
                <span class="shop-emoji">🏪</span>
                <span class="shop-copy">
                  <strong>{{ selectedShop.name }}</strong>
                  <em>{{ selectedShop.area }}</em>
                </span>
                <el-icon><ArrowRight /></el-icon>
              </template>
              <template v-else>
                <span class="shop-emoji">➕</span>
                <span class="shop-copy">
                  <strong>选择一家店铺</strong>
                  <em>可选</em>
                </span>
                <el-icon><ArrowRight /></el-icon>
              </template>
            </button>
            <el-button v-if="selectedShop" text class="clear-shop" @click="selectedShop = null">
              取消关联
            </el-button>
          </section>

          <section class="side-card card tips">
            <h3>发布小贴士</h3>
            <ul>
              <li>🍜 拍下好吃的那一刻，比文字更有说服力</li>
              <li>📍 关联店铺，方便同学顺路找到</li>
              <li>🎟️ 有优惠券的店，记得提醒大家去领</li>
            </ul>
          </section>
        </aside>
      </div>

      <el-dialog v-model="shopDialog" title="选择关联商户" width="560px">
        <el-input
          v-model="shopKeyword"
          placeholder="搜索校园店铺"
          clearable
          @input="onShopInput"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="dialog-list">
          <button
            v-for="item in shopOptions"
            :key="item.id"
            type="button"
            class="dialog-item"
            @click="chooseShop(item)"
          >
            <span class="dialog-emoji">🏪</span>
            <span class="dialog-copy">
              <strong>{{ item.name }}</strong>
              <em>{{ item.area }}</em>
            </span>
          </button>
          <p v-if="!shopOptions.length" class="dialog-empty">没有找到相关店铺</p>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { createBlog } from '@/api/blog'
import { getShopsByName } from '@/api/shop'

const router = useRouter()

const images = ref([])
const title = ref('')
const content = ref('')
const submitting = ref(false)

const shopDialog = ref(false)
const shopKeyword = ref('')
const shopOptions = ref([])
const selectedShop = ref(null)

let searchTimer = null

onBeforeUnmount(() => window.clearTimeout(searchTimer))

function openShopDialog() {
  shopDialog.value = true
  if (!shopOptions.value.length) queryShops('')
}

function onShopInput() {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => queryShops(shopKeyword.value.trim()), 260)
}

async function queryShops(name) {
  try {
    shopOptions.value = (await getShopsByName({ name, current: 1 })) || []
  } catch (error) {
    shopOptions.value = []
  }
}

function chooseShop(shop) {
  selectedShop.value = shop
  shopDialog.value = false
}

async function submit() {
  if (!title.value.trim()) {
    ElMessage.warning('给动态加个标题吧')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('正文还没有写呢')
    return
  }

  submitting.value = true
  try {
    await createBlog({
      shopId: selectedShop.value?.id,
      title: title.value.trim(),
      images: images.value.join(','),
      content: content.value.trim()
    })
    ElMessage.success('发布成功')
    router.push('/me')
  } catch (error) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.edit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 24px;
  align-items: start;
}

.composer {
  padding: 28px 30px 34px;
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.block-label {
  display: block;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
}

.hint {
  margin-top: 10px;
  font-size: 11px;
  color: var(--ink-faint);
}

.aside {
  position: sticky;
  top: calc(var(--nav-h) + 20px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  padding: 22px;
}

.side-card h3 {
  font-size: 15px;
  font-weight: 800;
}

.side-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
}

.shop-select {
  width: 100%;
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdfa;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.shop-select:hover {
  border-color: var(--coral);
  background: var(--coral-soft);
}

.shop-emoji {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--cream-100);
  font-size: 18px;
}

.shop-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.shop-copy strong {
  font-size: 13px;
  font-weight: 800;
}

.shop-copy em {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-soft);
}

.clear-shop {
  margin-top: 8px;
  color: var(--ink-soft);
  font-size: 12px;
}

.tips ul {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  font-size: 12px;
  color: var(--ink-soft);
  line-height: 1.6;
}

.dialog-list {
  max-height: 360px;
  overflow-y: auto;
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdfa;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.dialog-item:hover {
  border-color: var(--coral);
  background: var(--coral-soft);
}

.dialog-emoji {
  font-size: 18px;
}

.dialog-copy {
  display: flex;
  flex-direction: column;
}

.dialog-copy strong {
  font-size: 13px;
  font-weight: 800;
}

.dialog-copy em {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-soft);
}

.dialog-empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--ink-faint);
}

@media (max-width: 1000px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }

  .aside {
    position: static;
  }
}

@media (max-width: 640px) {
  .composer {
    padding: 22px 18px 26px;
  }
}
</style>

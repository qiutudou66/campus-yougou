<template>
  <router-link :to="`/blog/${blog.id}`" class="blog-card hover-lift">
    <div class="cover">
      <img :src="cover" :alt="blog.title" />
    </div>
    <div class="body">
      <h3 class="title clamp-2">{{ blog.title }}</h3>
      <div class="meta">
        <div class="author">
          <img :src="blog.icon || authorIcon || '/imgs/icons/default-icon.png'" alt="avatar" />
          <span class="clamp-1">{{ blog.name || authorName || '校园同学' }}</span>
        </div>
        <button class="likes" :class="{ liked: blog.isLike }" type="button" @click.prevent.stop="$emit('like', blog)">
          ♥ {{ blog.liked || 0 }}
        </button>
      </div>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { splitImages } from '@/utils/format'

const props = defineProps({
  blog: { type: Object, required: true },
  // /blog/of/me、/blog/of/user 不返回作者名，由页面传入当前主页用户
  authorName: { type: String, default: '' },
  authorIcon: { type: String, default: '' }
})

defineEmits(['like'])

const cover = computed(() => splitImages(props.blog.images)[0] || '/imgs/blogs/blog1.jpg')
</script>

<style scoped>
.blog-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius);
  background: #fff;
  border: 1px solid rgba(240, 226, 211, 0.8);
  box-shadow: var(--shadow-card);
}

.cover {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--cream-100);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease);
}

.blog-card:hover .cover img {
  transform: scale(1.05);
}

.body {
  padding: 14px 16px 16px;
}

.title {
  min-height: 44px;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.5;
}

.meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.author {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  font-size: 12px;
  color: var(--ink-soft);
}

.author img {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  object-fit: cover;
}

.likes {
  flex: none;
  padding: 3px 8px;
  border: none;
  border-radius: 999px;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  color: var(--coral);
  cursor: pointer;
  transition: background 0.2s var(--ease);
}

.likes:hover {
  background: var(--coral-soft);
}

.likes.liked {
  background: var(--coral-soft);
  color: var(--coral-dark);
}
</style>

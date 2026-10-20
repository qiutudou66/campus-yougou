<template>
  <div class="uploader">
    <div v-for="(url, index) in modelValue" :key="url + index" class="tile">
      <img :src="url" alt="已上传图片" />
      <button class="remove" type="button" @click="remove(index)">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <button
      v-if="modelValue.length < max"
      class="tile add"
      type="button"
      :disabled="uploading"
      @click="pick"
    >
      <template v-if="uploading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>上传中</span>
      </template>
      <template v-else>
        <el-icon><Plus /></el-icon>
        <span>添加照片</span>
      </template>
    </button>

    <input
      ref="inputRef"
      type="file"
      accept="image/*"
      multiple
      hidden
      @change="handleFiles"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Loading, Plus } from '@element-plus/icons-vue'
import { deleteBlogImage, uploadBlogImage } from '@/api/upload'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 9 }
})

const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)
const uploading = ref(false)

function pick() {
  inputRef.value?.click()
}

async function handleFiles(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return

  const remain = props.max - props.modelValue.length
  const selected = files.slice(0, remain)
  const valid = selected.filter((file) => {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('只能上传图片文件')
      return false
    }
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('单张图片不能超过 10MB')
      return false
    }
    return true
  })
  if (!valid.length) return

  uploading.value = true
  try {
    const paths = await Promise.all(valid.map((file) => uploadBlogImage(file)))
    const urls = paths.map((path) => `/imgs${path}`)
    emit('update:modelValue', [...props.modelValue, ...urls])
    ElMessage.success(`已上传 ${urls.length} 张图片`)
  } catch (error) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    uploading.value = false
  }
}

async function remove(index) {
  const url = props.modelValue[index]
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)

  const name = url.replace(/^\/imgs/, '')
  try {
    await deleteBlogImage(name)
  } catch (error) {
    // 文件可能已经不存在，不影响界面移除
  }
}
</script>

<style scoped>
.uploader {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 12px;
}

.tile {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
  background: var(--cream-100);
  border: 1px solid var(--line);
}

.tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 999px;
  background: rgba(59, 47, 42, 0.7);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1.5px dashed var(--cream-300);
  background: #fffdfa;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease);
}

.add:hover:not(:disabled) {
  border-color: var(--coral);
  color: var(--coral-dark);
  background: var(--coral-soft);
}

.add:disabled {
  cursor: wait;
}

.add .el-icon {
  font-size: 20px;
}
</style>

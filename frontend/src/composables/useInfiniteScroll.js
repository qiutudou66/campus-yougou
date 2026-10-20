import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * IntersectionObserver 无限加载。
 * loader 返回 false 表示没有更多数据。
 */
export function useInfiniteScroll(loader, { distance = 320 } = {}) {
  const sentinel = ref(null)
  const loading = ref(false)
  const finished = ref(false)
  let observer = null

  async function loadMore() {
    if (loading.value || finished.value) return
    loading.value = true
    try {
      const hasMore = await loader()
      if (hasMore === false) finished.value = true
    } finally {
      loading.value = false
      // 内容不足一屏时继续补齐
      window.requestAnimationFrame(() => {
        if (!sentinel.value || finished.value || loading.value) return
        const rect = sentinel.value.getBoundingClientRect()
        if (rect.top < window.innerHeight + distance) loadMore()
      })
    }
  }

  function reset() {
    finished.value = false
  }

  onMounted(() => {
    observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) loadMore()
      },
      { rootMargin: `0px 0px ${distance}px 0px` }
    )
    if (sentinel.value) observer.observe(sentinel.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })

  return { sentinel, loading, finished, loadMore, reset }
}

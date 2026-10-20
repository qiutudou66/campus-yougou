export function formatPrice(cents) {
  const n = Number(cents)
  if (cents === null || cents === undefined || Number.isNaN(n)) return '0'
  const value = (n / 100).toFixed(2).replace(/\.?0+$/, '')
  return value
}

export function discount(payValue, actualValue) {
  if (!actualValue || !payValue) return ''
  return ((Number(payValue) / Number(actualValue)) * 10)
    .toFixed(1)
    .replace(/\.0$/, '')
}

export function splitImages(images) {
  if (!images) return []
  if (Array.isArray(images)) return images.filter(Boolean)
  return String(images)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function formatDistance(distance) {
  const n = Number(distance)
  if (!distance && distance !== 0) return ''
  if (Number.isNaN(n)) return ''
  if (n < 1000) return `${Math.round(n)}m`
  return `${(n / 1000).toFixed(1)}km`
}

function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatTime(value) {
  const date = parseDate(value)
  if (!date) return ''
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 30 * day) return `${Math.floor(diff / day)}天前`

  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatDateTime(value) {
  const date = parseDate(value)
  if (!date) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatCount(value) {
  const n = Number(value) || 0
  if (n >= 10000) return `${(n / 10000).toFixed(1).replace(/\.0$/, '')}w`
  if (n >= 1000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}k`
  return String(n)
}

export function safeImage(url, fallback = '/imgs/icons/default-icon.png') {
  return url || fallback
}

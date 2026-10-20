import request from './request'

export function getHotBlogs(current = 1) {
  return request.get('/blog/hot', { params: { current } })
}

export function getBlog(id, config = {}) {
  return request.get(`/blog/${id}`, config)
}

export function likeBlog(id) {
  return request.put(`/blog/like/${id}`)
}

export function getBlogLikes(id) {
  return request.get(`/blog/likes/${id}`)
}

export function getMyBlogs(current = 1) {
  return request.get('/blog/of/me', { params: { current } })
}

export function getBlogsOfUser({ userId, current = 1 }) {
  return request.get('/blog/of/user', { params: { user_id: userId, current } })
}

export function getFollowFeed({ lastId, offset }) {
  const params = { lastId }
  if (offset) params.offset = offset
  return request.get('/blog/of/follow', { params })
}

export function createBlog({ shopId, title, images, content }) {
  return request.post('/blog', { shopId, title, images, content })
}

import request from './request'

export function uploadBlogImage(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload/blog', form)
}

export function deleteBlogImage(name) {
  return request.get('/upload/blog/delete', { params: { name } })
}

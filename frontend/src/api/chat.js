import request from './request'

export function askChat({ question, history }) {
  return request.post(
    '/chat',
    { question, history },
    { timeout: 60000 }
  )
}

export function askChatImage(file) {
  const form = new FormData()
  form.append('file', file, 'photo.jpg')
  return request.post('/chat/image', form, { timeout: 60000 })
}

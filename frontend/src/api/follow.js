import request from './request'

export function followUser(userId, isFollow) {
  return request.post(`/follow/${userId}/${isFollow}`)
}

export function isFollowing(userId, config = {}) {
  return request.post(`/follow/or/not/${userId}`, null, config)
}

export function getCommonFollows(userId) {
  return request.get(`/follow/common/${userId}`)
}

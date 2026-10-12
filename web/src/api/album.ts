import { request } from '@/utils/request'
import type { Album, AlbumForm } from '@/types'

/** 某个摄影师的客片 */
export const listAlbums = (userId: number, style?: string) =>
  request<Album[]>({ url: '/albums', params: { userId, style } })
/** 我的客片（含不公开的） */
export const myAlbums = () => request<Album[]>({ url: '/albums/mine' })
export const getAlbum = (id: number) => request<Album>({ url: `/albums/${id}` })
export const createAlbum = (data: AlbumForm) => request<Album>({ url: '/albums', method: 'post', data })
export const updateAlbum = (id: number, data: AlbumForm) => request<Album>({ url: `/albums/${id}`, method: 'put', data })
export const deleteAlbum = (id: number) => request<void>({ url: `/albums/${id}`, method: 'delete' })
export const setAlbumCover = (id: number, photoId: number) =>
  request<void>({ url: `/albums/${id}/cover/${photoId}`, method: 'put' })

export const likeAlbum = (id: number) => request<void>({ url: `/me/likes/${id}`, method: 'post' })
export const unlikeAlbum = (id: number) => request<void>({ url: `/me/likes/${id}`, method: 'delete' })
export const myLikes = () => request<Album[]>({ url: '/me/likes' })

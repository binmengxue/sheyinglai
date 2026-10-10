import { request } from '@/utils/request'
import type { Album, AlbumForm } from '@/types'

export const listAlbums = () => request<Album[]>({ url: '/albums' })
export const getAlbum = (id: number) => request<Album>({ url: `/albums/${id}` })
export const createAlbum = (data: AlbumForm) => request<Album>({ url: '/albums', method: 'post', data })
export const updateAlbum = (id: number, data: AlbumForm) => request<Album>({ url: `/albums/${id}`, method: 'put', data })
export const deleteAlbum = (id: number) => request<void>({ url: `/albums/${id}`, method: 'delete' })
export const setAlbumCover = (id: number, photoId: number) =>
  request<void>({ url: `/albums/${id}/cover/${photoId}`, method: 'put' })

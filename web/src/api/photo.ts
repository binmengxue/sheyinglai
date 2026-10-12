import { request } from '@/utils/request'
import type { PageResult, Photo } from '@/types'

export const listPhotos = (params: { albumId: number; page?: number; size?: number }) =>
  request<PageResult<Photo>>({ url: '/photos', params })

export const deletePhoto = (id: number) => request<void>({ url: `/photos/${id}`, method: 'delete' })

/** 上传单张图片，onProgress 回传 0-100 */
export function uploadPhoto(albumId: number, file: File, onProgress?: (p: number) => void) {
  const form = new FormData()
  form.append('albumId', String(albumId))
  form.append('file', file)
  return request<Photo>({
    url: '/photos',
    method: 'post',
    data: form,
    timeout: 5 * 60 * 1000,
    onUploadProgress: (e) => e.total && onProgress?.(Math.round((e.loaded / e.total) * 100)),
  })
}

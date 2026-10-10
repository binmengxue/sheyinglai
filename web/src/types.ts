export interface Result<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface Album {
  id: number
  title: string
  description?: string
  coverUrl?: string
  visible: boolean
  sort: number
  createdAt: string
  photoCount?: number
}

export interface Photo {
  id: number
  albumId: number
  title?: string
  url: string
  thumbUrl: string
  width?: number
  height?: number
  size?: number
  createdAt: string
}

export type AlbumForm = Pick<Album, 'title' | 'description' | 'visible' | 'sort'>

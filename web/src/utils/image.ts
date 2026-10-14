/** 服务器单张图片上限，和后端 multipart 配置保持一致 */
export const MAX_UPLOAD_BYTES = 1024 * 1024

/**
 * 上传前在浏览器里压缩：超过上限（或边长过大）的照片缩到 maxSide 以内、转成 JPG，
 * 逐步降低质量直到小于 maxBytes。手机原图通常 3~8MB，压缩后几百 KB，肉眼看不出差别。
 * GIF 可能是动图，不压缩。
 */
export async function compressImage(file: File, { maxBytes = MAX_UPLOAD_BYTES, maxSide = 2560 } = {}): Promise<File> {
  if (file.type === 'image/gif') {
    if (file.size > maxBytes) throw new Error('GIF 动图不能超过 1M')
    return file
  }
  let bitmap: ImageBitmap
  try {
    // from-image：按照片 EXIF 方向摆正，避免竖拍的照片变横
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
  } catch {
    throw new Error('无法读取这张图片，请换成 JPG 或 PNG')
  }
  const tooBig = Math.max(bitmap.width, bitmap.height) > maxSide
  if (file.size <= maxBytes && !tooBig) {
    bitmap.close()
    return file
  }

  let scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
  let quality = 0.86
  try {
    for (let i = 0; i < 8; i++) {
      const blob = await draw(bitmap, scale, quality)
      if (blob.size <= maxBytes) {
        return new File([blob], file.name.replace(/\.\w+$/, '') + '.jpg', { type: 'image/jpeg' })
      }
      // 先降质量，质量够低了再缩尺寸
      if (quality > 0.65) quality -= 0.1
      else scale *= 0.8
    }
  } finally {
    bitmap.close()
  }
  throw new Error('图片太大，压缩后仍超过 1M')
}

function draw(bitmap: ImageBitmap, scale: number, quality: number): Promise<Blob> {
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  const ctx = canvas.getContext('2d')!
  // 透明 PNG 转 JPG 时铺白底，否则透明处会变黑
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  return new Promise((resolve, reject) =>
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('图片压缩失败'))), 'image/jpeg', quality))
}

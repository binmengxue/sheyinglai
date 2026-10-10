package com.sheyinglai.storage;

/** 一次上传的结果 */
public record StoredFile(String key, String url, String thumbUrl, Integer width, Integer height, long size) {
}

package io.git.zjoker.zcache.core;

import java.io.IOException;

import io.git.zjoker.zcache.CacheUtils;
import io.git.zjoker.zcache.Utils;
import io.git.zjoker.zcache.disklrucache.DiskLruCacheHelper;
import io.git.zjoker.zcache.mapper.IByteConverter;

import static io.git.zjoker.zcache.helper.ICacheHelper.C_Illegal_Duration;

public class DiskCacheCore implements ICacheCore {
    private DiskLruCacheHelper diskLruCacheHelper;

    public DiskCacheCore(int appVersion, String cacheDir, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSpace must > 0");
        }
        try {
            diskLruCacheHelper = new DiskLruCacheHelper(appVersion, cacheDir, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> void put(String key, T obj, IByteConverter<T> mapper) {
        put(key, obj, C_Illegal_Duration, mapper);
    }

    @Override
    public <T> void put(String key, T obj, long duration, IByteConverter<T> converter) {
        byte[] bytes = converter.getBytes(obj);
        if (!CacheUtils.isLegalDuration(duration)) {
            diskLruCacheHelper.put(key, bytes);
        } else {
            diskLruCacheHelper.put(key, CacheUtils.newByteArrayWithDateInfo(duration, bytes));
        }
    }

    @Override
    public <T> T get(String key, IByteConverter<T> converter) {
        byte[] bytes = diskLruCacheHelper.getAsByte(key);
        if (bytes == null) {
            return null;
        }
        if (CacheUtils.isDue(bytes)) {
            evict(key);
            return null;
        }

        return converter.getObject(CacheUtils.clearDateInfo(bytes));
    }

    @Override
    public boolean isExpired(String key) {
        byte[] bytes = diskLruCacheHelper.getAsByte(key);
        return bytes != null && CacheUtils.isDue(bytes);
    }

    @Override
    public void evict(String key) {
        diskLruCacheHelper.remove(key);
    }

    @Override
    public void evictAll() {
        diskLruCacheHelper.clear();
    }

    @Override
    public boolean isCached(String key) {
        return diskLruCacheHelper.contains(key);
    }

    @Override
    public long[] getDurationInfo(String key) {
        byte[] bytes = diskLruCacheHelper.getAsByte(key);
        if (bytes == null) {
            return null;
        }
        String[] info = CacheUtils.getDateInfoFromDate(bytes);
        if (info != null && info.length == 2) {
            return new long[]{Utils.parseLong(info[0]), Utils.parseLong(info[1])};
        }
        return null;
    }
}

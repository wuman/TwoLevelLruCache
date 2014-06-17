package com.wuman.twolevellrucache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link com.wuman.twolevellrucache.TwoLevelLruCache.Converter} of {@link android.graphics
 * .Bitmap} that will serialize and deserialize an Android {@link android.graphics.Bitmap} to
 * disk.  This converter will by default serialize using {@link android.graphics.Bitmap
 * .CompressFormat#PNG} with quality 100.
 */
public class BitmapConverter implements TwoLevelLruCache.Converter<Bitmap> {

    public BitmapConverter() {
    }

    @Override public Bitmap from(byte[] bytes) throws IOException {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override public void toStream(Bitmap bitmap, OutputStream bytes) throws IOException {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
    }
}

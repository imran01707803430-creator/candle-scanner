package com.example.signalscanner;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import java.nio.ByteBuffer;

public final class ImageUtils {
    private ImageUtils() {}
    public static Bitmap imageToBitmap(Image image, int outW, int outH) {
        Image.Plane[] planes = image.getPlanes();
        if (planes.length == 0) return null;
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap temp = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride,
                image.getHeight(), Bitmap.Config.ARGB_8888);
        temp.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(temp, 0, 0, image.getWidth(), image.getHeight());
        temp.recycle();
        if (cropped.getWidth() != outW || cropped.getHeight() != outH)
            return Bitmap.createScaledBitmap(cropped, outW, outH, false);
        return cropped;
    }
}

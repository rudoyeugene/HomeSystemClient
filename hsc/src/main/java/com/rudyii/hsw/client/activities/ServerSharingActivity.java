package com.rudyii.hsw.client.activities;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.rudyii.hsw.client.R;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServerAlias;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServerKey;

public class ServerSharingActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_share_server);

        ImageView qrCodeView = findViewById(R.id.qr_code);

        qrCodeView.setImageBitmap(generateQrCodeForActiveServer());
    }

    private Bitmap generateQrCodeForActiveServer() {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(getActiveServerAlias() + ":" + getActiveServerKey(),
                    BarcodeFormat.QR_CODE, getQrCodeSize(), getQrCodeSize(), null);
        } catch (Exception e) {
            return BitmapFactory.decodeResource(getResources(), R.mipmap.image_warning);
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, getQrCodeSize(), 0, 0, w, h);
        return bitmap;
    }

    private int getQrCodeSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return width;
        } else {
            return height;
        }
    }
}

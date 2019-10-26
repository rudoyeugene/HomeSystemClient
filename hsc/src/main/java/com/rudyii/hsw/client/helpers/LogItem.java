package com.rudyii.hsw.client.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import androidx.annotation.NonNull;

import com.rudyii.hsw.client.R;

import java.util.Objects;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;

/**
 * Created by Jack on 14.01.2018.
 */

public class LogItem extends View implements Comparable<LogItem> {
    private Long timestamp;
    private Bitmap image;
    private String title, description;

    public LogItem(Context context) {
        super(context);
    }

    public LogItem fill(Bitmap image, String title, String description, Long timestamp) {
        if (image == null) {
            this.image = BitmapFactory.decodeResource(getAppContext().getResources(), R.mipmap.image_warning);
        } else {
            this.image = image;
        }

        this.timestamp = timestamp;
        this.title = title;
        this.description = description;
        return this;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void fireAction() {
    }

    @Override
    public int compareTo(@NonNull LogItem logItem) {
        if (this.timestamp > logItem.getTimestamp()) {
            return 1;
        } else if (Objects.equals(this.timestamp, logItem.getTimestamp())) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int hashCode() {
        return (int) ((timestamp >> 32) ^ timestamp);
    }
}
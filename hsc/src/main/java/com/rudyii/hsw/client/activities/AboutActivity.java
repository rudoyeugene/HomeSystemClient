package com.rudyii.hsw.client.activities;

import static com.rudyii.hsw.client.helpers.Utils.currentLocale;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.hsw.client.R;

/**
 * Created by Jack on 29.12.2017.
 */

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        String applicationVersion = "0";
        int applicationBuild = 0;
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);

            applicationVersion = pInfo.versionName;
            applicationBuild = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView aboutTextView = findViewById(R.id.aboutTextView);
        aboutTextView.setText(String.format(currentLocale, getResources().getString(R.string.text_about), applicationVersion, applicationBuild));
    }
}

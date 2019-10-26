package com.rudyii.hsw.client.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.annotation.RequiresApi;

import com.rudyii.hsw.client.R;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.N_MR1;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServerAlias;
import static com.rudyii.hsw.client.helpers.Utils.stringIsEmptyOrNull;
import static java.util.Objects.requireNonNull;

public class ShortcutsBuilder {

    @RequiresApi(api = N_MR1)
    public static void buildDynamicShortcuts() {
        Context context = getAppContext();

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        requireNonNull(shortcutManager).removeAllDynamicShortcuts();
        List<ShortcutInfo> scInfoFromXml = shortcutManager.getDynamicShortcuts();

        ShortcutInfo serverName = new ShortcutInfo.Builder(context, "serverName")
                .setShortLabel(stringIsEmptyOrNull(getActiveServerAlias()) ? context.getResources().getString(R.string.text_no_server) : getActiveServerAlias())
                .setIcon(Icon.createWithResource(context, R.mipmap.shortcut_server))
                .setIntent(new Intent(Intent.ACTION_MAIN, Uri.EMPTY))
                .build();

        List<ShortcutInfo> scAllShortcuts = new ArrayList<>();

        scAllShortcuts.add(serverName);
        scAllShortcuts.addAll(scInfoFromXml);

        shortcutManager.setDynamicShortcuts(scAllShortcuts);
    }
}

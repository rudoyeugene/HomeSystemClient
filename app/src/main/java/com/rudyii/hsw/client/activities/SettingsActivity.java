package com.rudyii.hsw.client.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.services.FirebaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TITLE;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TYPE;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.getSoundNameBy;
import static com.rudyii.hsw.client.helpers.Utils.isMyServiceRunning;
import static com.rudyii.hsw.client.helpers.Utils.isPaired;
import static com.rudyii.hsw.client.helpers.Utils.serverKeyIsValid;
import static com.rudyii.hsw.client.providers.DatabaseProvider.deleteIdFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;

public class SettingsActivity extends AppCompatActivity {
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private final int QR_SCAN_CODE = 84695;
    private final int INFORMATION_NOTIFICATION_SOUND_CODE = 35978;
    private final int MOTION_NOTIFICATION_SOUND_CODE = 35949;
    private Button pairServerButton, infoSoundButton, motionSoundButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Settings Activity created");

        setContentView(R.layout.activity_settings);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);

        Collections.sort(pkgAppsList, new ResolveInfo.DisplayNameComparator(getPackageManager()));

        final ArrayList<ResolveInfoWrapper> infoWrappers = new ArrayList<>();
        for (ResolveInfo resolveInfo : pkgAppsList) {
            infoWrappers.add(new ResolveInfoWrapper(resolveInfo));
        }

        Spinner appsList = (Spinner) findViewById(R.id.appsList);
        final ActivityAdapter arrayAdapter = new ActivityAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, infoWrappers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);

                ((TextView) convertView).setText(getCameraAppName());

                return convertView;
            }


        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appsList.setAdapter(arrayAdapter);
        appsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int selected, long current) {
                if (!init) {
                    init = true;
                    return;
                }

                ResolveInfoWrapper info = (ResolveInfoWrapper) parent.getItemAtPosition(selected);
                String packageName = info.getInfo().activityInfo.packageName;

                saveStringValueToSettings("CAMERA_APP", packageName);

                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        infoSoundButton = (Button) findViewById(R.id.infoSound);
        infoSoundButton.setText(getSoundNameBy(getStringValueFromSettings("INFO_SOUND")));

        infoSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent infoSoundIntent = new Intent(ACTION_RINGTONE_PICKER);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_TYPE, TYPE_NOTIFICATION);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_PICKED_URI, (Uri) null);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_TITLE, "Select Tone");
                infoSoundIntent.putExtra(EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                startActivityForResult(infoSoundIntent, INFORMATION_NOTIFICATION_SOUND_CODE);
            }
        });

        motionSoundButton = (Button) findViewById(R.id.motionSound);
        motionSoundButton.setText(getSoundNameBy(getStringValueFromSettings("MOTION_SOUND")));

        motionSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent infoSoundIntent = new Intent(ACTION_RINGTONE_PICKER);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_TYPE, TYPE_NOTIFICATION);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_PICKED_URI, (Uri) null);
                infoSoundIntent.putExtra(EXTRA_RINGTONE_TITLE, "Select Tone");
                infoSoundIntent.putExtra(EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                startActivityForResult(infoSoundIntent, MOTION_NOTIFICATION_SOUND_CODE);
            }
        });

        pairServerButton = (Button) findViewById(R.id.SCAN_SECRET_QR);
        pairServerButton.setText(isPaired() ? getResources().getString(R.string.server_paired) : getResources().getString(R.string.server_paired));
        pairServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPaired()) {
                    AlertDialog.Builder unpairServerAlert = new AlertDialog.Builder(SettingsActivity.this);
                    unpairServerAlert.setTitle(getResources().getString(R.string.server_unpair_alert_title));
                    unpairServerAlert.setMessage(getResources().getString(R.string.server_unpair_alert_message));

                    unpairServerAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteIdFromSettings("SERVER_KEY");

                            if (isMyServiceRunning(FirebaseService.class)) {
                                stopService(new Intent(getApplicationContext(), FirebaseService.class));
                            }

                            new ToastDrawer().showToast(isPaired() ? getResources().getString(R.string.server_unpaired_failure) : getResources().getString(R.string.server_unpaired_success));
                            pairServerButton.setText(R.string.server_unpaired);
                        }
                    });

                    unpairServerAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    unpairServerAlert.show();

                } else {
                    try {
                        Intent intent = new Intent(ACTION_SCAN);
                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                        startActivityForResult(intent, QR_SCAN_CODE);
                    } catch (ActivityNotFoundException anfe) {
                        showDialogToDownloadQrCodeScanner(SettingsActivity.this);
                    }
                }
            }
        });
    }

    private void showDialogToDownloadQrCodeScanner(final Activity act) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(getResources().getString(R.string.download_qr_scanner_title));
        downloadDialog.setMessage(getResources().getString(R.string.download_qr_scanner_message));
        downloadDialog.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    Log.e(TAG, "Failed to open Play Store.");
                }
            }
        });
        downloadDialog.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        downloadDialog.show();
    }

    private String getCameraAppPackageName() {
        return getStringValueFromSettings("CAMERA_APP");
    }

    private String getCameraAppName() {
        String appName = getResources().getString(R.string.select_camera_app_text);
        try {
            appName = String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCameraAppPackageName(), PackageManager.GET_META_DATA)));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to load package name");
        }

        return appName;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent == null) {
            return;
        }

        String contents, soundName = null;
        Uri soundUri;
        switch (requestCode) {
            case QR_SCAN_CODE:
                contents = intent.getStringExtra("SCAN_RESULT");

                if (serverKeyIsValid(contents)) {
                    saveStringValueToSettings("SERVER_KEY", contents);

                    if (isMyServiceRunning(FirebaseService.class)) {
                        stopService(new Intent(getApplicationContext(), FirebaseService.class));
                        startService(new Intent(getApplicationContext(), FirebaseService.class));
                    } else {
                        startService(new Intent(getApplicationContext(), FirebaseService.class));
                    }
                    new ToastDrawer().showToast(isPaired() ? getResources().getString(R.string.server_paired_success) : getResources().getString(R.string.server_paired_failure));
                    pairServerButton.setText(R.string.server_paired);
                } else {
                    new ToastDrawer().showToast(getResources().getString(R.string.server_paired_failure_detailed));
                }

                break;

            case INFORMATION_NOTIFICATION_SOUND_CODE:
                soundUri = (Uri) intent.getExtras().get("android.intent.extra.ringtone.PICKED_URI");

                if (soundUri == null) {
                    deleteIdFromSettings("INFO_SOUND");
                    soundName = getSoundNameBy(getStringValueFromSettings("INFO_SOUND"));
                    new ToastDrawer().showToast(getResources().getString(R.string.info_sound_removed));
                } else {
                    saveStringValueToSettings("INFO_SOUND", soundUri.toString());
                    soundName = getSoundNameBy(getStringValueFromSettings("INFO_SOUND"));
                    new ToastDrawer().showToast(getResources().getString(R.string.info_sound_changed_to) + " " + soundName);
                }
                infoSoundButton.setText(soundName);
                break;

            case MOTION_NOTIFICATION_SOUND_CODE:
                soundUri = (Uri) intent.getExtras().get("android.intent.extra.ringtone.PICKED_URI");

                if (soundUri == null) {
                    deleteIdFromSettings("MOTION_SOUND");
                    soundName = getSoundNameBy(getStringValueFromSettings("MOTION_SOUND"));
                    new ToastDrawer().showToast(getResources().getString(R.string.motion_sound_removed));
                } else {
                    saveStringValueToSettings("MOTION_SOUND", soundUri.toString());
                    soundName = getSoundNameBy(getStringValueFromSettings("MOTION_SOUND"));
                    new ToastDrawer().showToast(getResources().getString(R.string.motion_sound_changed_to) + " " + soundName);
                }
                motionSoundButton.setText(soundName);
                break;
        }
    }

    private final class ResolveInfoWrapper {
        private ResolveInfo mInfo;

        public ResolveInfoWrapper(ResolveInfo info) {
            mInfo = info;
        }

        @Override
        public String toString() {
            return mInfo.loadLabel(getPackageManager()).toString();
        }

        public ResolveInfo getInfo() {
            return mInfo;
        }
    }

    private class ActivityAdapter extends ArrayAdapter<ResolveInfoWrapper> {
        private LayoutInflater mInflater;

        public ActivityAdapter(Context context, int resourceId, ArrayList<ResolveInfoWrapper> list) {
            super(context, resourceId, list);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ResolveInfoWrapper info = getItem(position);

            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                view.setTag(view.findViewById(android.R.id.text1));
            }

            final TextView textView = (TextView) view.getTag();
            textView.setText(info.getInfo().loadLabel(getPackageManager()));

            return view;
        }
    }
}

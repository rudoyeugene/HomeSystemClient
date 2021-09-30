package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.writeJson;
import static com.rudyii.hsw.client.objects.internal.CameraSettingsInternal.CAMERA_SETTINSG_EXTRA_DATA_NAME;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.CameraSettingsActivity;
import com.rudyii.hsw.client.objects.internal.CameraSettingsInternal;

import java.util.List;

public class CameraListAdapter extends BaseAdapter implements ListAdapter {
    private final List<CameraSettingsInternal> list;
    private final Context context;

    public CameraListAdapter(List<CameraSettingsInternal> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        CameraSettingsInternal cameraSettingsInternal = list.get(position);
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.servers_list, null);
        }

        Button button = view.findViewById(R.id.btn);

        button.setText(cameraSettingsInternal.getCameraName());
        button.setOnClickListener(v -> {
            Intent myIntent = new Intent(getAppContext(), CameraSettingsActivity.class);
            myIntent.putExtra(CAMERA_SETTINSG_EXTRA_DATA_NAME, writeJson(cameraSettingsInternal));
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getAppContext().startActivity(myIntent);
        });

        return view;
    }
}
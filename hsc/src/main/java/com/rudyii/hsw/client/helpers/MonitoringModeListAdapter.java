package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.helpers.Utils.getCameraModeLocalized;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import com.rudyii.hs.common.type.MonitoringModeType;
import com.rudyii.hsw.client.R;

import java.util.List;

public class MonitoringModeListAdapter extends BaseAdapter implements ListAdapter {
    public static final String SELECTED_MODE = "selectedMode";
    private final List<MonitoringModeType> list;
    private final Context context;

    public MonitoringModeListAdapter(List<MonitoringModeType> list, Context context) {
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
        MonitoringModeType monitoringModeType = list.get(position);

        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.servers_list, null);
        }

        Button button = view.findViewById(R.id.btn);
        button.setText(getCameraModeLocalized(monitoringModeType));

        button.setOnClickListener(view1 -> {
            Intent intent = new Intent();
            intent.putExtra(SELECTED_MODE, monitoringModeType.toString());
            ((Activity) context).setResult(Activity.RESULT_OK, intent);
            ((Activity) context).finish();
        });

        return view;
    }
}
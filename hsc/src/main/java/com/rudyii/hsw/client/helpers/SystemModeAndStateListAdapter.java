package com.rudyii.hsw.client.helpers;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.REQUEST_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.REQUEST_SYSTEM_MODE_AND_STATE;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getPrimaryAccountEmail;
import static com.rudyii.hsw.client.helpers.Utils.getSystemModeLocalized;
import static com.rudyii.hsw.client.helpers.Utils.getSystemStateLocalized;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import com.rudyii.hs.common.objects.ServerStatusChangeRequest;
import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.objects.internal.SystemModeAndState;

import java.util.List;

public class SystemModeAndStateListAdapter extends BaseAdapter implements ListAdapter {
    private final List<SystemModeAndState> list;
    private final Context context;

    public SystemModeAndStateListAdapter(List<SystemModeAndState> list, Context context) {
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
        SystemModeAndState systemModeAndState = list.get(position);
        SystemModeType systemMode = systemModeAndState.getSystemMode();
        SystemStateType systemState = systemModeAndState.getSystemState();

        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.servers_list, null);
        }

        Button button = view.findViewById(R.id.btn);

        if (systemMode.equals(SystemModeType.AUTOMATIC)) {
            button.setText(String.format(currentLocale, "%s", getSystemModeLocalized(systemMode)));
        } else {
            button.setText(String.format(currentLocale, "%s", getSystemStateLocalized(systemState)));
        }

        button.setOnClickListener(v -> {
            getActiveServerRootReference().child(REQUEST_ROOT).child(REQUEST_SYSTEM_MODE_AND_STATE).setValue(ServerStatusChangeRequest.builder()
                    .systemMode(systemMode)
                    .systemState(systemState)
                    .by(getPrimaryAccountEmail())
                    .build());
            ((Activity) context).finish();
        });

        return view;
    }
}
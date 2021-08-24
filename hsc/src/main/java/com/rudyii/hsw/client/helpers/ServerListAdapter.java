package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.objects.ServerData;

import java.util.List;

public class ServerListAdapter extends BaseAdapter implements ListAdapter {
    private final List<ServerData> list;
    private final Context context;

    public ServerListAdapter(List<ServerData> list, Context context) {
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
        ServerData serverData = list.get(position);
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.servers_list, null);
        }

        Button serverButton = (Button) view.findViewById(R.id.btn);

        serverButton.setText(serverData.getServerAlias());
        serverButton.setOnClickListener(v -> {
            setOrUpdateActiveServer(serverData);
            ((Activity) context).finish();
        });

        return view;
    }
}
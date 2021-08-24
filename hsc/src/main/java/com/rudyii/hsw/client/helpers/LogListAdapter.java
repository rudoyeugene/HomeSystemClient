package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.activities.SystemLogActivity.HSC_SYSTEM_LOG_ITEM_CLICKED;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rudyii.hsw.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jack on 14.01.2018.
 */

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.ViewHolder> {
    private final List<LogItem> mData;
    private final LayoutInflater mInflater;

    // data is passed into the constructor
    public LogListAdapter(Context context, ArrayList<LogItem> mData) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.log_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LogItem logItem = mData.get(position);
        holder.imageView.setImageDrawable(new BitmapDrawable(getAppContext().getResources(), logItem.getImage()));
        holder.logDescriptionHeader.setText(logItem.getTitle());
        holder.logDescriptionDetails.setText(logItem.getDescription());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView logDescriptionHeader;
        private final TextView logDescriptionDetails;
        private final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            logDescriptionHeader = itemView.findViewById(R.id.logDescriptionHeader);
            logDescriptionHeader.setTextColor(getAppContext().getColor(R.color.textColor));
            logDescriptionDetails = itemView.findViewById(R.id.logDescriptionDetails);
            logDescriptionDetails.setTextColor(getAppContext().getColor(R.color.textColor));
            imageView = itemView.findViewById(R.id.logImage);

            itemView.setOnClickListener(v -> {
                System.out.println(getLayoutPosition());
                System.out.println("clicked");
                System.out.println("firing intent");

                Intent intent = new Intent();
                intent.setAction(HSC_SYSTEM_LOG_ITEM_CLICKED);
                intent.putExtra("itemId", getLayoutPosition());

                getAppContext().sendBroadcast(intent);
            });
        }
    }
}
package com.rudyii.hsw.client.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rudyii.hsw.client.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.activities.SystemLogActivity.HSC_SYSTEM_LOG_ITEM_CLICKED;

/**
 * Created by j-a-c on 14.01.2018.
 */

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.ViewHolder> {
    private List<LogItem> mData = Collections.emptyList();
    private LayoutInflater mInflater;

    // data is passed into the constructor
    public LogListAdapter(Context context, ArrayList<LogItem> mData) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.log_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LogItem logItem = mData.get(position);
        holder.imageView.setImageDrawable(new BitmapDrawable(logItem.getImage()));
        holder.logDescriptionHeader.setText(logItem.getHeader());
        holder.logDescriptionDetails.setText(logItem.getDescription());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView logDescriptionHeader, logDescriptionDetails;
        private ImageView imageView;
        private Intent intent;

        public ViewHolder(View itemView) {
            super(itemView);
            logDescriptionHeader = (TextView) itemView.findViewById(R.id.logDescriptionHeader);
            logDescriptionDetails = (TextView) itemView.findViewById(R.id.logDescriptionDetails);
            imageView = (ImageView) itemView.findViewById(R.id.logImage);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(getLayoutPosition());
                    System.out.println("clicked");
                    System.out.println("firing intent");

                    Intent intent = new Intent();
                    intent.setAction(HSC_SYSTEM_LOG_ITEM_CLICKED);
                    intent.putExtra("itemId", getLayoutPosition());

                    getAppContext().sendBroadcast(intent);
                }
            });
        }
    }
}
package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.USAGE_STATS_ROOT;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;

import java.util.ArrayList;
import java.util.List;

public class UsageChartActivity extends AppCompatActivity {
    private final DatabaseReference usageRef = getActiveServerRootReference().child(USAGE_STATS_ROOT);
    private Cartesian cartesian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Usage chart Activity created");

        setContentView(R.layout.activity_usage_chart);

        AnyChartView anyChartView = findViewById(R.id.usageChart);
        cartesian = AnyChart.vertical();
        cartesian.animation(true, 1000);
        anyChartView.setChart(cartesian);
        usageRef.addValueEventListener(buildInfoValueEventListener());
    }

    private ValueEventListener buildInfoValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<DataEntry> dataEntries = new ArrayList<>();
                    dataSnapshot.getChildren().forEach(stat -> {
                        dataEntries.add(new ValueDataEntry(stat.getKey(), convertToHoursFrom((long) stat.getValue())));
                    });
                    cartesian.data(dataEntries);
                    cartesian.draw(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private double convertToHoursFrom(long minutes) {
        long fullHours = minutes / 60;
        double minutesLeft = minutes - (fullHours * 60);
        double minutesLeftDecimal = minutesLeft / 60;
        return fullHours + minutesLeftDecimal;
    }
}


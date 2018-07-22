package com.rudyii.hsw.client.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

public class UsageChartActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        OnChartGestureListener, OnChartValueSelectedListener {
    private BarChart barChart;
    private TreeMap<String, Object> usageStats;
    private final DatabaseReference usageRef = getRootReference().child("/usageStats");
    private Handler usageHandler;
    private Runnable usageRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Usage chart Activity created");

        setContentView(R.layout.activity_usage_chart);

        barChart = (BarChart) findViewById(R.id.usageChart);
        barChart.setOnChartGestureListener(this);
        barChart.setOnChartValueSelectedListener(this);
        barChart.setDrawGridBackground(false);

        barChart.getDescription().setEnabled(false);

        barChart.setTouchEnabled(true);

        barChart.setDragEnabled(true);
        barChart.setScaleXEnabled(true);
        barChart.setScaleYEnabled(true);
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                String x = barChart.getXAxis().getValueFormatter().getFormattedValue(e.getX(), barChart.getXAxis());
                new ToastDrawer().showToast(x);
            }

            @Override
            public void onNothingSelected() {

            }
        });

        barChart.animateX(1000);
        fillStatsFromFirebase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        usageHandler.removeCallbacks(usageRunnable);
    }

    private void fillStatsFromFirebase() {
        usageHandler = new Handler();
        usageRunnable = new Runnable() {
            @Override
            public void run() {
                usageRef.addListenerForSingleValueEvent(buildInfoValueEventListener());
                usageHandler.postDelayed(this, 60000);
            }
        };

        usageHandler.post(usageRunnable);
    }

    private ValueEventListener buildInfoValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                @SuppressWarnings("unchecked") HashMap<String, Object> tempData = (HashMap<String, Object>) dataSnapshot.getValue();

                if (tempData == null) {
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_text_data_unavailable));
                } else {
                    usageStats = new TreeMap<>(tempData);
                    List<BarEntry> values = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    List<Integer> colors = new ArrayList<>();

                    int i = 0;
                    for (Map.Entry<String, Object> entry : usageStats.entrySet()) {
                        BarEntry barEntry = new BarEntry((float) i++, toFloatHours((Long) entry.getValue()), entry.getValue());

                        long value = (long) entry.getValue();

                        if (value == 1440L) {
                            colors.add(R.color.red);
                        } else if (value > 720 && value < 1440L) {
                            colors.add(R.color.orange);
                        } else if (value > 480 && value < 720) {
                            colors.add(R.color.yellow);
                        } else if (value > 240 && value < 480) {
                            colors.add(R.color.cyan);
                        } else if (value < 240) {
                            colors.add(R.color.green);
                        }

                        values.add(barEntry);
                        labels.add(entry.getKey());
                    }

                    BarDataSet dataSet = new BarDataSet(values, String.format(getResources().getString(R.string.text_bar_data_usage_for),
                            usageStats.size(), usageStats.size() > 1 ? getResources().getString(R.string.text_days) : getResources().getString(R.string.text_day)));
                    dataSet.setStackLabels(labels.toArray(new String[labels.size()]));

                    int[] colorArray = new int[colors.size()];
                    for (int i1 = 0; i1 < colorArray.length; i1++) {
                        colorArray[i1] = colors.get(i1);
                    }
                    dataSet.setColors(colorArray, getApplicationContext());

                    BarData barData = new BarData(dataSet);
                    barData.setDrawValues(true);
                    barData.setValueTextSize(10f);

                    barChart.setDrawValueAboveBar(true);
                    barChart.setData(barData);
                    barChart.setFitBars(true);

                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    barChart.getXAxis().setAvoidFirstLastClipping(true);
                    barChart.getXAxis().setLabelCount(labels.size());
                    barChart.getXAxis().setDrawLabels(false);
                    barChart.getXAxis().setGranularity(1f);

                    barChart.getAxisRight().setEnabled(false);
                    barChart.getAxisLeft().setAxisMaximum(24f);

                    barChart.invalidate();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private Float toFloatHours(Long minutes) {
        BigDecimal minsInHours = BigDecimal.valueOf(60L);
        BigDecimal totalMinutes = BigDecimal.valueOf(minutes);

        BigDecimal[] result = totalMinutes.divideAndRemainder(minsInHours);
        return Float.parseFloat(result[0] + "." + result[1]);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}

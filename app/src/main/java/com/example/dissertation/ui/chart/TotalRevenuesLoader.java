package com.example.dissertation.ui.chart;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.example.dissertation.DatabaseHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TotalRevenuesLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable " +
                "WHERE budgetDate BETWEEN %d AND %d ORDER BY budgetDate ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {

            TreeMap<String, float[]> dateTotalsMap = new TreeMap<>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

            // Aggregate data by date
            while (cursor.moveToNext()) {
                long budgetDate = cursor.getLong(cursor.getColumnIndex("budgetDate"));
                float total = cursor.getFloat(cursor.getColumnIndex("total"));
                String date = dateFormat.format(new Date(budgetDate));

                if (!dateTotalsMap.containsKey(date)) {
                    dateTotalsMap.put(date, new float[]{0, 0});
                }

                if (total >= 0) {
                    dateTotalsMap.get(date)[0] += total;
                } else {
                    dateTotalsMap.get(date)[1] += total;
                }
            }

            List<BarEntry> positiveEntries = new ArrayList<>();
            List<BarEntry> negativeEntries = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            int index = 0;

            // Create entries for the chart
            for (Map.Entry<String, float[]> entry : dateTotalsMap.entrySet()) {
                String date = entry.getKey();
                float[] totals = entry.getValue();

                // Prepare data for the chart and add them
                positiveEntries.add(new BarEntry(index, totals[0]));
                negativeEntries.add(new BarEntry(index, totals[1]));
                dates.add(date);
                index++;
            }

            // Create datasets for both negative and positive sets
            BarDataSet positiveDataSet = new BarDataSet(positiveEntries, "Positive Total Revenues");
            positiveDataSet.setColor(Color.GREEN);
            positiveDataSet.setValueTextColor(Color.GREEN);

            BarDataSet negativeDataSet = new BarDataSet(negativeEntries, "Negative Total Revenues");
            negativeDataSet.setColor(Color.RED);
            negativeDataSet.setValueTextColor(Color.GREEN);


            // Check if entries have been added
            if (dateTotalsMap.isEmpty()) {
                Log.d("BudgetChart", "No data available for the selected date range.");
            } else {
                // Set data on the chart
                BarData barData = new BarData(positiveDataSet, negativeDataSet);
                chart.setData(barData);
            }

            // Set up custom formatter for x-axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Refresh chart
            chart.invalidate();
        } catch (Exception e) {
            Log.e("ChartError", "Error loading chart data: " + e.getMessage());
            Toast.makeText(chart.getContext(), "Error loading chart data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

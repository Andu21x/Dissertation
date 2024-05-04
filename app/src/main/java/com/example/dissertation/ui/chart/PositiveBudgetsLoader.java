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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PositiveBudgetsLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable " +
                "WHERE total > 0 AND budgetDate BETWEEN %d AND %d ORDER BY budgetDate ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {
            Map<String, Float> dateTotalMap = new TreeMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                long budgetDate = cursor.getLong(cursor.getColumnIndex("budgetDate"));
                float total = cursor.getFloat(cursor.getColumnIndex("total"));
                String date = dateFormat.format(new Date(budgetDate));

                dateTotalMap.put(date, dateTotalMap.getOrDefault(date, 0f) + total);
            }

            List<BarEntry> entries = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            int index = 0;

            // Create entries for chart
            for (Map.Entry<String, Float> entry : dateTotalMap.entrySet()) {
                String date = entry.getKey();
                float total = entry.getValue();

                entries.add(new BarEntry(index, total));
                dates.add(date);
                index++;
            }

            // Create dataset
            BarDataSet dataSet = new BarDataSet(entries, "Positive Budgets");
            dataSet.setValueTextColor(Color.GREEN);
            dataSet.setColor(Color.GREEN);

            // Create bar data and set it to chart
            BarData barData = new BarData(dataSet);
            chart.setData(barData);

            // Set up custom formatter for x-axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Refresh chart
            chart.invalidate();
        } catch (Exception e) {
            Log.e("ChartError", "Error loading positive budgets: " + e.getMessage());
            Toast.makeText(chart.getContext(), "Error loading positive budgets: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

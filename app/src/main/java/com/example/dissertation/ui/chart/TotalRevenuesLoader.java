package com.example.dissertation.ui.chart;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.widget.Toast;

import com.example.dissertation.DatabaseHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TotalRevenuesLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable WHERE budgetDate BETWEEN %d AND %d", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {
            List<BarEntry> entries = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            int index = 0;

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                long budgetDate = cursor.getLong(cursor.getColumnIndex("budgetDate"));
                float total = cursor.getFloat(cursor.getColumnIndex("total"));

                // Prepare data for chart
                entries.add(new BarEntry(index, total));
                dates.add(dateFormat.format(new Date(budgetDate)));
                index++;
            }

            // Create dataset
            BarDataSet dataSet = new BarDataSet(entries, "Total Revenues");
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setColors(new ArrayList<>());

            // Set colors based on positive or negative total
            for (BarEntry entry : entries) {
                float total = entry.getY();
                int color = total >= 0 ? Color.GREEN : Color.RED;
                dataSet.addColor(color);
            }

            // Create bar data and set it to chart
            BarData barData = new BarData(dataSet);
            chart.setData(barData);

            // Set up custom formatter for x-axis
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
            chart.getXAxis().setGranularity(1f);

            // Refresh chart
            chart.invalidate();
        } catch (Exception e) {
            Toast.makeText(chart.getContext(), "Error loading chart data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

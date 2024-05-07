// Heavily inspired by https://weeklycoding.com/mpandroidchart-documentation/

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

public class NetProfitOrLossLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        // Query to select budgets between specified dates and order them by date
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable " +
                "WHERE budgetDate BETWEEN %d AND %d ORDER BY budgetDate ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {
            // Use TreeMap to ensure that the dates are sorted in ascending order automatically
            // Each date string will be mapped to an array of floats (totals)
            Map<String, float[]> dateTotalsMap = new TreeMap<>();

            // SimpleDateFormat to format dates from the database into a standard format later
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                // Gather all the data we need from the SQL
                long budgetDate = cursor.getLong(cursor.getColumnIndex("budgetDate"));
                float total = cursor.getFloat(cursor.getColumnIndex("total"));

                // Format budgetDate
                String date = dateFormat.format(new Date(budgetDate));

                // If the date does not already exist in the map, initialize it with zero totals
                if (!dateTotalsMap.containsKey(date)) {
                    dateTotalsMap.put(date, new float[]{0, 0});
                }

                // Accumulate positive and negative totals separately
                if (total >= 0) {
                    dateTotalsMap.get(date)[0] += total;
                } else {
                    dateTotalsMap.get(date)[1] += total;
                }
            }

            // List to hold all revenue entries
            List<BarEntry> entries = new ArrayList<>();

            // List to hold the formatted date labels for the x-axis
            List<String> dates = new ArrayList<>();

            // Index to iterate through for loop
            int index = 0;

            // Create entries for chart, iterate over each entry in the map
            for (Map.Entry<String, float[]> entry : dateTotalsMap.entrySet()) {
                String date = entry.getKey();
                float[] totals = entry.getValue();

                // Sum the totals to get either a positive or a negative
                float netProfitOrLoss = totals[0] + totals[1];

                // Create a new BarEntry for the chart using the current index as the x-value and total as the y-value
                entries.add(new BarEntry(index, netProfitOrLoss));
                dates.add(date);

                // Increment index to move to the next total
                index++;
            }

            // Create dataset and set its colour
            BarDataSet dataSet = new BarDataSet(entries, "Net Profit or Loss");
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setColors(new ArrayList<>());

            // Iterate through every entry and set colors based on profit or loss
            for (BarEntry entry : entries) {
                float net = entry.getY();
                int color = net >= 0 ? Color.GREEN : Color.RED;
                dataSet.addColor(color);
            }

            // Create bar data and set it to chart
            BarData barData = new BarData(dataSet);
            chart.setData(barData);

            // Set up custom formatter for x-axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
            xAxis.setGranularity(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Invalidate the chart to refresh its content
            chart.invalidate();
        } catch (Exception e) {
            Log.e("ChartError", "Error loading net profit or loss data: " + e.getMessage());
            Toast.makeText(chart.getContext(), "Error loading net profit or loss data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

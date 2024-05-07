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

public class TotalRevenuesLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        // Query to select budgets between specified dates and order them by date
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable " +
                "WHERE budgetDate BETWEEN %d AND %d ORDER BY budgetDate ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {

            // A TreeMap to store date keys and float arrays of total values (positive and negative)
            TreeMap<String, float[]> dateTotalsMap = new TreeMap<>();

            // SimpleDateFormat to format dates from the database into a standard format later
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

            // Process each row to accumulate totals by date
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

            // Lists to hold the positive and negative revenue entries
            List<BarEntry> positiveEntries = new ArrayList<>();
            List<BarEntry> negativeEntries = new ArrayList<>();

            // List to hold the formatted date labels for the x-axis
            List<String> dates = new ArrayList<>();

            // Index to iterate through for loop
            int index = 0;

            // Create entries for chart, iterate over each entry in the map
            for (Map.Entry<String, float[]> entry : dateTotalsMap.entrySet()) {
                String date = entry.getKey();
                float[] totals = entry.getValue();

                // Prepare data for the chart and add them
                positiveEntries.add(new BarEntry(index, totals[0]));
                negativeEntries.add(new BarEntry(index, totals[1]));
                dates.add(date);

                // Increment index to move to the next total
                index++;
            }

            // Create datasets for both negative and positive sets, set their colour accordingly
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

            // Invalidate the chart to refresh its content
            chart.invalidate();
        } catch (Exception e) {
            Log.e("ChartError", "Error loading chart data: " + e.getMessage());
            Toast.makeText(chart.getContext(), "Error loading chart data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

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

public class NegativeBudgetsLoader implements ChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper) {
        // Query to select budgets between specified dates and order them by date
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT budgetDate, total FROM budgetTable " +
                "WHERE total < 0 AND budgetDate BETWEEN %d AND %d ORDER BY budgetDate ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.readChart(query)) {

            // Use TreeMap to ensure that the dates are sorted in ascending order automatically
            TreeMap<String, Float> dateTotalsMap = new TreeMap<>();

            // SimpleDateFormat to format dates from the database into a standard format later
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                // Gather all the data we need from the SQL
                long budgetDate = cursor.getLong(cursor.getColumnIndex("budgetDate"));
                float total = cursor.getFloat(cursor.getColumnIndex("total"));

                // Format budgetDate
                String date = dateFormat.format(new Date(budgetDate));

                // Use getOrDefault() to put the total if there is no total assigned to that date
                dateTotalsMap.put(date, dateTotalsMap.getOrDefault(date, 0f) + total);
            }

            // List to hold the bar entries
            List<BarEntry> entries = new ArrayList<>();

            // List to hold the dates as strings
            List<String> dates = new ArrayList<>();

            // Index to iterate through for loop
            int index = 0;

            // Create entries for chart, iterate over each entry in the map
            for (Map.Entry<String, Float> entry : dateTotalsMap.entrySet()) {
                String date = entry.getKey();
                float total = entry.getValue();

                // Create a new BarEntry for the chart using the current index as the x-value and total as the y-value
                entries.add(new BarEntry(index, total));
                dates.add(date);

                // Increment index to move to the next total
                index++;
            }

            // Create dataset and set colour red
            BarDataSet dataSet = new BarDataSet(entries, "Negative Budgets");
            dataSet.setValueTextColor(Color.RED);
            dataSet.setColor(Color.RED); // Negative budgets are red

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
            Log.e("ChartError", "Error loading negative budget data: " + e.getMessage());
            Toast.makeText(chart.getContext(), "Error loading negative budget data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

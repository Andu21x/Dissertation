// Heavily inspired by https://weeklycoding.com/mpandroidchart-documentation/

package com.example.dissertation.ui.weatherchart;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.example.dissertation.DatabaseHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TemperatureLoader implements WeatherChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(LineChart chart, long startDate, long endDate, String cityName, DatabaseHelper dbHelper) {
        // Query to select previous weather data between specified dates and order them by date
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT dateTime, temperature FROM prevWeatherDataTable " +
                "WHERE cityName = ? AND dateTime BETWEEN %d AND %d ORDER BY dateTime ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, new String[]{cityName})) {
            // Create a list for entries
            List<Entry> entries = new ArrayList<>();

            // Create a list for string dates
            List<String> dates = new ArrayList<>();

            // Create a set for long unique dates
            Set<Long> uniqueDates = new HashSet<>();

            // SimpleDateFormat to format dates from the database into a standard format later
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK);

            // Index to iterate through for loop
            int index = 0;

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                // Gather all the data we need from the SQL
                long dateTime = cursor.getLong(cursor.getColumnIndex("dateTime"));
                float temperature = cursor.getFloat(cursor.getColumnIndex("temperature"));

                // Log the data
                Log.d("ChartData", String.format("DateTime: %d, Temperature: %.2f", dateTime, temperature));

                // Check for unique dates to prevent duplicates
                if (uniqueDates.contains(dateTime)) {
                    continue;
                }
                uniqueDates.add(dateTime);

                // Convert Unix timestamp to milliseconds
                Date date = new Date(dateTime * 1000L);

                // Create a new Entry for the chart using the current index as the x-value and temperature as the y-value
                entries.add(new Entry(index, temperature));
                dates.add(dateFormat.format(date));

                // Increment index to move to the next total
                index++;
            }

            // Create dataset and set custom styling
            LineDataSet dataSet = new LineDataSet(entries, "Temperature");
            dataSet.setValueTextColor(Color.RED);
            dataSet.setValueTextSize(10);
            dataSet.setColor(Color.RED);
            dataSet.setCircleColor(Color.RED);
            dataSet.setCircleRadius(4f);
            dataSet.setLineWidth(2f);

            // Check if entries have been added
            if (entries.isEmpty()) {
                Log.d("WeatherChart", "No data available for the selected date range.");
            } else {
                // Set data on the chart
                LineData lineData = new LineData(dataSet);
                chart.setData(lineData);
            }

            // Set up custom formatter for x-axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
            xAxis.setGranularity(2f);
            xAxis.setLabelRotationAngle(-90);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Invalidate the chart to refresh its content
            chart.invalidate();
        } catch (Exception e) {
            Toast.makeText(chart.getContext(), "Error loading weather data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

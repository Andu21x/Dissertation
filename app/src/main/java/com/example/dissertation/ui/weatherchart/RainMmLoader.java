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

public class RainMmLoader implements WeatherChartLoader {

    @SuppressLint("Range")
    @Override
    public void loadChartData(LineChart chart, long startDate, long endDate, String cityName, DatabaseHelper dbHelper) {
        @SuppressLint("DefaultLocale")
        String query = String.format("SELECT dateTime, rain_mm FROM prevWeatherDataTable " +
                "WHERE dateTime BETWEEN %d AND %d AND cityName = ? ORDER BY dateTime ASC", startDate, endDate);

        try (Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, new String[]{cityName})) {
            List<Entry> entries = new ArrayList<>();
            List<String> dates = new ArrayList<>();
            Set<Long> uniqueDates = new HashSet<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK);
            int index = 0;

            // Iterate through cursor and extract relevant data
            while (cursor.moveToNext()) {
                long dateTime = cursor.getLong(cursor.getColumnIndex("dateTime"));
                float rainMm = cursor.getFloat(cursor.getColumnIndex("rain_mm"));

                // Log the data
                Log.d("ChartData", String.format("DateTime: %d, Rain: %.1f mm/3h", dateTime, rainMm));

                // Check for unique dates to prevent duplicates
                if (!uniqueDates.add(dateTime)) {
                    continue;
                }

                // Convert Unix timestamp to milliseconds
                Date date = new Date(dateTime * 1000L);

                // Prepare data for chart
                entries.add(new Entry(index, rainMm));
                dates.add(dateFormat.format(date));
                index++;
            }

            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Rainfall (mm/3h)");
            dataSet.setValueTextColor(Color.BLUE);
            dataSet.setValueTextSize(10);
            dataSet.setColor(Color.BLUE);
            dataSet.setCircleColor(Color.BLUE);
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
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(-90);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Refresh chart
            chart.invalidate();
        } catch (Exception e) {
            Toast.makeText(chart.getContext(), "Error loading weather data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

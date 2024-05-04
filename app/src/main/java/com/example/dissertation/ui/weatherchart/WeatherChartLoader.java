package com.example.dissertation.ui.weatherchart;

import com.example.dissertation.DatabaseHelper;
import com.github.mikephil.charting.charts.LineChart;

public interface WeatherChartLoader {
    void loadChartData(LineChart chart, long startDate, long endDate, String cityName, DatabaseHelper dbHelper);
}

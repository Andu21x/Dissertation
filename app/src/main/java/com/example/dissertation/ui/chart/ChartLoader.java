package com.example.dissertation.ui.chart;

import com.example.dissertation.DatabaseHelper;
import com.github.mikephil.charting.charts.BarChart;

public interface ChartLoader {
    void loadChartData(BarChart chart, long startDate, long endDate, DatabaseHelper dbHelper);
}


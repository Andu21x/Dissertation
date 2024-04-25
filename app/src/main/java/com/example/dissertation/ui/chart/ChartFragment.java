package com.example.dissertation.ui.chart;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class ChartFragment extends Fragment {

    private Spinner chartTypeSpinner;
    private BarChart chart;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);

        chartTypeSpinner = view.findViewById(R.id.chartTypeSpinner);
        chart = view.findViewById(R.id.chart);

        dbHelper = new DatabaseHelper(getActivity());

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.chart_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);
        chartTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChart(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }


    @SuppressLint("Range")
    private void loadChartData(String tableName, String column) {
        try (Cursor cursor = dbHelper.readChart(tableName, column)) {
            List<BarEntry> entries = new ArrayList<>();
            int index = 0;
            while (cursor.moveToNext()) {
                float value = cursor.getFloat(cursor.getColumnIndex(column));
                entries.add(new BarEntry(index++, value));
            }
            BarDataSet dataSet = new BarDataSet(entries, "Data Set");
            BarData barData = new BarData(dataSet);
            chart.setData(barData);
            chart.invalidate(); // Refresh chart
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error loading chart data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateChart(int chartType) {
        switch (chartType) {
            case 0: // Total Revenues
                loadChartData("budgetTable", "total");
                break;
            case 1: // Total Expenses, didn't implement yet, will do next development day
                loadChartData("expenseTable", "total");
                break;
        }
    }
}

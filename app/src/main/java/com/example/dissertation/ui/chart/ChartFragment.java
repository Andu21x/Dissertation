package com.example.dissertation.ui.chart;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;
import com.github.mikephil.charting.charts.BarChart;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChartFragment extends Fragment {

    private BarChart chart;
    private EditText startDatePicker, endDatePicker;
    private DatabaseHelper dbHelper;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);

        Spinner chartTypeSpinner = view.findViewById(R.id.chartTypeSpinner);
        chart = view.findViewById(R.id.chart);
        startDatePicker = view.findViewById(R.id.startDatePicker);
        endDatePicker = view.findViewById(R.id.endDatePicker);

        dbHelper = new DatabaseHelper(getActivity());

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.chart_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);

        startDatePicker.setOnClickListener(v -> showDatePickerDialog(startDatePicker, startCalendar));
        endDatePicker.setOnClickListener(v -> showDatePickerDialog(endDatePicker, endCalendar));

        chartTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Convert integer position to ChartType enum
                ChartType chartType = ChartType.values()[position];
                updateChart(chartType);
            }

            // Method recommended by the IDE to handle no selection
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        return view;
    }

    private void showDatePickerDialog(EditText editText, Calendar calendar) {
        DatePickerDialog.OnDateSetListener date = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(editText, calendar);
        };
        new DatePickerDialog(getContext(), date,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateLabel(EditText editText, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        editText.setText(sdf.format(calendar.getTime()));
    }


    private void updateChart(ChartType chartType) {
        long startDate = startCalendar.getTimeInMillis();
        long endDate = endCalendar.getTimeInMillis();

        ChartLoader loader = null;

        switch (chartType) {
            case TOTAL_REVENUES:
                loader = new TotalRevenuesLoader();
                break;
            case POSITIVE_BUDGETS:
                loader = new PositiveBudgetsLoader();
                break;
            case NEGATIVE_BUDGETS:
                loader = new NegativeBudgetsLoader();
                break;
            case NET_PROFIT_OR_LOSS:
                loader = new NetProfitOrLossLoader();
                break;
        }

        if (loader != null) {
            loader.loadChartData(chart, startDate, endDate, dbHelper);
        }
    }
}

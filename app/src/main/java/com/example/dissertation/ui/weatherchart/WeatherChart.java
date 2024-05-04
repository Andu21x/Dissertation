package com.example.dissertation.ui.weatherchart;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.example.dissertation.DatabaseHelper;
import com.example.dissertation.R;
import com.github.mikephil.charting.charts.LineChart;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeatherChart extends Fragment {

    private Spinner chartTypeSpinner;
    private LineChart chart;
    private AutoCompleteTextView cityNameTextView;
    private EditText startDatePicker, endDatePicker;
    private DatabaseHelper dbHelper;
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weatherchart, container, false);

        chartTypeSpinner = view.findViewById(R.id.weatherChartTypeSpinner);
        chart = view.findViewById(R.id.weatherChart);
        cityNameTextView = view.findViewById(R.id.cityNameTextView);
        startDatePicker = view.findViewById(R.id.startDatePicker);
        endDatePicker = view.findViewById(R.id.endDatePicker);

        dbHelper = new DatabaseHelper(getActivity());

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.weather_chart_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);

        startDatePicker.setOnClickListener(v -> showDatePickerDialog(startDatePicker, startCalendar));
        endDatePicker.setOnClickListener(v -> showDatePickerDialog(endDatePicker, endCalendar));

        chartTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Convert integer position to WeatherChartType enum to find the right chart to display
                WeatherChartType chartType = WeatherChartType.values()[position];
                updateChart(chartType);
            }

            // Method recommended by the IDE to handle no selection
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set up city name AutoCompleteTextView
        List<String> cityNames = Arrays.asList(
                "Tokyo", "Delhi", "Shanghai", "Sao Paulo", "Mexico City", "Cairo",
                "Mumbai", "Beijing", "Dhaka", "Osaka", "New York", "Karachi",
                "Buenos Aires", "Chongqing", "Istanbul", "Kolkata", "Manila",
                "Lagos", "Rio de Janeiro", "Tianjin", "Kinshasa", "Rome",
                "Lisbon", "Athens", "Berlin", "Honolulu", "Vienna", "Bangkok",
                "Madrid", "Paris", "Prague", "Valletta", "Abu Dhabi", "Dublin",
                "London", "Budapest", "Bucharest", "Toronto", "San Juan",
                "Bogota", "Cape Town", "Austin", "Moscow");

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, cityNames);
        cityNameTextView.setAdapter(cityAdapter);
        cityNameTextView.setThreshold(1);

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

    private void updateChart(WeatherChartType chartType) {
        long startDate = startCalendar.getTimeInMillis() / 1000; // Convert to Unix timestamp
        long endDate = endCalendar.getTimeInMillis() / 1000; // Convert to Unix timestamp
        String cityName = cityNameTextView.getText().toString(); // Get the city name

        WeatherChartLoader loader = null;

        switch (chartType) {
            case TEMPERATURE:
                loader = new TemperatureLoader();
                break;
            case HUMIDITY:
                loader = new HumidityLoader();
                break;
            case CLOUDS:
                loader = new CloudsLoader();
                break;
        }

        if (loader != null) {
            loader.loadChartData(chart, startDate, endDate, cityName, dbHelper);
        }
    }
}

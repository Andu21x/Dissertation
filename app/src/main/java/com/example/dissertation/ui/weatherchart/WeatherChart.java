// Heavily inspired by https://developer.android.com/develop/ui/views/layout/declaring-layout?authuser=2#java

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
        // Set up the view and link our .xml file to it
        View view = inflater.inflate(R.layout.fragment_weatherchart, container, false);

        // Initialize views by their specific IDs
        chartTypeSpinner = view.findViewById(R.id.weatherChartTypeSpinner);
        chart = view.findViewById(R.id.weatherChart);
        cityNameTextView = view.findViewById(R.id.cityNameTextView);
        startDatePicker = view.findViewById(R.id.startDatePicker);
        endDatePicker = view.findViewById(R.id.endDatePicker);

        // Initialize the DatabaseHelper to facilitate database operations (CRUD)
        dbHelper = new DatabaseHelper(getActivity());

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.weather_chart_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);

        // Click listeners for the date pickers
        startDatePicker.setOnClickListener(v -> showDatePickerDialog(startDatePicker, startCalendar));
        endDatePicker.setOnClickListener(v -> showDatePickerDialog(endDatePicker, endCalendar));

        // Initialize the spinner and handling
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
                "Tokyo", "Delhi", "Shanghai", "Sao Paulo", "Mexico City", "Cairo", "Mumbai", "Beijing", "Dhaka",
                "Osaka", "New York", "Karachi", "Buenos Aires", "Chongqing", "Istanbul", "Kolkata", "Manila",
                "Lagos", "Rio de Janeiro", "Tianjin", "Kinshasa", "Rome", "Lisbon", "Athens", "Berlin", "Honolulu",
                "Vienna", "Bangkok", "Madrid", "Paris", "Prague", "Valletta", "Abu Dhabi", "Dublin", "London",
                "Budapest", "Bucharest", "Toronto", "San Juan", "Bogota", "Cape Town", "Austin", "Moscow",
                "Barcelona", "Los Angeles", "San Francisco", "Seattle", "Washington DC", "Miami", "Orlando",
                "Chicago", "Montreal", "Vancouver", "Calgary", "Edmonton", "Halifax", "Ottawa", "Havana",
                "Kingston", "Panama City", "Caracas", "Lima", "Santiago", "Belo Horizonte", "Brasilia",
                "Montevideo", "Amsterdam", "Rotterdam", "Munich", "Frankfurt", "Hamburg", "Cologne",
                "Stockholm", "Gothenburg", "Oslo", "Copenhagen", "Helsinki", "Zurich", "Geneva",
                "Luxembourg", "Brussels", "Antwerp", "Warsaw", "Krakow", "Milan", "Naples", "Turin", "Venice",
                "Florence", "Jerusalem", "Tel Aviv", "Beirut", "Kuwait City", "Doha", "Dubai", "Melbourne", "Sydney",
                "Brisbane", "Perth", "Auckland", "Wellington", "Christchurch", "Ankara", "Kyiv", "Lviv", "Glasgow",
                "Edinburgh", "Belfast", "Cardiff", "Manchester", "Birmingham", "Leeds", "Liverpool", "Southampton",
                "Portsmouth", "Newcastle", "Sheffield", "Constanta", "Brasov", "Chengdu", "Porto", "Geneva",
                "Saint Petersburg", "Belgrade", "Sofia", "Marseille", "Seville", "Stuttgart", "Palermo",
                "Leipzig", "Toulouse", "Dortmund", "Lyon", "Bologna", "Palma de Mallorca", "Varna");

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, cityNames);
        cityNameTextView.setAdapter(cityAdapter);
        cityNameTextView.setThreshold(1);

        return view;
    }

    // Build the date picker pop-up for the user.
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

    // Updates the editTextBudgetDate with the date selected from the DatePickerDialog
    // formatted as dd/MM/yyyy. This method is called after the user sets a date.
    private void updateLabel(EditText editText, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        editText.setText(sdf.format(calendar.getTime()));
    }

    // Handle chart switching. Retrieve and send through the selected dates
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
            case RAINMM:
                loader = new RainMmLoader();
                break;
        }

        if (loader != null) {
            loader.loadChartData(chart, startDate, endDate, cityName, dbHelper);
        }
    }
}

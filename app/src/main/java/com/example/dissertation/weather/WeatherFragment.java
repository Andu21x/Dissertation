package com.example.dissertation.weather;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.dissertation.R;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherFragment extends Fragment {

    private TextView weatherDescriptionTextView;

    private OpenWeatherMapService openWeatherMapService;

    private Calendar selectedDateTime = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weather, container, false);

        weatherDescriptionTextView = root.findViewById(R.id.weather_description_text_view);

        AutoCompleteTextView cityTextView = root.findViewById(R.id.autoCompleteCityTextView);

        // List of cities for auto-completion
        List<String> cityNames = Arrays.asList(
                "Tokyo", "Delhi", "Shanghai", "Sao Paulo", "Mexico City", "Cairo", "Mumbai", "Beijing", "Dhaka",
                "Osaka", "New York", "Karachi", "Buenos Aires", "Chongqing", "Istanbul", "Kolkata", "Manila",
                "Lagos", "Rio de Janeiro", "Tianjin", "Kinshasa", "Rome", "Lisbon", "Athens", "Berlin", "Honolulu",
                "Vienna", "Bangkok", "Madrid", "Paris", "Prague", "Valletta", "Abu Dhabi", "Dublin", "London",
                "Budapest", "Bucharest", "Toronto", "San Juan", "Bogota", "Cape Town", "Austin", "Moscow"
        );

        // Create an ArrayAdapter for the AutoCompleteTextView
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, cityNames);

        cityTextView.setAdapter(cityAdapter); // Set the adapter

        cityTextView.setThreshold(1); // Show suggestions when at least 1 character is typed

        // Initialize OpenWeatherMapService
        openWeatherMapService = ApiClient.getInstance().create(OpenWeatherMapService.class);

        Button buttonFetchWeather = root.findViewById(R.id.buttonFetchWeather);
        buttonFetchWeather.setOnClickListener(v -> {
            String city = cityTextView.getText().toString(); // Read the city from AutoCompleteTextView
            if (!city.isEmpty()) {
                loadWeatherData(city);
            } else {
                Toast.makeText(getContext(), "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        // Date picker setup
        Button buttonDatePicker = root.findViewById(R.id.buttonDatePicker);
        buttonDatePicker.setOnClickListener(v -> showDatePickerDialog());

        return root;
    }

    private void showDatePickerDialog() {
        int year = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
            selectedDateTime.set(Calendar.YEAR, selectedYear);
            selectedDateTime.set(Calendar.MONTH, selectedMonth);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDay);

            showTimePickerDialog();

        }, year, month, day).show();
    }

    private void showTimePickerDialog() {
        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        new TimePickerDialog(requireContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
            selectedDateTime.set(Calendar.MINUTE, selectedMinute);

            loadWeatherData(((AutoCompleteTextView) getView().findViewById(R.id.autoCompleteCityTextView)).getText().toString());
        }, hour, minute, true).show();
    }

    private void loadWeatherData(String city) {
        String apiKey = "83eb8c7321549e147b8d74d3c9796331";

        if (openWeatherMapService == null) {
            Log.e("WeatherError", "OpenWeatherMapService is not initialized.");
            showErrorToast();
            return;
        }

        openWeatherMapService.getForecastWeatherData(city, apiKey).enqueue(new Callback<ForecastData>() {
            @Override
            public void onResponse(@NonNull Call<ForecastData> call, @NonNull Response<ForecastData> response) {
                if (response.isSuccessful()) {
                    ForecastData forecastData = response.body();
                    if (forecastData != null) {
                        List<ForecastData.Forecast> forecasts = forecastData.getForecastList();
                        ForecastData.City city = forecastData.getCity();

                        // Convert selected datetime to Unix timestamp
                        long selectedTimestamp = selectedDateTime.getTimeInMillis(); // Get the date in milliseconds

                        ForecastData.Forecast closestForecast = null;
                        long minTimeDiff = Long.MAX_VALUE;

                        for (ForecastData.Forecast forecast : forecasts) {
                            long timeDiff = Math.abs(forecast.getDt() - selectedTimestamp);

                            if (timeDiff < minTimeDiff) {
                                minTimeDiff = timeDiff;
                                closestForecast = forecast;
                            }
                        }

                        if (closestForecast != null) {
                            updateUI(closestForecast, city); // Pass both forecast and city to updateUI
                        } else {
                            showErrorToast();
                        }
                    }
                } else {
                    Log.e("WeatherError", "Response not successful");
                    showErrorToast();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForecastData> call, @NonNull Throwable t) {
                Log.e("WeatherError", "Failure: " + t.getMessage());
                showErrorToast();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(ForecastData.Forecast forecast, ForecastData.City city) {
        if (forecast != null && city != null) {

            String formattedDate = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK).format(new java.util.Date(forecast.getDt() * 1000)); // Convert to milliseconds

            String formattedSunrise = new java.text.SimpleDateFormat("HH:mm:ss", Locale.UK).format(new java.util.Date(city.getSunrise() * 1000)); // Convert to milliseconds

            String formattedSunset = new java.text.SimpleDateFormat("HH:mm:ss", Locale.UK).format(new java.util.Date(city.getSunset() * 1000)); // Convert to milliseconds

            // Constructing weather information
            StringBuilder weatherInfo = new StringBuilder()
                    .append("City: ").append(city.getName()).append(", ").append(city.getCountry())
                    .append("\n" + "Temperature: ").append(String.format(Locale.getDefault(), "%.0f°C", forecast.getMain().temp - 273.15))
                    .append(" (Feels like: ").append(String.format(Locale.getDefault(), "%.0f°C", forecast.getMain().feels_like - 273.15)).append(")")
                    .append("\n" + "Weather: ").append(forecast.getWeather().get(0).description)
                    .append("\n" + "Clouds: ").append(forecast.getClouds().all).append("%")
                    .append("\n" + "Date: ").append(formattedDate)
                    .append("\n" + "\n" + "Humidity: ").append(forecast.getMain().humidity).append("%")
                    .append("\n" + "Wind: ").append(forecast.getWind().speed).append(" m/s at ").append(forecast.getWind().deg).append("°")
                    .append("\n" + "Wind Gust: ").append(forecast.getWind().gust).append(" m/s")
                    .append("\n" + "Pressure: ").append(forecast.getMain().pressure).append(" hPa")
                    .append("\n" + "Visibility: ").append(forecast.getVisibility()).append(" m")
                    .append("\n" + "\n" + "Timezone: UTC").append(city.getTimezone() / 3600) // Converting seconds to hours
                    .append("\n" + "Sunrise: ").append(formattedSunrise)
                    .append("\n" + "Sunset: ").append(formattedSunset)
                    .append("\n" + "Min Temp: ").append(String.format(Locale.getDefault(), "%.0f°C", forecast.getMain().temp_min - 273.15))
                    .append("\n" + "Max Temp: ").append(String.format(Locale.getDefault(), "%.0f°C", forecast.getMain().temp_max - 273.15));

            // Avoid crashes when rain is null, sometimes it can be null
            if (forecast.getRain() != null) {
                weatherInfo.append("\n" + "Rain: ").append(forecast.getRain().h3).append(" mm/3h");
            }

            // Displaying the constructed weather information
            weatherDescriptionTextView.setText(weatherInfo.toString());
        }
    }

    private void showErrorToast() {
        Toast.makeText(getContext(), "Failed to load weather data", Toast.LENGTH_SHORT).show();
    }
}

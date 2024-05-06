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

import com.example.dissertation.DatabaseHelper;
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
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        weatherDescriptionTextView = view.findViewById(R.id.weather_description_text_view);

        AutoCompleteTextView cityTextView = view.findViewById(R.id.autoCompleteCityTextView);

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

        Button buttonFetchWeather = view.findViewById(R.id.buttonFetchWeather);
        buttonFetchWeather.setOnClickListener(v -> {
            String city = cityTextView.getText().toString(); // Read the city from AutoCompleteTextView
            if (!city.isEmpty()) {
                loadWeatherData(city); // Use the 1 parameter version
            } else {
                Toast.makeText(getContext(), "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonFetchWeather5Days = view.findViewById(R.id.buttonFetchWeather5Days);
        buttonFetchWeather5Days.setOnClickListener(v -> {
            String city = cityTextView.getText().toString().trim(); // Ensure there's no leading/trailing whitespace
            if (!city.isEmpty()) {
                fetchWeather5Days(city); // Call the method to fetch weather 5 days in advance
            } else {
                Toast.makeText(getContext(), "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        // Date picker setup
        Button buttonDatePicker = view.findViewById(R.id.buttonDatePicker);
        buttonDatePicker.setOnClickListener(v -> showDatePickerDialog());

        dbHelper = new DatabaseHelper(getContext());

        return view;
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
        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        loadWeatherData(city, currentTime);
    }

    private void loadWeatherData(String city, long timestamp) {
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

                        // Find the forecast closest to the provided timestamp
                        ForecastData.Forecast closestForecast = null;
                        long minTimeDiff = Long.MAX_VALUE;

                        for (ForecastData.Forecast forecast : forecasts) {
                            long timeDiff = Math.abs(forecast.getDt() - timestamp); // Use provided timestamp

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
                    .append("\n" + "Temperature: ").append(String.format(Locale.UK, "%.0f°C", forecast.getMain().temp - 273.15))
                    .append(" (Feels like: ").append(String.format(Locale.UK, "%.0f°C", forecast.getMain().feels_like - 273.15)).append(")")
                    .append("\n" + "Weather: ").append(forecast.getWeather().get(0).description)
                    .append("\n" + "Clouds: ").append(forecast.getClouds().all).append("%")
                    .append("\n" + "Date: ").append(formattedDate)
                    .append("\n" + "\n" + "Humidity: ").append(forecast.getMain().humidity).append("%")
                    .append("\n" + "Probability of Precipitation: ").append(String.format(Locale.UK, "%.0f%%", forecast.getPop() * 100))
                    .append("\n" + "Rain: ").append(forecast.getRain() != null ? forecast.getRain().h3 : 0.0).append(" mm/3h")
                    .append("\n" + "\n" + "Wind: ").append(forecast.getWind().speed).append(" m/s at ").append(forecast.getWind().deg).append("°")
                    .append("\n" + "Wind Gust: ").append(forecast.getWind().gust).append(" m/s")
                    .append("\n" + "Pressure: ").append(forecast.getMain().pressure).append(" hPa")
                    .append("\n" + "Visibility: ").append(forecast.getVisibility()).append(" m")
                    .append("\n" + "\n" + "Timezone: UTC").append(city.getTimezone() / 3600) // Converting seconds to hours
                    .append("\n" + "Sunrise: ").append(formattedSunrise)
                    .append("\n" + "Sunset: ").append(formattedSunset)
                    .append("\n" + "Min Temp: ").append(String.format(Locale.UK, "%.0f°C", forecast.getMain().temp_min - 273.15))
                    .append("\n" + "Max Temp: ").append(String.format(Locale.UK, "%.0f°C", forecast.getMain().temp_max - 273.15));

            // Displaying the constructed weather information
            weatherDescriptionTextView.setText(weatherInfo.toString());

            // Save the weather data to the database
            logAndInsertWeatherData(forecast, city);
        }
    }

    private void showErrorToast() {
        Toast.makeText(getContext(), "Failed to load weather data", Toast.LENGTH_SHORT).show();
    }

    private void fetchWeather5Days(String city) {
        long currentTime = System.currentTimeMillis() / 1000; // Get current time in seconds
        long threeHours = 3 * 3600; // 3 hours in seconds

        // Set the time to the current time, loop through and add three hours to time until time
        // Is less than 5 days from the current time
        for (long time = currentTime; time <= currentTime + 5 * 24 * 3600; time += threeHours) {
            loadWeatherData(city, time); // Use the two-parameter version
        }
    }

    private void logAndInsertWeatherData(ForecastData.Forecast forecast, ForecastData.City city) {
        // Log the data before inserting it
        Log.d("WeatherData", "Inserting weather data into database:");
        Log.d("WeatherData", "City: " + city.getName());
        Log.d("WeatherData", "Country: " + city.getCountry());
        Log.d("WeatherData", "DateTime: " + forecast.getDt());
        Log.d("WeatherData", "Temperature: " + (forecast.getMain().temp - 273.15f));
        Log.d("WeatherData", "Feels Like: " + (forecast.getMain().feels_like - 273.15f));
        Log.d("WeatherData", "Description: " + forecast.getWeather().get(0).description);
        Log.d("WeatherData", "Clouds: " + forecast.getClouds().all);
        Log.d("WeatherData", "Humidity: " + forecast.getMain().humidity);
        Log.d("WeatherData", "Probability of precipitation: " + forecast.getPop() * 100 + "%");
        Log.d("WeatherData", "Rain: " + (forecast.getRain() != null ? forecast.getRain().h3 : 0.0f));
        Log.d("WeatherData", "Wind Speed: " + forecast.getWind().speed);
        Log.d("WeatherData", "Wind Degree: " + forecast.getWind().deg);
        Log.d("WeatherData", "Wind Gust: " + forecast.getWind().gust);
        Log.d("WeatherData", "Pressure: " + forecast.getMain().pressure);
        Log.d("WeatherData", "Visibility: " + forecast.getVisibility());
        Log.d("WeatherData", "Timezone: " + city.getTimezone());
        Log.d("WeatherData", "Sunrise: " + city.getSunrise());
        Log.d("WeatherData", "Sunset: " + city.getSunset());
        Log.d("WeatherData", "Min Temperature: " + (forecast.getMain().temp_min - 273.15f));
        Log.d("WeatherData", "Max Temperature: " + (forecast.getMain().temp_max - 273.15f));

        // Insert the data into the database
        dbHelper.insertPrevWeatherData(
                city.getName(),
                city.getCountry(),
                forecast.getDt(),
                forecast.getMain().temp - 273.15f,
                forecast.getMain().feels_like - 273.15f,
                forecast.getWeather().get(0).description,
                forecast.getClouds().all,
                forecast.getMain().humidity,
                forecast.getPop() * 100,
                forecast.getRain() != null ? forecast.getRain().h3 : 0.0f,
                forecast.getWind().speed,
                forecast.getWind().deg,
                forecast.getWind().gust,
                forecast.getMain().pressure,
                forecast.getVisibility(),
                city.getTimezone(),
                city.getSunrise(),
                city.getSunset(),
                forecast.getMain().temp_min - 273.15f,
                forecast.getMain().temp_max - 273.15f
        );
    }
}

package com.example.dissertation.weather;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.dissertation.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherFragment extends Fragment {

    private TextView cityNameTextView;
    private TextView temperatureTextView;
    private TextView weatherDescriptionTextView;

    private OpenWeatherMapService openWeatherMapService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weather, container, false);
        cityNameTextView = root.findViewById(R.id.city_name_text_view);
        temperatureTextView = root.findViewById(R.id.temperature_text_view);
        weatherDescriptionTextView = root.findViewById(R.id.weather_description_text_view);

        openWeatherMapService = ApiClient.getInstance().create(OpenWeatherMapService.class);
        loadWeatherData();

        return root;
    }

    private void loadWeatherData() {
        String apiKey = "83eb8c7321549e147b8d74d3c9796331";
        openWeatherMapService.getCurrentWeatherData("London", apiKey).enqueue(new Callback<WeatherData>() {
            @Override
            public void onResponse(@NonNull Call<WeatherData> call, @NonNull Response<WeatherData> response) {
                if (response.isSuccessful()) {
                    WeatherData weatherData = response.body();
                    Log.d("WeatherData", "Data: " + weatherData);
                    updateUI(weatherData);
                } else {
                    Log.e("WeatherError", "Response not successful");
                    showErrorToast();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherData> call, @NonNull Throwable t) {
                Log.e("WeatherError", "Failure: " + t.getMessage());
                showErrorToast();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(WeatherData weatherData) {
        if (weatherData != null) {
            cityNameTextView.setText(weatherData.getCityName());
            temperatureTextView.setText(weatherData.getTemperature() + "°C");
            weatherDescriptionTextView.setText(weatherData.getWeatherDescription());
        }
    }

    private void showErrorToast() {
        Toast.makeText(getContext(), "Failed to load weather data", Toast.LENGTH_SHORT).show();
    }
}

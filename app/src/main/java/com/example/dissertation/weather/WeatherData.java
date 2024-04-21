package com.example.dissertation.weather;

import java.util.List;
import java.util.Locale;

public class WeatherData {
    private List<Weather> weather;
    private Main main;
    private String name;

    public WeatherData() {
    }

    public String getCityName() {
        return name;
    }

    public String getTemperature() {
        return main != null ? String.format(Locale.getDefault(), "%.0f", main.temp - 273.15) : null; // Convert Kelvin to Celsius
    }

    public String getWeatherDescription() {
        return weather != null && !weather.isEmpty() ? weather.get(0).description : null;
    }

    // Inner classes to match the JSON structure
    class Weather {
        public String description;

    }

    class Main {
        public double temp;
    }
}

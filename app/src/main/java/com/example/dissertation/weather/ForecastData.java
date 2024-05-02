package com.example.dissertation.weather;

import java.util.List;

public class ForecastData {
    private List<Forecast> list;
    private City city;

    public List<Forecast> getForecastList() {
        return list;
    }

    public City getCity() {
        return city;
    }

    public static class Forecast {
        private Main main;
        private List<Weather> weather;
        private long dt;
        private Wind wind;
        private Rain rain;
        private Clouds clouds;

        private int visibility;

        public Main getMain() {
            return main;
        }

        public List<Weather> getWeather() {
            return weather;
        }

        public long getDt() {
            return dt;
        }

        public Wind getWind() {
            return wind;
        }

        public Rain getRain() {
            return rain;
        }

        public Clouds getClouds() {
            return clouds;
        }

        public int getVisibility() {
            return visibility;
        }

        public static class Main {
            public double temp;
            public double feels_like;
            public int pressure;
            public int humidity;
            public double temp_min;
            public double temp_max;
        }

        public static class Weather {
            public String description;
        }

        public static class Wind {
            public double speed;
            public int deg;
            public double gust;
        }

        public static class Rain {
            public double h3;
        }

        public static class Clouds {
            public int all;
        }
    }

    public static class City {
        private String name;
        private String country;
        private int timezone;
        private long sunrise;
        private long sunset;

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public int getTimezone() {
            return timezone;
        }

        public long getSunrise() {
            return sunrise;
        }

        public long getSunset() {
            return sunset;
        }
    }

}

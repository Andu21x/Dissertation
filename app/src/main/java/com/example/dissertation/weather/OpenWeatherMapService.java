package com.example.dissertation.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherMapService {
    @GET("weather")
    Call<WeatherData> getCurrentWeatherData(@Query("q") String location, @Query("appid") String apiKey);

}
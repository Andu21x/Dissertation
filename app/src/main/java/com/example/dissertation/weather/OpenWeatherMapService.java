package com.example.dissertation.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherMapService {

    @GET("forecast")
    Call<ForecastData> getForecastWeatherData(@Query("q") String location, @Query("appid") String apiKey);

}
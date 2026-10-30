package com.gathin.tvhome;

import android.os.Parcel;
import android.os.Parcelable;

public class WeatherModel implements Parcelable {
    public final float temperature;
    public final int humidity;
    public final float windSpeed;
    public final int weatherCode;
    public final String locationName;

    public WeatherModel(float temperature, int humidity, float windSpeed, int weatherCode, String locationName) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.locationName = locationName;
    }

    protected WeatherModel(Parcel in) {
        temperature = in.readFloat();
        humidity = in.readInt();
        windSpeed = in.readFloat();
        weatherCode = in.readInt();
        locationName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(temperature);
        dest.writeInt(humidity);
        dest.writeFloat(windSpeed);
        dest.writeInt(weatherCode);
        dest.writeString(locationName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WeatherModel> CREATOR = new Creator<WeatherModel>() {
        @Override
        public WeatherModel createFromParcel(Parcel in) {
            return new WeatherModel(in);
        }

        @Override
        public WeatherModel[] newArray(int size) {
            return new WeatherModel[size];
        }
    };
}

package com.DebjoyBuiltIt.placemark;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MarkerModel extends RealmObject {

    @PrimaryKey
    private long id;

    private String title;
    private String emoji;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private String color;
    private double latitude;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    private double longitude;

    public MarkerModel(){
    }

}

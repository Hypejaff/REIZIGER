package com.example.jaff.reiziger.models;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

public class PlaceInfo implements Serializable {
    private String id;
    private String name;
    private String address;
    private LatLng latlng;
    private String uri;
    private String dateDeparture;
    private String dateArrived;

    public PlaceInfo() {}
    public PlaceInfo(String name, String address, String id, LatLng latlng,  String dateArrived, String dateDeparture, String uri) {
        this.name = name;
        this.address = address;
        this.id = id;
        this.latlng = latlng;
        this.dateArrived = dateArrived;
        this.dateDeparture = dateDeparture;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public LatLng getLatlng() {
        return latlng;
    }
    public void setLatlng(LatLng latlng) {
        this.latlng = latlng;
    }

    public String getDateArrived() {
        return dateArrived;
    }
    public void setDateArrived(String dateArrived) {
        this.dateArrived = dateArrived;
    }

    public String getDateDeparture() {
        return dateDeparture;
    }
    public void setDateDeparture(String dateDeparture) {
        this.dateDeparture = dateDeparture;
    }

    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }

    @NonNull
    @Override
    public String toString() {
        return "PlaceInfo{" + "name='" + name
                + '\'' +", address='" + address
                + '\'' +", id='" + id
                + '\'' +", latlng='" + latlng
                + '\'' +", dateArrived='" + dateArrived
                + '\'' +", dateDeparture='" + dateDeparture
                + '\'' +", uri=" + uri
                + '}';
    }
}

package com.romanvytv.targetalarm2;

public class Target {

    private int radius = 50;
    private double latitude;
    private double longtitude;
    private boolean running = false;


    public Target(int radius, double longtitude, double latitude) {
        this.radius = radius;
        this.longtitude = longtitude;
        this.latitude = latitude;
    }

    public Target() {
    }


    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    public boolean isEnabled() {
        return running;
    }

    public void setEnabled(boolean running) {
        this.running = running;
    }


}


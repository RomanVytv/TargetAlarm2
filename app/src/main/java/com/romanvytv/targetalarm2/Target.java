package com.romanvytv.targetalarm2;

public class Target {

    private int radius = 0;
    private double latitude = 0;
    private double longtitude = 0;




    //public static ArrayList<Target> targets = new ArrayList<Target>();//Gson().fromJson(AddActivity.getJsonStrTargets(),
    //new TypeToken<ArrayList<Target>>(){}.getType()  );

    //public static Target target = new Target();

    public Target( int radius, double longtitude, double latitude) {
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

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private boolean running = false;

}


package com.pisco.deydempro3;

public class Trip {

    public String pickup;
    public String dropoff;
    public String price;
    public String gain;
    public String status;
    public String commission;
    public  String date;

    public Trip(String pickup, String dropoff, String price, String gain, String status, String commission, String date){
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.price = price;
        this.gain = gain;
        this.status = status;
        this.commission = commission;
        this.date = date;
    }
}

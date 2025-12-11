package com.pisco.deydempro3;

public class Delivery {
    public String id;
    public String pickup_address;
    public String dropoff_address;
    public String price;

    public Delivery(String id, String pickup, String drop, String price) {
        this.id = id;
        this.pickup_address = pickup;
        this.dropoff_address = drop;
        this.price = price;
    }
}





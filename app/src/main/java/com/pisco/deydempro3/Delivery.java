package com.pisco.deydempro3;

public class Delivery {

    public String id;
    public String pickup_address;
    public String dropoff_address;
    public String pickup_lat;
    public String pickup_lng;
    public String dropoff_lat;
    public String dropoff_lng;
    public String price;

    public Delivery(String id,
                    String pickup_address,
                    String pickup_lat,
                    String pickup_lng,
                    String dropoff_address,
                    String dropoff_lat,
                    String dropoff_lng,
                    String price) {

        this.id = id;
        this.pickup_address = pickup_address;
        this.pickup_lat = pickup_lat;
        this.pickup_lng = pickup_lng;

        this.dropoff_address = dropoff_address;
        this.dropoff_lat = dropoff_lat;
        this.dropoff_lng = dropoff_lng;

        this.price = price;
    }

    public Delivery(String id, String pickupAddress, String dropoffAddress, String price) {
        this.id = id;
        this.pickup_address = pickupAddress;
        this.dropoff_address = dropoffAddress;
        this.price = price;
    }
}

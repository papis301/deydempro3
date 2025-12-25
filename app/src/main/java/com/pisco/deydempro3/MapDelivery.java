package com.pisco.deydempro3;

public class MapDelivery {
    public String id;
    public String pickup;
    public double pickupLat;
    public double pickupLng;
    public String dropoff;
    public double dropLat;
    public double dropLng;
    public String price;
    public String client_id;

    public MapDelivery(String id, String pickup, double pLat, double pLng,
                       String dropoff, double dLat, double dLng, String price, String client_id) {

        this.id = id;
        this.pickup = pickup;
        this.pickupLat = pLat;
        this.pickupLng = pLng;
        this.dropoff = dropoff;
        this.dropLat = dLat;
        this.dropLng = dLng;
        this.price = price;
        this.client_id = client_id;
    }
}


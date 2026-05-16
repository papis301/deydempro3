package deydemv3;

public class TripModel {

    private String id;

    private String clientName;

    private String pickup;

    private String dropoff;

    private String vehicle;

    private String price;

    private String status;

    private String date;
    private String cancelledBy;

    //
    // 🔥 EMPTY CONSTRUCTOR
    //
    public TripModel() {
    }

    //
    // 🔥 ID
    //
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    //
    // 🔥 CLIENT NAME
    //
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    //
    // 🔥 PICKUP
    //
    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    //
    // 🔥 DROPOFF
    //
    public String getDropoff() {
        return dropoff;
    }

    public void setDropoff(String dropoff) {
        this.dropoff = dropoff;
    }

    //
    // 🔥 VEHICLE
    //
    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    //
    // 🔥 PRICE
    //
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    //
    // 🔥 STATUS
    //
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    //
    // 🔥 DATE
    //
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
}

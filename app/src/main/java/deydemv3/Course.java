package deydemv3;

public class Course {
    private int id;
    private int client_id;
    private int driver_id;
    private String pickup_address;
    private String dropoff_address;
    private double distance_km;
    private String vehicle_type;
    private int price;
    private String status;
    private String created_at;

    public Course(int id, String pickup_address, String dropoff_address, String status, int price, String created_at) {
        this.id = id;
        this.pickup_address = pickup_address;
        this.dropoff_address = dropoff_address;
        this.status = status;
        this.price = price;
        this.created_at = created_at;
    }


    public int getId() { return id; }
    public String getCreated_at() { return created_at; }
    public String getStatus() { return status; }
    public String getPickup_address() { return pickup_address; }
    public String getDropoff_address() { return dropoff_address; }
    public int getPrice() { return price; }
}


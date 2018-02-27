package us.cognice.secrets.data;

/**
 * Created by Kirill Simonov on 11.10.2017.
 */
public class Location {

    private String id;
    private String name;
    private double latitude, longitude;
    private int radius;
    private String message;
    private boolean active;

    public Location(String id, String name, double latitude, double longitude, int radius, String message, boolean active) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.message = message;
        this.active = active;
    }

    public Location(String id) {
        this(id, "", 0, 0, 5, "", true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

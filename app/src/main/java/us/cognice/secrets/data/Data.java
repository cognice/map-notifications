package us.cognice.secrets.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kirill Simonov on 11.10.2017.
 */
public class Data {

    private String nickname;
    private String phone;
    private String avaPath;
    private boolean locationServiceOn = true;
    private boolean locationServiceRunning = false;
    private List<Location> places = new ArrayList<>();

    public Data() {
    }

    public Data(String nickname, String phone, String avaPath, boolean locationServiceOn, boolean locationServiceRunning, List<Location> places) {
        this.nickname = nickname;
        this.phone = phone;
        this.avaPath = avaPath;
        this.locationServiceOn = locationServiceOn;
        this.locationServiceRunning = locationServiceRunning;
        if (places != null) this.places = places;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<Location> getPlaces() {
        return places;
    }

    public void setPlaces(List<Location> places) {
        if (places != null) this.places = places;
    }

    public String getAvaPath() {
        return avaPath;
    }

    public void setAvaPath(String avaPath) {
        this.avaPath = avaPath;
    }

    public boolean isLocationServiceOn() {
        return locationServiceOn;
    }

    public void setLocationServiceOn(boolean locationServiceOn) {
        this.locationServiceOn = locationServiceOn;
    }

    public boolean isLocationServiceRunning() {
        return locationServiceRunning;
    }

    public void setLocationServiceRunning(boolean locationServiceRunning) {
        this.locationServiceRunning = locationServiceRunning;
    }
}

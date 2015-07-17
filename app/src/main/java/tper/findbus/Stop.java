package tper.findbus;

import java.util.ArrayList;
import java.util.HashSet;

public class Stop
{
    public int Code, Zone, Distance;
    public String Denomination, Location, Municipality;
    public float Latitude, Longitude;
    public ArrayList<Bus> Incoming;
    public HashSet<String> Favorite;

    public Stop()
    {
        Favorite = new HashSet<>();
        Incoming = new ArrayList<>();
        Bus bus1 = new Bus("", "");
        Bus bus2 = new Bus("", "");
        Incoming.add(bus1);
        Incoming.add(bus2);
    }
    public Stop(int code,
                int zone,
                String denomination,
                String location,
                String municipality,
                float latitude,
                float longitude)
    {
        this();
        Code = code;
        Zone = zone;
        Denomination = denomination;
        Location = location;
        Municipality = municipality;
        Latitude = latitude;
        Longitude = longitude;
    }
}

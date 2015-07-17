package tper.findbus;

public class StopLine
{
    StopLine(Stop stop, String line)
    {
        Bus1 = Bus2 = Time1 = Time2 = "";
        Distance = 0;
        StopName = line + ": " + stop.Denomination;
        StopCode = stop.Code;
        Latitude = stop.Latitude;
        Longitude = stop.Longitude;
        Line = line;
    }

    public String Line, StopName;
    public int StopCode, Distance;
    public float Latitude, Longitude;
    public String Bus1, Bus2, Time1, Time2;
}

package tper.findbus;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class StopsList extends ExpandableListActivity implements LocationListener
{
    private TperDataSource _dataSource = null;
    private LocationManager _locationManager;
    private Location _phoneLocation;
    private ArrayList<Stop> _stops;
    private String _line;
    private StopsListAdapter _adapter = null;
    private Stop _selectedStop;
    private HashMap<Integer, Stop> _favorites;
    private Utility _utility = new Utility();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stops_activity);

        Drawable divider = this.getResources().getDrawable(R.drawable.line);
        getExpandableListView().setGroupIndicator(null);
        getExpandableListView().setDivider(divider);
        getExpandableListView().setChildDivider(divider);
        getExpandableListView().setDividerHeight(5);
        registerForContextMenu(getExpandableListView());

        (findViewById(R.id.buttonBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        // Open the connection with the database.
        _dataSource = new TperDataSource(this);
        _dataSource.open();

        // Get the requested line.
        Intent i = getIntent();
        _line = i.getStringExtra("line");

        // Get the actual phone location.
        _locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 5, this);
        _phoneLocation = getPhoneLocation();

        // Get the favorite stop-line pairs.
        _favorites = _dataSource.getFavoritePairsHashMap();

        // Populate list with the stops ordered by distance.
        final ArrayList<Stop> stops = getStops(_line);
        populateList(stops);
    }

    @Override
    public void onLocationChanged(Location location) {
        _phoneLocation = location;
        updateDistance();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private Location getPhoneLocation()
    {
        Location locationGps = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long timeGps = 0, timeNet = 0;
        if (locationGps != null) timeGps = locationGps.getTime();
        if (locationNet != null) timeNet = locationNet.getTime();

        return (timeGps - timeNet > 0)? locationGps : locationNet;
    }

    private void populateList(final ArrayList<Stop> stops)
    {
        _stops = stops;

        if (_adapter == null)
        {
            _adapter = new StopsListAdapter();
            this.setListAdapter(_adapter);
        }
        else
        {
            ((StopsListAdapter)getExpandableListAdapter()).notifyDataSetChanged();
        }
    }

    private class StopsListAdapter extends BaseExpandableListAdapter
    {
        private LayoutInflater inflater;

        public StopsListAdapter()
        {
            inflater = LayoutInflater.from(StopsList.this);
        }

        @Override
        public View getGroupView(int groupPosition,
                                 boolean isExpanded,
                                 View convertView,
                                 ViewGroup parentView)
        {
            final Stop stop = _stops.get(groupPosition);
            convertView = inflater.inflate(R.layout.bus_item, parentView, false);

            ((TextView) convertView.findViewById(R.id.textName)).setText(stop.Denomination);

            if (stop.Distance < 0)
                ((TextView) convertView.findViewById(R.id.textDistance)).setText("-");
            else
                ((TextView) convertView.findViewById(R.id.textDistance)).setText(stop.Distance + "m");

            ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), BusMap.class);
                    intent.putExtra("denomination", stop.Denomination);
                    intent.putExtra("latitude", stop.Latitude);
                    intent.putExtra("longitude", stop.Longitude);
                    startActivity(intent);
                }
            });

            CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
            boolean favorite =
                    _favorites.containsKey(stop.Code) &&
                    _favorites.get(stop.Code).Favorite.contains(_line);
            checkbox.setChecked(favorite);
            checkbox.setOnCheckedChangeListener(new FavoriteStopListener(stop));

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parentView)
        {
            final Stop stop = _stops.get(groupPosition);
            final Bus bus = stop.Incoming.get(childPosition);

            convertView = inflater.inflate(R.layout.bus_sub_item, parentView, false);
            if (bus.Code != null)
                ((TextView) convertView.findViewById(R.id.textBus)).setText(bus.Code);
            if (bus.Time != null)
                ((TextView) convertView.findViewById(R.id.textTime)).setText(bus.Time);

            return convertView;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition)
        {
            return _stops.get(groupPosition).Incoming.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition)
        {
            return childPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition)
        {
            _selectedStop = _stops.get(groupPosition);

            // Get the actual time.
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
            String now = dateFormat.format(calendar.getTime());

            // Get the incoming bus.
            if (_selectedStop.Incoming.get(0).Code.contentEquals(""))
                new DownloadTimes().execute("" + _selectedStop.Code, _line, now);

            return 2;
        }

        @Override
        public Object getGroup(int groupPosition)
        {
            return _stops.get(groupPosition);
        }

        @Override
        public int getGroupCount()
        {
            return _stops.size();
        }

        @Override
        public long getGroupId(int groupPosition)
        {
            return groupPosition;
        }

        @Override
        public void notifyDataSetChanged()
        {
            super.notifyDataSetChanged();
        }

        @Override
        public boolean isEmpty()
        {
            return ((_stops == null) || _stops.isEmpty());
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return true;
        }

        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        private final class FavoriteStopListener implements OnCheckedChangeListener
        {
            private final Stop stop;

            private FavoriteStopListener(Stop stop)
            {
                this.stop = stop;
            }

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    if (!_favorites.containsKey(stop.Code))
                    {
                        _favorites.put(stop.Code, stop);
                    }
                    _favorites.get(stop.Code).Favorite.add(_line);
                }
                else
                {
                    Stop favorite = _favorites.get(stop.Code);
                    favorite.Favorite.remove(_line);
                    if (favorite.Favorite.size() == 0)
                    {
                        _favorites.remove(stop.Code);
                    }
                }
                _dataSource.setFavoriteStop(stop.Code, _line, isChecked);
            }
        }
    }

    private class DownloadTimes extends AsyncTask<String, Integer, Void>
    {
        private final String URL_TIMES = "https://solweb.tper.it/tperit/webservices/hellobus.asmx/QueryHellobus";
        private String _rawString;
        private ArrayList<Bus> _bus;

        @Override
        protected Void doInBackground(String... params)
        {
            InputStream stream;
            _bus = new ArrayList<>();

            String url = Uri.parse(URL_TIMES)
                    .buildUpon()
                    .appendQueryParameter("fermata", params[0])
                    .appendQueryParameter("linea", params[1])
                    .appendQueryParameter("oraHHMM", params[2])
                    .build().toString();
            try
            {
                stream = _utility.downloadUrl(url);
                try
                {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = docBuilder.parse(stream);
                    NodeList nodes = doc.getElementsByTagName("string");
                    Element element = (Element)nodes.item(0);
                    _rawString = element.getTextContent();

                    // Parse the raw string to extract the two times
                    Pattern pattern = Pattern.compile("(\\S+) Previsto ([0-2][0-9]:[0-5][0-9])");
                    Matcher matcher = pattern.matcher(_rawString);
                    while (matcher.find())
                    {
                        Bus bus = new Bus(matcher.group(1), matcher.group(2));
                        _bus.add(bus);
                    }
                }
                catch (ParserConfigurationException | SAXException e)
                {
                    _utility.alertParsingError(getApplicationContext());
                }
            }
            catch (IOException e)
            {
                _utility.alertServerError(getApplicationContext());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            switch (_bus.size())
            {
                case 2:
                    _selectedStop.Incoming.get(1).Code = _bus.get(1).Code;
                    _selectedStop.Incoming.get(1).Time = _bus.get(1).Time;
                case 1:
                    _selectedStop.Incoming.get(0).Code = _bus.get(0).Code;
                    _selectedStop.Incoming.get(0).Time = _bus.get(0).Time;
                    break;
                case 0:
                    _selectedStop.Incoming.get(0).Code = "No bus";
            }
            _adapter.notifyDataSetChanged();
        }
    }

    private void updateDistance()
    {
        // For each stop compute the distance in meters from the phone location.
        for (Stop stop : _stops)
        {
            Location stopLocation = new Location("");
            stopLocation.setLatitude(stop.Latitude);
            stopLocation.setLongitude(stop.Longitude);
            stop.Distance = (int) _phoneLocation.distanceTo(stopLocation);
        }

        // Order the stops by distance from the phone location.
        Collections.sort(_stops, new Comparator<Stop>()
        {
            public int compare(Stop stop1, Stop stop2)
            {
                return stop1.Distance - stop2.Distance;
            }
        });

        ((StopsListAdapter)getExpandableListAdapter()).notifyDataSetChanged();
    }

    private ArrayList<Stop> getStops(String line)
    {
        ArrayList<Stop> stops;

        // Get all the bus stops touched by the bus line.
        stops = _dataSource.getLineStops(line);

        // For each stop compute the distance in meters from the phone location.
        for (Stop stop : stops)
        {
            Location stopLocation = new Location("");
            stopLocation.setLatitude(stop.Latitude);
            stopLocation.setLongitude(stop.Longitude);
            if (_phoneLocation != null)
                stop.Distance = (int) _phoneLocation.distanceTo(stopLocation);
            else
                stop.Distance = -1;
        }

        // Order the stops by distance from the phone location.
        Collections.sort(stops, new Comparator<Stop>() {
            public int compare(Stop stop1, Stop stop2) {
                return stop1.Distance - stop2.Distance;
            }
        });

        return stops;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_dataSource != null)
            _dataSource.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_dataSource != null)
            _dataSource.close();
    }

    @Override
    protected void onDestroy()
    {
        if (_dataSource != null)
            _dataSource.close();
        super.onDestroy();
    }
}
package tper.findbus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Favorites extends ListActivity
{
    private Location _phoneLocation;
    private Utility _utility = new Utility();
    private TperDataSource _dataSource = null;
    private static ArrayList<StopLine> _favorites = null;
    private static StopLineAdapterItem _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_activity);

        _phoneLocation = getPhoneLocation();

        retrieveStops(getApplicationContext());

        _adapter = new StopLineAdapterItem(this, R.layout.favorites_item, _favorites);
        setListAdapter(_adapter);

        final Button update = (Button) findViewById(R.id.buttonUpdateAll);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                update.setEnabled(false);
                _phoneLocation = getPhoneLocation();
                new DownloadTimes().execute();
            }
        });
    }

    @Override
    protected void onResume()
    {
        if (_dataSource != null)
            _dataSource.open();

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        if (_dataSource != null)
            _dataSource.close();
        super.onPause();
    }

    private void retrieveStops(Context context)
    {
        // Open the connection with the database.
        _dataSource = new TperDataSource(context);
        _dataSource.open();

        // Get the favorite stop-line couples.
        _favorites = _dataSource.getFavoritePairs();

        // Order them alphabetically by bus line.
        Collections.sort(_favorites, new Comparator<StopLine>() {
            public int compare(StopLine favorite1, StopLine favorite2) {
                return favorite1.StopName.compareTo(favorite2.StopName);
            }
        });
    }

    public class StopLineAdapterItem extends ArrayAdapter<StopLine>
    {
        private Context context;
        private int layoutId;
        private ArrayList<StopLine> data;

        public StopLineAdapterItem(Context context, int layoutId, ArrayList<StopLine> data)
        {
            super(context, layoutId, data);
            this.layoutId = layoutId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent)
        {
            if (view == null)
            {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                view = inflater.inflate(layoutId, parent, false);
            }

            final StopLine favorite = data.get(position);

            Location stopLocation = new Location("");
            stopLocation.setLatitude(favorite.Latitude);
            stopLocation.setLongitude(favorite.Longitude);
            favorite.Distance = (int)_phoneLocation.distanceTo(stopLocation);

            ((TextView) view.findViewById(R.id.widgetName)).setText(favorite.StopName);
            ((TextView) view.findViewById(R.id.widgetDistance)).setText(favorite.Distance + "m");
            ((TextView) view.findViewById(R.id.widgetBus1)).setText(favorite.Bus1);
            ((TextView) view.findViewById(R.id.widgetTime1)).setText(favorite.Time1);
            ((TextView) view.findViewById(R.id.widgetBus2)).setText(favorite.Bus2);
            ((TextView) view.findViewById(R.id.widgetTime2)).setText(favorite.Time2);

            (view.findViewById(R.id.favoriteMap)).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Intent intent = new Intent(getApplicationContext(), BusMap.class);
                    intent.putExtra("denomination", favorite.StopName);
                    intent.putExtra("latitude", favorite.Latitude);
                    intent.putExtra("longitude", favorite.Longitude);
                    startActivity(intent);
                }
            });

            final ImageView update = (ImageView) view.findViewById(R.id.favoriteUpdate);
            update.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    update.setEnabled(false);
                    new DownloadTime(position).execute();
                }
            });

            final ImageView remove = (ImageView) view.findViewById(R.id.favoriteRemove);
            remove.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    alertReset(position);
                }
            });

            return view;
        }

        @Override
        public int getCount()
        {
            return data != null ? data.size() : 0;
        }
    }

    private void alertReset(int position)
    {
        final StopLine stopLine = _favorites.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_delete_favorite);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
                _dataSource.deleteFavorite(stopLine.StopCode, stopLine.Line);
                _favorites.remove(stopLine);
                _adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public Location getPhoneLocation()
    {
        LocationManager _locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = _locationManager.getBestProvider(criteria, false);
        return _locationManager.getLastKnownLocation(provider);
    }

    private class DownloadTimes extends AsyncTask<Void, Void, Void>
    {
        private final String URL_TIMES = "https://solweb.tper.it/tperit/webservices/hellobus.asmx/QueryHellobus";
        private String rawString;

        public DownloadTimes()
        {
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            InputStream stream;

            // Get the actual time.
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
            String now = dateFormat.format(calendar.getTime());

            for (StopLine favorite : _favorites)
            {
                String url = Uri.parse(URL_TIMES).buildUpon()
                        .appendQueryParameter("fermata", "" + favorite.StopCode)
                        .appendQueryParameter("linea", favorite.Line)
                        .appendQueryParameter("oraHHMM", now)
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
                        rawString = element.getTextContent();

                        // Parse the raw string to extract the two times
                        Pattern pattern = Pattern.compile("(\\S+) Previsto ([0-2][0-9]:[0-5][0-9])");
                        Matcher matcher = pattern.matcher(rawString);
                        if (matcher.find())
                        {
                            favorite.Bus1 = matcher.group(1);
                            favorite.Time1 = matcher.group(2);
                        }
                        if (matcher.find())
                        {
                            favorite.Bus2 = matcher.group(1);
                            favorite.Time2 = matcher.group(2);
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
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void buses)
        {
            super.onPostExecute(buses);
            _adapter.notifyDataSetChanged();
            (findViewById(R.id.buttonUpdateAll)).setEnabled(true);
        }
    }

    private class DownloadTime extends AsyncTask<Void, Void, Void>
    {
        private final String URL_TIMES = "https://solweb.tper.it/tperit/webservices/hellobus.asmx/QueryHellobus";
        private String rawString;
        private int position;

        public DownloadTime(int position)
        {
            this.position = position;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            InputStream stream;

            // Get the actual time.
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
            String now = dateFormat.format(calendar.getTime());

            StopLine favorite = _favorites.get(position);

            String url = Uri.parse(URL_TIMES).buildUpon()
                    .appendQueryParameter("fermata", "" + favorite.StopCode)
                    .appendQueryParameter("linea", favorite.Line)
                    .appendQueryParameter("oraHHMM", now)
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
                    rawString = element.getTextContent();

                    // Parse the raw string to extract the two times
                    Pattern pattern = Pattern.compile("(\\S+) Previsto ([0-2][0-9]:[0-5][0-9])");
                    Matcher matcher = pattern.matcher(rawString);
                    if (matcher.find())
                    {
                        favorite.Bus1 = matcher.group(1);
                        favorite.Time1 = matcher.group(2);
                    }
                    if (matcher.find())
                    {
                        favorite.Bus2 = matcher.group(1);
                        favorite.Time2 = matcher.group(2);
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
        protected void onPostExecute(Void buses)
        {
            super.onPostExecute(buses);
            _adapter.notifyDataSetChanged();
            (findViewById(R.id.favoriteUpdate)).setEnabled(true);
        }
    }
}

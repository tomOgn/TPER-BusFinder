package tper.findbus;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BusMap extends ActionBarActivity
{
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.map_activity);

        // Get the coordinates.
        Bundle extras = getIntent().getExtras();
        String denomination = extras.getString("denomination");
        float latitude = extras.getFloat("latitude");
        float longitude = extras.getFloat("longitude");
        LatLng stop = new LatLng(latitude, longitude);

        // Generate the _map.
        GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if (map == null)
        {
            Toast.makeText(getApplicationContext(), "Sorry! unable to create Map.", Toast.LENGTH_SHORT).show();
            return;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        // Display the bus stop location.
        Marker stopMarker = map.addMarker(new MarkerOptions()
                .position(stop)
                .title(denomination)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopMarker.getPosition(), 18));
	}
}
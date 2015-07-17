package tper.findbus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends ActionBarActivity
{
    private TperDataSource _dataSource = null;
    HashMap<String, Integer> _lines;

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

        Button update = (Button) findViewById(R.id.buttonUpdate);
        update.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                alertUpdate();
            }
        });

        Button reset = (Button) findViewById(R.id.buttonReset);
        reset.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                alertReset();
            }
        });

        Button favorites = (Button) findViewById(R.id.buttonFavorites);
        favorites.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!isNetworkAvailable(getApplicationContext())) return;
                startActivity(new Intent(getApplicationContext(), Favorites.class));
            }
        });

        _dataSource = new TperDataSource(this);
        _dataSource.open();
        _lines = _dataSource.getLinesUsage();
        populateListViewLines();
        populateGridViewLines();
    }

    private void alertReset()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete all user data?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                _dataSource.reset();
                populateGridViewLines();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void alertUpdate()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to update all bus data? The process may take few minutes to complete.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
                if (!isNetworkAvailable(getApplicationContext())) return;
                startActivity(new Intent(getApplicationContext(), Update.class));
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

    private void populateListViewLines()
    {
        List<String> lines = _dataSource.getLines();
        final ListView listview = (ListView) findViewById(R.id.listViewLines);
        final ArrayAdapter adapter = new ArrayAdapter(this, R.layout.main_list_item, lines);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
            {
                startBusActivity(view);
            }
        });
    }

    private void populateGridViewLines()
    {
        ArrayList<String> favoriteLines = _dataSource.getFavoriteLines();
        GridView gridView = (GridView) findViewById(R.id.gridViewLines);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.main_grid_item, favoriteLines);

        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                startBusActivity(view);
            }
        });
    }

    private void startBusActivity(View view)
    {
        if (!isNetworkAvailable(getApplicationContext())) return;

        String line = String.valueOf(((TextView) view).getText());
        int usage = _lines.get(line) + 1;
        _dataSource.incrementLineUsage(line, usage);
        _lines.put(line, usage);
        populateGridViewLines();
        Intent i = new Intent(getApplicationContext(), StopsList.class);
        i.putExtra("line", line);
        startActivity(i);
    }

    public boolean isNetworkAvailable(final Context context)
    {
        final ConnectivityManager connectivityManager =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        boolean isConnected =
                connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected();
        if (!isConnected)
            Toast.makeText(getApplicationContext(), "The device must be connected to Internet.", Toast.LENGTH_SHORT).show();
        return isConnected;
    }
}
package tper.findbus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity
{
    private TperDataSource _dataSource = null;
    private HashMap<String, Integer> _lines;
    private static Language _language = Language.English;

    public enum Language
    {
        English, Italian
    }

	@Override
	protected void onCreate(final Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

        (findViewById(R.id.buttonUpdate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertUpdate();
            }
        });

        (findViewById(R.id.buttonReset)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertReset();
            }
        });

        (findViewById(R.id.buttonFavorites)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNetworkAvailable(getApplicationContext())) return;
                startActivity(new Intent(getApplicationContext(), Favorites.class));
            }
        });

        RadioButton english = (RadioButton) findViewById(R.id.radioButtonEn);
        english.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (_language != Language.English)
                {
                    _language = Language.English;
                    callSwitchLang("en");
                }
            }
        });

        RadioButton italian = (RadioButton) findViewById(R.id.radioButtonIt);
        italian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_language != Language.Italian) {
                    _language = Language.Italian;
                    callSwitchLang("it");
                }
            }
        });

        String currentLanguage = Locale.getDefault().getLanguage();
        if (currentLanguage.contentEquals("en"))
        {
            _language = Language.English;
            english.setChecked(true);
        }
        else
        {
            _language = Language.Italian;
            italian.setChecked(true);
        }

        _dataSource = new TperDataSource(this);
        _dataSource.open();
        _lines = _dataSource.getLinesUsage();
        populateListViewLines();
        populateGridViewLines();
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

    private void callSwitchLang(String langCode)
    {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(
                config,
                getBaseContext().getResources().getDisplayMetrics());

        if (_dataSource != null)
            _dataSource.close();

        this.recreate();
    }

    private void alertReset()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_delete_all);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                _dataSource.reset();
                populateGridViewLines();
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

    private void alertUpdate()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_update_database);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface arg0, int arg1)
            {
                if (!isNetworkAvailable(getApplicationContext())) return;
                startActivity(new Intent(getApplicationContext(), Update.class));
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
            Toast.makeText(getApplicationContext(), R.string.message_no_internet, Toast.LENGTH_SHORT).show();
        return isConnected;
    }
}
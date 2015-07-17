package tper.findbus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Update extends ActionBarActivity
{
    private TperDataSource _dataSource = null;
    private boolean _lines = false, _stops = false, _paths = false;
    private HashMap<String, Integer> _usage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_activity);

        (findViewById(R.id.buttonDone)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (_dataSource != null)
                    _dataSource.close();
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });

        updateData();
    }

    private void updateData()
    {
        _dataSource = new TperDataSource(this);
        _dataSource.open();
        _usage = _dataSource.getLinesUsage();
        _dataSource.erase();
        new DownloadLines().execute();
        new DownloadStops().execute();
        new DownloadPaths().execute();
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

    private void updateButton()
    {
        if (_lines && _stops && _paths)
        {
            Button update = (Button)findViewById(R.id.buttonDone);
            update.setEnabled(true);
            update.setText("Done!");
        }
    }

    private InputStream downloadUrl(String urlString) throws IOException
    {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(20000);
        conn.setConnectTimeout(30000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }

    private class DownloadLines extends AsyncTask<Void, Integer, Void>
    {
        private final String URL_LINES = "https://solweb.tper.it/tperit/webservices/opendata.asmx/OpenDataLinee";
        private ProgressBar _progressBar = (ProgressBar) findViewById(R.id.progressBarLines);
        private TextView _textView = (TextView) findViewById(R.id.textViewLines);
        private int _length;

        @Override
        protected void onPreExecute()
        {
            _textView.setText("Downloading...");
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            InputStream stream;
            try
            {
                stream = downloadUrl(URL_LINES);
                try
                {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = docBuilder.parse(stream);
                    NodeList nodes = doc.getElementsByTagName("Table");
                    _length = nodes.getLength();
                    _progressBar.setMax(_length);
                    String line;
                    int usage;
                    for (int i = 0; i < _length; i++)
                    {
                        Element element0 = (Element)nodes.item(i);
                        Element element1 = (Element)element0.getElementsByTagName("codice_linea").item(0);
                        line = element1.getTextContent();
                        usage = _usage.containsKey(line)? _usage.get(line) : 0;
                        _dataSource.insertLine(line, usage);
                        publishProgress(i + 1);
                    }
                }
                catch (ParserConfigurationException | SAXException ignored)
                {

                }
                finally
                {
                    if (stream != null)
                        stream.close();
                }
            }
            catch (IOException ignored)
            {

            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
            _progressBar.setProgress(values[0]);
            _textView.setText("" + values[0] + "/" + _length);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            _lines = true;
            updateButton();
        }
    }

    private class DownloadStops extends AsyncTask<Void, Integer, Void>
    {
        public final String URL_STOPS = "https://solweb.tper.it/tperit/webservices/opendata.asmx/OpenDataFermate";
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarStops);
        TextView textView = (TextView) findViewById(R.id.textViewStops);
        int length;

        @Override
        protected void onPreExecute()
        {
            textView.setText("Downloading...");
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            InputStream stream;
            try
            {
                stream = downloadUrl(URL_STOPS);
                try
                {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = docBuilder.parse(stream);
                    NodeList nodes = doc.getElementsByTagName("Table");
                    length = nodes.getLength();
                    progressBar.setMax(length);

                    for (int i = 0; i < length; i++)
                    {
                        Element element = (Element)nodes.item(i);

                        int code = Integer.parseInt(element.getElementsByTagName("codice").item(0).getTextContent());
                        int zone = Integer.parseInt(element.getElementsByTagName("codice_zona").item(0).getTextContent());
                        String denomination = element.getElementsByTagName("denominazione").item(0).getTextContent();
                        String location = element.getElementsByTagName("ubicazione").item(0).getTextContent();
                        String municipality = element.getElementsByTagName("comune").item(0).getTextContent();
                        float latitude = Float.parseFloat(element.getElementsByTagName("latitudine").item(0).getTextContent());
                        float longitude = Float.parseFloat(element.getElementsByTagName("longitudine").item(0).getTextContent());

                        Stop stop = new Stop(code, zone, denomination, location, municipality, latitude, longitude);
                        _dataSource.insertStop(stop);

                        publishProgress(i + 1);
                    }
                }
                catch (ParserConfigurationException | SAXException ignored) {}
                finally
                {
                    if (stream != null)
                        stream.close();
                }
            }
            catch (IOException ignored) {}
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            textView.setText("" + values[0] + "/" + length);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            _stops = true;
            updateButton();
        }
    }

    private class DownloadPaths extends AsyncTask<Void, Integer, Void>
    {
        public final String URL_PATHS = "https://solweb.tper.it/tperit/webservices/opendata.asmx/OpenDataLineeFermate";
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarPaths);
        TextView textView = (TextView) findViewById(R.id.textViewPaths);
        int length;

        @Override
        protected void onPreExecute()
        {
            textView.setText("Downloading...");
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            InputStream stream;
            try
            {
                stream = downloadUrl(URL_PATHS);
                try
                {
                    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = docBuilder.parse(stream);
                    NodeList nodes = doc.getElementsByTagName("Table");
                    length = nodes.getLength();
                    progressBar.setMax(length);
                    for (int i = 0; i < length; i++)
                    {
                        Element element = (Element)nodes.item(i);
                        String line = element.getElementsByTagName("codice_linea").item(0).getTextContent();
                        int stop = Integer.parseInt(element.getElementsByTagName("codice_fermata").item(0).getTextContent());
                        _dataSource.insertPath(line, stop);
                        publishProgress(i + 1);
                    }
                }
                catch (ParserConfigurationException | SAXException ignored) {}
                finally
                {
                    if (stream != null)
                        stream.close();
                }
            }
            catch (IOException ignored) {}
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            textView.setText("" + values[0] + "/" + length);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            _paths = true;
            updateButton();
        }
    }
}

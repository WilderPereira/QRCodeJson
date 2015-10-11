package com.wilder.QRCodeJson;

import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {

    String local = "" ;
    TextView tv;
    String locationFromQR="";
    QRCodeReaderView mydecoderview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        tv = (TextView) findViewById(R.id.tv);
        setSupportActionBar(toolbar);

        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);
        mydecoderview.getCameraManager().startPreview();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv.setText(local);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        locationFromQR = text;
        tv.setText("Decodificado! Parseando Json...");
        new FetchWeatherTask().execute(text);

    }

    @Override
    public void cameraNotFound() {

    }

    @Override
    public void QRCodeNotFoundOnCamImage() {
        Snackbar.make(mydecoderview, "QR Code não encontrado", Snackbar.LENGTH_SHORT);

    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        @Override
        protected String[] doInBackground(String... fromQR) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            try {

                URL url = null;
                try {
                    url = new URL("http://www.previsaodotempo.org/api.php?city="+fromQR[0]);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                Log.v(LOG_TAG, "Weather URL: " + url);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                try {
                    urlConnection.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                String[] res = getWeatherDataFromJson(forecastJsonStr, 7);
                local=res[0];
                return res;

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return null;
        }


        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(String result) {
          //  local = result;
          //  tv.setText(result);
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {
            String[] resultStrs = new String[1];
            // These are the names of the JSON objects that need to be extracted.

            final String OWM_DATA = "data";
            final String OWM_LOCATION = "location";
            final String OWM_TEMPERATURE = "temperature";



            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONObject weatherObj = forecastJson.getJSONObject(OWM_DATA);
            String location = weatherObj.getString(OWM_LOCATION);
            String temperature = weatherObj.getString(OWM_TEMPERATURE);

            Log.d(LOG_TAG, location + " "+ temperature);


            resultStrs[0]="Cidade: "+location+"\n Temperatura: "+temperature;
            local = resultStrs[0];
            return resultStrs;
        }
    }
}

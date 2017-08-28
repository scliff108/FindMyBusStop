package com.tailoreddevelopmentgroup.findmybusstop;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;


/*
 * Finds the closest Bus Stop, to a specific address depending on
 * which school a student goes to.
 * Then finds the bus number that the student gets on at the
 * beginning and end of the day.
 * If the student goes to Brockton High then they take a different
 * bus in the afternoon than in the morning, so if finds the
 * afternoon bus as well.
 *
 * Uses the Google Maps API and ParseDB/Server
 */

public class MainActivity extends AppCompatActivity {

    Button mSearchButton;
    EditText mAddressEditText;
    TextView mMorningTextView;
    TextView mAfternoonTextView;
    Spinner mSchoolNameSpinner;
    String mSchoolNameString;
    ParseObject mSchoolParseObject;
    ParseGeoPoint mStudentGeoPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
    }

    private void buttonClicked () {

        // Reset Text Views
        mMorningTextView.setText("");
        mAfternoonTextView.setText("");

        if (mAddressEditText.getText().toString().length() == 0) {

            Toast.makeText(this, "Enter your address and then search.", Toast.LENGTH_LONG).show();

        } else {
            // Get the address the user has entered
            String address = mAddressEditText.getText().toString();

            // Format the address for use with the Google API
            String formattedAddress = address.replaceAll("\\s+", "+");

            // Create the URL for the API call
            String tempUrl = getResources().getString(R.string.maps_url_beginning) + formattedAddress + getResources().getString(R.string.maps_url_end);

            // Make the API call
            GetGoogleMapsData mapsAPITask = new GetGoogleMapsData();

            // The string returned by the Google API
            String mapsResult;

            // The result array of latitude and longitude of the address
            Double[] latLngArray;

            try {

                mapsResult = mapsAPITask.execute(tempUrl).get();

                latLngArray = getLatAndLng(mapsResult);

                checkDistanceToSchool(latLngArray);

            } catch (Exception e) {

                e.printStackTrace();

            }
        }
    }

    private void checkDistanceToSchool (Double[] resultArray) {

        // GeoPoint of Address Entered
        mStudentGeoPoint = new ParseGeoPoint(resultArray[0], resultArray[1]);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("schoolName", mSchoolNameString);

        ParseCloud.callFunctionInBackground("getSchool", params, new FunctionCallback<ParseObject>() {
            @Override
            public void done(ParseObject school, ParseException e) {

                if (e != null) {

                    e.printStackTrace();

                } else {

                    mSchoolParseObject = school;

                    Double distanceToSchool = mStudentGeoPoint.distanceInMilesTo(mSchoolParseObject.getParseGeoPoint("location"));

                    if (distanceToSchool < mSchoolParseObject.getDouble("walkerRadius")) {

                        mMorningTextView.setText(getResources().getString(R.string.too_close_to_school));

                    } else {

                        searchParseDBForBusStop();
                    }
                }
            }
        });
    }

    private void searchParseDBForBusStop () {

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("schoolName", mSchoolNameString);
        params.put("studentLocation", mStudentGeoPoint);

        ParseCloud.callFunctionInBackground("getMorningStop", params, new FunctionCallback<String>() {
            @Override
            public void done(String morning, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                } else {
                    mMorningTextView.setText(morning);
                }
            }
        });

        if (mSchoolParseObject.getObjectId().equals(getResources().getString(R.string.bhs_object_id))) {

            ParseCloud.callFunctionInBackground("getAfternoonStop", params, new FunctionCallback<String>() {
                @Override
                public void done(String afternoon, ParseException e) {
                    if (e != null) {
                        e.printStackTrace();
                    } else {
                        mAfternoonTextView.setText(afternoon);
                    }
                }
            });

        }
    }

    public class GetGoogleMapsData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";

            URL url;

            HttpURLConnection urlConnection;

            try {

                url = new URL(urls[0]);

                Log.i("contents", url.toString());

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();

                }

                return result;

            }
            catch (Exception e) {

                e.printStackTrace();

                return "Failed";
            }
        }
    }

    @Nullable
    private Double[] getLatAndLng (String input) {

        Double latitude;

        Double longitude;

        try {

            JSONObject inputJSON = new JSONObject(input);

            JSONArray resultsArray = inputJSON.getJSONArray("results");

            JSONObject resultObject = new JSONObject(resultsArray.get(0).toString());

            JSONObject geometryArray = resultObject.getJSONObject("geometry");

            JSONObject locationObject = geometryArray.getJSONObject("location");

            latitude = locationObject.getDouble("lat");

            longitude = locationObject.getDouble("lng");

            return new Double[]{latitude, longitude};

        }
        catch (Exception e) {

            Toast.makeText(this, "Sorry, we could not find your address.", Toast.LENGTH_LONG).show();

            e.printStackTrace();

            return null;

        }

    }

    private void initializeViews () {

        mSearchButton = (Button) findViewById(R.id.searchButton);
        mAddressEditText = (EditText) findViewById(R.id.addressEditText);

        // Handles hitting "enter" key when typing in EditText
        TextView.OnEditorActionListener listener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    buttonClicked();
                }
                return true;
            }
        };

        // Apply the listener to EditText
        mAddressEditText.setOnEditorActionListener(listener);

        mMorningTextView = (TextView) findViewById(R.id.morningBusStopTextView);

        mAfternoonTextView = (TextView) findViewById(R.id.afternoonBusStopTextView);

        initializeSpinner();

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked();
            }
        });
    }

    private void initializeSpinner () {

        mSchoolNameSpinner = (Spinner) findViewById(R.id.school_spinner);
        mSchoolNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                mSchoolNameString = parent.getItemAtPosition(position).toString();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                Log.i("ItemSelected", "No item selected");

            }
        });

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.school_list, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSchoolNameSpinner.setAdapter(adapter);

    }
}


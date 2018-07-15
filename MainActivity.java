package com.example.quickstart;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;


import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,Runnable {

    GoogleAccountCredential mCredential;
    private TextView mOutputText,text2;
    private Button mCallApiButton;
    ProgressDialog mProgress;

    private static final String TAG= "MainActivity";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT +"\' button to test the API.");
        activityLayout.addView(mOutputText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }



    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public void run() {

    }


    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */

//
//        private List<String> getDataFromApi() throws IOException {
//            // List the next 10 events from the primary calendar.
//
//            DateTime now = new DateTime(System.currentTimeMillis());
//            DateTime after = new DateTime(System.currentTimeMillis()+24 * 60 * 60 * 1000);
//
//            List<String> eventStrings = new ArrayList<String>();
//            List<String> tests = new ArrayList<String>();
//            String start2="";
//            String end2="";
//
//
//
//            Events events = mService.events().list("primary")
//                    .setMaxResults(10)
//                    .setTimeMin(now)
//                    .setTimeMax(after)
//                    .setOrderBy("startTime")
//                    .setSingleEvents(true)
//                    .execute();
//
//
//            List<Event> items = events.getItems();
//            SimpleDateFormat sdf = new SimpleDateFormat( "HH:mm:ss" );
//            for (Event event : items) {
//                DateTime start = event.getStart().getDateTime();
//                DateTime end = event.getEnd().getDateTime();
//
//
//                if (start == null) {
//                    // All-day events don't have start times, so just use
//                    // the start date.
////                    start = event.getStart().getDate();
////                    end   = event.getEnd().getDate();
//
//                    start = event.getStart().getDate();
//                    end   = event.getEnd().getDateTime();
////
////                    start2 = sdf.format(start);
////                    end2 = sdf.format(end);
//                }
//                eventStrings.add(
//                        String.format("%s (%s) - (%s)", event.getSummary(), start, end));
//            }
//
//            return eventStrings;
//        }


        private List<String> getDataFromApi() throws IOException {
//             List the next 10 events from the primary calendar.

//            DateTime startDate = new DateTime(System.currentTimeMillis());
//            DateTime endDate = new DateTime(System.currentTimeMillis()+24 * 60 * 60 * 1000);
//
//            Log.v(TAG,"Start Date value: "+startDate .toString());
//            Log.v(TAG,"End Date value: "+endDate.toString());

            LocalDateTime now = LocalDateTime.now();
            int year = now.getYear();
            int month = now.getMonthOfYear();
            int day = now.getDayOfMonth();

            org.joda.time.DateTime startDateJoda= new org.joda.time.DateTime(year,month,day,7,00);
            org.joda.time.DateTime endDateJoda= new org.joda.time.DateTime(year,month,day,22,00);

            // Convert from Joda-Time to old bundled j.u.Date
            java.util.Date juDateStart = startDateJoda.toDate();
            java.util.Date juDateEnd = endDateJoda.toDate();
            Log.v(TAG,"Start Date joda value: "+juDateStart.toString());
            Log.v(TAG,"End Date joda value: "+juDateEnd.toString());

            // Convert from j.u.Date to Google Date.
            com.google.api.client.util.DateTime googleDateTimeStart = new com.google.api.client.util.DateTime( juDateStart );
            com.google.api.client.util.DateTime googleDateTimeEnd = new com.google.api.client.util.DateTime( juDateEnd );
            Log.v(TAG,"Start Date Google value: "+googleDateTimeStart.toString());
            Log.v(TAG,"End Date Google value: "+googleDateTimeEnd.toString());

            List<String> eventStrings = new ArrayList<String>();

            org.joda.time.DateTime rootStart = startDateJoda;
            org.joda.time.DateTime rootEnd = endDateJoda;

            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(googleDateTimeStart)
                    .setTimeMax(googleDateTimeEnd)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();


            List<Event> items = events.getItems();
            Log.v(TAG,"Items : "+items.toString()+"\n");
            Log.v(TAG,"Items size : "+items.size());
            int interval = 1 ; // how big single slot should be (in this case 1 hrs)

            ArrayList<MyEvent> freeSlots = new ArrayList<MyEvent>();
//            ArrayList<MyEvent> freeSlots = new ArrayList<MyEvent>();
            for (int index =0;index<items.size();index++) {
                Event event = items.get(index);
                Log.v(TAG,"Items Index: "+event.toString()+"\n");

                DateTime teststart=event.getStart().getDateTime();
                DateTime testend=event.getEnd().getDateTime();
                Log.v(TAG,"Test Start Date value: "+teststart.toString());
                Log.v(TAG,"Test End Date value: "+testend.toString());

                long milliseconds1 = teststart.getValue();
                long milliseconds2 = testend.getValue();

                org.joda.time.DateTime eventStartJoda= new org.joda.time.DateTime(milliseconds1);
                org.joda.time.DateTime eventEndJoda= new org.joda.time.DateTime(milliseconds2);

                Log.v(TAG,"Event Start Date joda value: "+eventStartJoda.toString());
                Log.v(TAG,"Event Date joda value: "+eventEndJoda.toString());

                Log.v(TAG,"Before if index == 0 && start<end");
                if ((index == 0) && (startDateJoda.isBefore(eventStartJoda)) ) {
                    Log.v(TAG,"Before Inside if index == 0 && start<end");
                    freeSlots.add( new MyEvent(startDateJoda,eventEndJoda) );
                    Log.v(TAG,"After Inside if index == 0 && start<end");
                }

                if (index == 0) {
                    Log.v(TAG,"Before else if index == 0");
                    DateTime teststart2=event.getEnd().getDateTime();
                    long milliseconds3 = teststart2.getValue();
                    startDateJoda=new org.joda.time.DateTime(milliseconds3);
                    Log.v(TAG,"startDateJoda value: "+startDateJoda.toString());
                }

                long milliseconds5=0;
                Log.v(TAG,"Before teststart4");
                if(index!=0) {
                    DateTime teststart4 = items.get(index - 1).getEnd().getDateTime();
                    Log.v(TAG, "After teststart4");
                    milliseconds5 = teststart4.getValue();
                    Log.v(TAG, "Before if milliseconds5");
                }
                if (new org.joda.time.DateTime(milliseconds5).isBefore(eventStartJoda)) {
                    if(index!=0) {
                        freeSlots.add(new MyEvent
                                (new org.joda.time.DateTime(milliseconds5), eventStartJoda));
                    Log.v(TAG,"xxxxxxx1 value: "+ new org.joda.time.DateTime(milliseconds5).toString());
                    }
                }

                DateTime teststart3=event.getEnd().getDateTime();
                long milliseconds4 = teststart3.getValue();

                if ((items.size() == (index + 1)) && new org.joda.time.DateTime(milliseconds4).isBefore(endDateJoda)) {
                    freeSlots.add(new MyEvent(eventEndJoda, endDateJoda));
                    Log.v(TAG,"xxxxxxx2 value: "+ new org.joda.time.DateTime(milliseconds4).toString());
                }
            }
            Log.v(TAG,"Before outside items size == 0 ");
            if (items.size() == 0) {
                Log.v(TAG,"Before Inside items size == 0 ");
                freeSlots.add(new MyEvent(startDateJoda,endDateJoda));
                Log.v(TAG,"After Inside items size == 0 ");
            }
            Log.v(TAG,"After outside items size == 0 ");

            ArrayList<MyEvent> hourSlots = new ArrayList<MyEvent>();
            org.joda.time.DateTime tempstart= null;
            org.joda.time.DateTime tempend= null;
            MyEvent temp = new MyEvent();
            int val=0;
            for(int index =0;index<freeSlots.size();index++){
                MyEvent free = (MyEvent) freeSlots.get(index);
                Log.v(TAG,"Free slot size : "+freeSlots.size());
                int freeHours = free.endDate.getHourOfDay()- free.startDate.getHourOfDay();
                Log.v(TAG,"FreeHours: "+freeHours);
                org.joda.time.DateTime freeStart = free.startDate, freeEnd = free.endDate;
                Log.v(TAG,"Free Start Date: "+free.startDate.toString()+"Free End Date: "+free.endDate.toString());
                Log.v(TAG,"Before eventStrings: "+free.startDate.toString()+" : "+free.endDate.toString());



//                eventStrings.add(
//                               String.format(" "+freeStart.toString("dd/MM/yy HH:mm:ss")+" - "+freeEnd.toString("dd/MM/yy HH:mm:ss")));
//                Log.v(TAG,"Before while eventStrings: "+free.startDate.toString("dd/MM/yy HH:mm:ss")+" : "+free.endDate.toString("dd/MM/yy HH:mm:ss"));
//
//
//                Log.v(TAG,"Hour slots : "+hourSlots.toString());
//                Log.v(TAG,"GetHour slots : "+freeStart.getHourOfDay());
//                Log.v(TAG,"free hours : "+freeHours);
//                Log.v(TAG,"interval : "+interval);

                while(freeStart.getHourOfDay() + freeHours + interval>=0) { // 11 + 4 + 1 >= 0
                    if(freeHours>=interval) {
                        Log.v(TAG,"free startDate : "+free.startDate);
//                        temp.endDate = free.startDate;
                        tempend=free.startDate;
                        Log.v(TAG,"Temp startDate : "+tempend);
//                        temp.endDate= temp.endDate.hourOfDay().setCopy(temp.endDate.getHourOfDay()+freeHours);
                        tempend= tempend.hourOfDay().setCopy(tempend.getHourOfDay()+freeHours);
                        Log.v(TAG,"Tmp Start Date1: "+String.valueOf(tempstart)+"Tmp End Date1: "+String.valueOf(tempend));
//                        temp.startDate = free.startDate;
                        tempstart=free.startDate;
                        Log.v(TAG,"Temp endDate : "+tempstart);
//                        temp.startDate = temp.startDate.hourOfDay().setCopy(temp.startDate.getHourOfDay()+freeHours-interval);

                        tempstart = tempstart.hourOfDay().setCopy(tempstart.getHourOfDay()+freeHours-interval);
                        Log.v(TAG,"Tmp Start Date2: "+tempstart+"Tmp End Date2: "+tempend);
                        if(tempstart.getHourOfDay() >= freeStart.getHourOfDay() && tempend.getHourOfDay() <= freeEnd.getHourOfDay()) {
                            Log.v(TAG,"While loop inside if condition inside if condition inside");
                            if(val!=0) {
                                hourSlots.add(new MyEvent(tempstart, tempend));
                                Log.v(TAG, "Tmp hour slot: " + tempstart + " : " + tempend);
                                eventStrings.add(
                                        String.format(" " + tempstart.toString("dd/MM/yy HH:mm:ss") + " - " + tempend.toString("dd/MM/yy HH:mm:ss")));
                                Log.v(TAG, "After eventStrings: " + tempstart.toString("dd/MM/yy HH:mm:ss") + " : " + tempend.toString("dd/MM/yy HH:mm:ss"));
                                int hour=Integer.valueOf(tempstart.toString("HH"));
                                int mminute=Integer.valueOf(tempstart.toString("mm"));
                                Log.v(TAG,"Alarm Time "+hour+":"+mminute);
                                startAlarm(hour,mminute);
                            }
                            val++;
                            tempstart=null;
                            tempend=null;
                        }
                    }
                    freeHours--;
                }


            }

            Log.v(TAG,"Event Strings : "+eventStrings);
            return eventStrings;
        }

        int broadcastCode=0;
        public void startAlarm(int hour,int mminute){
            broadcastCode++;
            Intent intent=new Intent(MainActivity.this,MyBroadcastReceiver.class);
            PendingIntent pendingIntent=PendingIntent.getBroadcast(MainActivity.this,broadcastCode,intent,0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Calendar cal_alarm=Calendar.getInstance();
            cal_alarm.set(Calendar.HOUR_OF_DAY,hour);
            cal_alarm.set(Calendar.MINUTE,mminute);
            cal_alarm.set(Calendar.SECOND,00);
            Log.v(TAG,"Broadcast code : "+broadcastCode);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,cal_alarm.getTimeInMillis(),pendingIntent);
                Log.v(TAG,"Set Alarm For : "+hour+":"+mminute);
//            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal_alarm.getTimeInMillis(),
//                    AlarmManager.INTERVAL_DAY, pendingIntent);
//                Toast.makeText(MainActivity.this,"Alarm > KITKAT & Alarm Set For: "+hour+" : "+mminute,Toast.LENGTH_SHORT).show();
            }
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
                alarmManager.set(AlarmManager.RTC_WAKEUP,cal_alarm.getTimeInMillis(),pendingIntent);
                Log.v(TAG,"Set Alarm For : "+hour+":"+mminute);
//            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal_alarm.getTimeInMillis(),
//                    AlarmManager.INTERVAL_DAY, pendingIntent);
//                Toast.makeText(MainActivity.this,"Alarm < KITKAT & Alarm Set For: "+hour+" : "+mminute,Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }

    }
}

package id.andaglos.jadwaldosenadmin.ruangan;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.kishan.askpermission.AskPermission;
import com.kishan.askpermission.ErrorCallback;
import com.kishan.askpermission.PermissionCallback;
import com.kishan.askpermission.PermissionInterface;

import id.andaglos.jadwaldosenadmin.MainActivity;
import id.andaglos.jadwaldosenadmin.R;
import id.andaglos.jadwaldosenadmin.config.CrudService;
import id.andaglos.jadwaldosenadmin.config.Value;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TambahRuangan extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PermissionCallback, ErrorCallback {

    private static final int REQUEST_PERMISSIONS = 20;

    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();

    //VARIABEL YG DIBUTUHKAN UNTUK AKSES LOKASI
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    LocationManager locationManager ;
    boolean GpsStatus ;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    // UI elements
    EditText edtKodeRuangan, edtNamaRuangan, edtGedung, edtLatitudeRuangan, edtLongitudeRuangan, edtBatasJarakAbsen, updateLokasi;
    Button btnTambahRuangan, btnLihatData, btnLokasi;

    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_ruangan);

        edtKodeRuangan = (EditText) findViewById(R.id.edtKodeRuangan);
        edtNamaRuangan = (EditText) findViewById(R.id.edtNamaRuangan);
        edtGedung = (EditText) findViewById(R.id.edtGedung);
        edtLatitudeRuangan = (EditText) findViewById(R.id.edtLatitudeRuangan);
        edtLongitudeRuangan = (EditText) findViewById(R.id.edtLongitudeRuangan);
        edtBatasJarakAbsen = (EditText) findViewById(R.id.edtBatasjarakAbsen);

        btnTambahRuangan = (Button) findViewById(R.id.btnTambahRuangan);
        btnLihatData = (Button) findViewById(R.id.btnLihatRuangan);
        btnLokasi = (Button) findViewById(R.id.btnLokasi);

        btnTambahRuangan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prosesTambahRuangan();
            }
        });

        // Show location button click listener
        btnLihatData.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                lihatRuangan();
            }
        });

        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();

            createLocationRequest();
        }

        CekStatusGPS();
        reqPermission();

//        btnLokasi.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                displayLocation();
//            }
//        });

        // Toggling the periodic location updates
        btnLokasi.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                togglePeriodicLocationUpdates();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();
        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    //PROSES INSERT RUANGAN
    private void prosesTambahRuangan() {

        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Loading ...");
        progress.show();

        CrudService crud = new CrudService();
        crud.tambahRuangan(edtKodeRuangan.getText().toString(), edtNamaRuangan.getText().toString(), edtGedung.getText().toString(),
                edtLatitudeRuangan.getText().toString(), edtLongitudeRuangan.getText().toString(), edtBatasJarakAbsen.getText().toString(), new Callback<Value>() {
                    @Override
                    public void onResponse(Call<Value> call, Response<Value> response) {

                        String value = response.body().getValue();
                        String message = response.body().getMessage();

                        if (value.equals("1")) {
                            progress.dismiss();
                            Toast.makeText(TambahRuangan.this, message, Toast.LENGTH_SHORT).show();
                            edtKodeRuangan.setText("");
                            edtNamaRuangan.setText("");
                            edtGedung.setText("");
                            edtLatitudeRuangan.setText("");
                            edtLongitudeRuangan.setText("");
                            edtBatasJarakAbsen.setText("");
                            edtKodeRuangan.requestFocus();
                        } else {
                            Toast.makeText(TambahRuangan.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call call, Throwable t) {
                        progress.dismiss();
                        Toast.makeText(TambahRuangan.this, "Jaringan Error!", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void lihatRuangan() {
        startActivity(new Intent(TambahRuangan.this, ListRuangan.class));
    }

    /**
     * Method to display the location on UI
     * */
    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            edtLatitudeRuangan.setText(String.valueOf(latitude));
            edtLongitudeRuangan.setText(String.valueOf(longitude));

        } else {

            Toast.makeText(TambahRuangan.this, "Couldn't get the location. Make sure location is enabled on the device!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
            btnLokasi.setText("BERHENTI PERBARUI LOKASI");

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text
            btnLokasi.setText("PERBARUI LOKASI");

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }

    //PERIKSA STATUS GPS MOBILE
    public void CekStatusGPS(){

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(GpsStatus == true) {
        }
        else {
            konfirmasiSettingGPS();
        }
    }

    //KONFIRMASI GPS MOBILE
    private void konfirmasiSettingGPS(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Aplikasi Ini Membutuhkan GPS Akurasi Tinggi, Aktifkan GPS ?");

        //JIKA PILIH IYA
        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });

        //JIKA PILIH TIDAK
        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        Toast.makeText(this, "Perizinan Diterima.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionsDenied(int requestCode) {
        reqPermission();
    }

    //untuk memberitahu user kenapa user harus mengijinkan app menggunakan hal-hal yang dibutuhkan app.
    @Override
    public void onShowRationalDialog(final PermissionInterface permissionInterface, int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Kami Membutuhkan Izin Lokasi Untuk Aplikasi Ini. Buka Perizinan ?");
        builder.setPositiveButton("oke", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                permissionInterface.onDialogShown();
            }
        });
        builder.show();
    }

    //Buka menu perizinan untuk aplikasi ini
    @Override
    public void onShowSettings(final PermissionInterface permissionInterface, int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Kami Membutuhkan Izin Lokasi Untuk Aplikasi Ini. Buka Perizinan ?");
        builder.setPositiveButton("Iya", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                permissionInterface.onSettingsShown();
            }
        });
        builder.show();
    }

    private void reqPermission() {
        new AskPermission.Builder(this).setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION)
                .setCallback(this)
                .setErrorCallback(this)
                .request(REQUEST_PERMISSIONS);
    }
}

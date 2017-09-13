package id.andaglos.jadwaldosenadmin.ruangan;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import butterknife.ButterKnife;
import id.andaglos.jadwaldosenadmin.MainActivity;
import id.andaglos.jadwaldosenadmin.R;
import id.andaglos.jadwaldosenadmin.config.CrudService;
import id.andaglos.jadwaldosenadmin.config.Value;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateRuangan extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private ProgressDialog progress;
    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    private FloatingActionButton btnEditLokasi;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    EditText edtKodeRuangan,edtNamaRuangan,edtGedung,edtLatitudeRuangan,edtLongitudeRuangan,edtBatasJarakAbsen,edtLatitudeRuanganBaru,edtLongitudeRuanganBaru;
    Button btnUpdate;
    TextView txtCopy;
    String id;


    public void prosesUpdateRuangan(){
        //membuat progress dialog
        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Loading ...");
        progress.show();

        //mengambil data dari edittext
        String kode_ruangan = edtKodeRuangan.getText().toString();
        String nama_ruangan = edtNamaRuangan.getText().toString();
        String gedung = edtGedung.getText().toString();
        String latitude = edtLatitudeRuangan.getText().toString();
        String longitude = edtLongitudeRuangan.getText().toString();
        String batas_jarak_absen = edtBatasJarakAbsen.getText().toString();
        String latitude_baru = edtLatitudeRuanganBaru.getText().toString();
        String longitude_baru = edtLongitudeRuanganBaru.getText().toString();

        CrudService crud = new CrudService();
        crud.updateRuangan(id, kode_ruangan, nama_ruangan, gedung, latitude, longitude, batas_jarak_absen, new Callback<Value>() {
            @Override
            public void onResponse(Call<Value> call, Response<Value> response) {
                String value = response.body().getValue();
                String message = response.body().getMessage();
                progress.dismiss();
                if (value.equals("1")) {
                    Toast.makeText(UpdateRuangan.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(UpdateRuangan.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Value> call, Throwable t) {
                t.printStackTrace();
                progress.dismiss();
                Toast.makeText(UpdateRuangan.this, "Jaringan Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_ruangan);
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Ubah Ruangan");

        Intent intent = getIntent();
        String kode_ruangan = intent.getStringExtra("kode_ruangan");
        String nama_ruangan = intent.getStringExtra("nama_ruangan");
        String gedung = intent.getStringExtra("lokasi_ruangan");
        String latitude = intent.getStringExtra("latitude");
        String longitude = intent.getStringExtra("longitude");
        String batas_jarak = intent.getStringExtra("batas_jarak_absen");
        id = intent.getStringExtra("id");

        btnUpdate = (Button) findViewById(R.id.btnUpdate);
        txtCopy = (Button) findViewById(R.id.copy_paste);

        edtKodeRuangan = (EditText) findViewById(R.id.edtTextKodeRuangan);
        edtNamaRuangan = (EditText) findViewById(R.id.edtTextNamaRuangan);
        edtGedung = (EditText) findViewById(R.id.edtTextGedung);
        edtLatitudeRuangan = (EditText) findViewById(R.id.edtTextLatitudeRuangan);
        edtLongitudeRuangan = (EditText) findViewById(R.id.edtTextLongitudeRuangan);
        edtBatasJarakAbsen = (EditText) findViewById(R.id.edtTextBatasjarakAbsen);
        btnEditLokasi = (FloatingActionButton) findViewById(R.id.btnEditLokasi);

        edtLatitudeRuanganBaru = (EditText) findViewById(R.id.edtTextLatitudeRuanganBaru);
        edtLongitudeRuanganBaru = (EditText) findViewById(R.id.edtTextLongitudeRuanganBaru);

        edtKodeRuangan.setText(kode_ruangan);
        edtNamaRuangan.setText(nama_ruangan);
        edtGedung.setText(gedung);
        edtLatitudeRuangan.setText(latitude);
        edtLongitudeRuangan.setText(longitude);
        edtBatasJarakAbsen.setText(batas_jarak);


        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vaidate_form() == true) {
                    prosesUpdateRuangan();
                }
            }
        });


        txtCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyLocation();
            }
        });

        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
            createLocationRequest();
        }

       // Show location button click listener
//        btnEditLokasi.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                tampilLokasi();
//            }
//        });

        // Toggling the periodic location updates
        btnEditLokasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePeriodicLocationUpdates();
            }
        });
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.action_delete:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Peringatan");
                alertDialogBuilder
                        .setIcon(R.drawable.logofinish)
                        .setMessage("Apakah Anda Yakin Ingin Mengapus Data Ini?")
                        .setCancelable(false)
                        .setPositiveButton("Hapus",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id_ruangan) {


                                CrudService crud = new CrudService();
                                crud.hapusRuangan(id, new Callback<Value>() {

                                    @Override
                                    public void onResponse(Call<Value> call, Response<Value> response) {
                                        String value = response.body().getValue();
                                        String message = response.body().getMessage();
                                        if (value.equals("1")) {
                                            Toast.makeText(UpdateRuangan.this, message, Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(UpdateRuangan.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call call, Throwable t) {
                                        t.printStackTrace();
                                        Toast.makeText(UpdateRuangan.this, "Terjadi Kesalahan!", Toast.LENGTH_SHORT).show();
                                    }
                                });


                            }
                        })
                        .setNegativeButton("Batal",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method to display the location on UI
     * */
    private void tampilLokasi() {

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

            edtLatitudeRuanganBaru.setText(String.valueOf(latitude));
            edtLongitudeRuanganBaru.setText(String.valueOf(longitude));

        } else {

            Toast.makeText(UpdateRuangan.this, "Couldn't get the location. Make sure location is enabled on the device!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates() {
//        if (!mRequestingLocationUpdates) {
            // Changing the button text

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

//        }
//        else {
//            // Changing the button text
//
//            mRequestingLocationUpdates = false;
//
//            // Stopping the location updates
//            stopLocationUpdates();
//
//            Log.d(TAG, "Periodic location updates stopped!");
//        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
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
     * VERIFIKASI KE GOOGLE PLAY SERVICES
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else {
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
        tampilLokasi();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    //MENAMPILKAN TOMBOL HAPUS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_delete, menu);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Lokasi Diubah!",
                Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        tampilLokasi();
    }

    public void copyLocation(){

        //mengambil data dari edittext
        String latitude = edtLatitudeRuanganBaru.getText().toString();
        String longitude = edtLongitudeRuanganBaru.getText().toString();

        edtLatitudeRuangan.setText(String.valueOf(latitude));
        edtLongitudeRuangan.setText(String.valueOf(longitude));

    }
    //JIKA KOLOM TIDAK DIISI KETIKA DIEDIT
    private boolean vaidate_form(){

        if (edtKodeRuangan.getText().toString().equals("")){

            edtKodeRuangan.setError("Silahkan Isi Kode Ruangan !");
            edtKodeRuangan.requestFocus();

            return false;
        }
        else if (edtNamaRuangan.getText().toString().equals("")){

            edtNamaRuangan.setError("Silahkan Isi Nama Ruangan !");
            edtNamaRuangan.requestFocus();

            return false;
        }
        else if (edtGedung.getText().toString().equals("")){

            edtGedung.setError("Silahkan Isi Gedung !");
            edtGedung.requestFocus();

            return false;
        }
        else if (edtLatitudeRuangan.getText().toString().equals("")){

            edtLatitudeRuangan.setError("Silahkan Isi Latitude !");
            edtLatitudeRuangan.requestFocus();

            return false;
        }
        else if (edtLongitudeRuangan.getText().toString().equals("")){

            edtLongitudeRuangan.setError("Silahkan Isi Logitude !");
            edtLongitudeRuangan.requestFocus();

            return false;
        }
        else if (edtBatasJarakAbsen.getText().toString().equals("")){

            edtBatasJarakAbsen.setError("Silahkan Isi Batas Jarak Absen !");
            edtBatasJarakAbsen.requestFocus();

            return false;
        }
        else{

            return  true;
        }

    }

}

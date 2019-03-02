package com.example.jaff.reiziger;
import com.example.jaff.reiziger.models.PlaceInfo;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/***CONTEXT*****************************************************************************************
 *   Main Activity -->
 *   class allowing the management of the Maps API and user data with different user functionalities.
 ***************************************************************************************************/

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "Reiziger";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private static final int PICK_IMAGE = 100;
    // General
    private AutoCompleteTextView mSearchText;
    private Button buttonMenu;
    private Button mGps, mGpsLarge, mDelete, mLink, mHelp;
    private String idTracer;
    private Dialog myHelpDialog;
    // BottomSheetBehavior
    private BottomSheetBehavior bottomSheetBehavior;
    private TextView placeName, placeAddress, placeDateArrived, placeDateDeparture;
    private LinearLayout ldate;
    private DatePickerDialog.OnDateSetListener mDateSetListenerDeparture, mDateSetListenerArrived;
    private ImageView placeImage;
    private Uri imageURI;
    // Map
    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private PlaceAutoCompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    // DataBase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference myDbRef;
    ValueEventListener mUserListListener;
    private ArrayList<PlaceInfo> lPlaceInfo;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
    // Charging style Maps / Permission location / Display Maps elements --> initialisation()
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Welcome traveller", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        //  Style MAPS
        try {
            // Customise the styling of the base map using a JSON object defined in a raw resource file.
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed."); }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e); }

        // if authorization is OK for location
        if (mLocationPermissionsGranted) {
            getDeviceLocation((float) 7.0);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // Display Maps elements
            mMap.setMyLocationEnabled(true);    // blue point localisation
            mMap.getUiSettings().setMyLocationButtonEnabled(false); // icon come back location
            mMap.getUiSettings().setZoomControlsEnabled(false);     // icons zoom maps
            mMap.getUiSettings().setMapToolbarEnabled(false);       // icons toolbar
            initialisation();
        }
    }
    // Bind data to lists / associate the activity with a ViewModel --> readDatabase()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //Fullscreen activity
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide(); //remove title bar
        overridePendingTransition(R.anim.fade_in_longtime, R.anim.fade_out_longtime); // Transition fade

        mSearchText = findViewById(R.id.input_search);
        // Menu and icons
        buttonMenu = findViewById(R.id.fab_main_menu);
        mGps = findViewById(R.id.ic_location);
        mGps.setVisibility(View.GONE);
        mGpsLarge = findViewById(R.id.ic_location_large);
        mGpsLarge.setVisibility(View.GONE);
        mDelete = findViewById(R.id.ic_delete);
        mDelete.setVisibility(View.GONE);
        //mLink = findViewById(R.id.ic_link);
        //mLink.setVisibility(View.GONE);
        mHelp = findViewById(R.id.ic_help);
        mHelp.setVisibility(View.GONE);
        myHelpDialog = new Dialog(this);

        View nestedScrollView = findViewById(R.id.nestedScrollView);
        bottomSheetBehavior = BottomSheetBehavior.from(nestedScrollView);
        bottomSheetBehavior.setState(bottomSheetBehavior.STATE_HIDDEN);
        placeName = findViewById(R.id.placeName);
        placeAddress = findViewById(R.id.placeAddress);
        ldate = findViewById(R.id.date);
        placeDateArrived = findViewById(R.id.placeDateArrived);
        placeDateDeparture = findViewById(R.id.placeDateDeparture);
        placeImage = findViewById(R.id.placeImage);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() == null){
                    //startActivity(new Intent(MainActivity.this, Connexion.class));
                    MainActivity.this.finish();
                }
            }
        };
        getLocationPermission();
        readDatabase();
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart !");
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**
     * ---------MAPS Initialisation-----------------------------------------------------------------
     **/
    // Button, widget visible or not / View.clickListener
    private void initialisation(){
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mPlaceAutocompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setOnItemClickListener(mAutocompleteClickListener);
        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){
                    //execute our method for searching
                    geolocatingLocate();
                }
                return false;
            }
        });
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked menu icon");
                if(mGps.isShown() && mGpsLarge.isShown() && mHelp.isShown()){
                    mGps.setVisibility(View.GONE);
                    mGpsLarge.setVisibility(View.GONE);
                    mHelp.setVisibility(View.GONE);
                } else {
                    mGps.setVisibility(View.VISIBLE);
                    mGpsLarge.setVisibility(View.VISIBLE);
                    mHelp.setVisibility(View.VISIBLE);
                }

            }
        });
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                cleanMap();
                getDeviceLocation((float) 7.0);
            }
        });
        mGpsLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                cleanMap();
                getDeviceLocation((float) 1.0);
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                idTracer = marker.getId();
                idTracer = idTracer.substring(idTracer.lastIndexOf("m")+1);
                chargeDataIntoBottomSheet(Integer.parseInt(idTracer));        // Charge Data in the bottom window
                mDelete.setVisibility(View.VISIBLE);        // Show icon link
                //mLink.setVisibility(View.VISIBLE);          // Show icon delete
                bottomSheetBehavior.setState(bottomSheetBehavior.STATE_COLLAPSED);      // Show Bottom Window

                return false;
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                cleanMap();     // Show only map
            }
        });
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked Delete icon");
                if(idTracer!=null){
                    deleteMarker();     // Show popup for delete or not the marker selected
                }
            }
        });
        /*mLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked Link icon");
            }
        });*/
        mHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanMap();
                showPopupHelp(v);
            }
        });
        placeDateArrived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked date");
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month =c.get(Calendar.MONTH);
                int day =c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialogDate = new DatePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mDateSetListenerArrived, year, month, day);
                dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogDate.show();
            }
        });
        placeDateDeparture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked date");
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month =c.get(Calendar.MONTH);
                int day =c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialogDate = new DatePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mDateSetListenerDeparture, year, month, day);
                dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogDate.show();
            }
        });
        mDateSetListenerArrived = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month +1;
                String date = month +" / "+ dayOfMonth +" / "+year;
                placeDateArrived.setText(date);
                String idMarker = lPlaceInfo.get(Integer.parseInt(idTracer)).getId();
                myDbRef = FirebaseDatabase.getInstance().getReference("users").child(nameCompte()).child(idMarker);
                myDbRef.child("dateArrived").setValue(date);
                lPlaceInfo.get(Integer.parseInt(idTracer)).setDateArrived(date);
            }
        };
        mDateSetListenerDeparture = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month +1;
                String date = month +" / "+ dayOfMonth +" / "+year;
                placeDateDeparture.setText(date);
                String idMarker = lPlaceInfo.get(Integer.parseInt(idTracer)).getId();
                myDbRef = FirebaseDatabase.getInstance().getReference("users").child(nameCompte()).child(idMarker);
                myDbRef.child("dateDeparture").setValue(date);
                lPlaceInfo.get(Integer.parseInt(idTracer)).setDateDeparture(date);
            }
        };
        placeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked image");
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PICK_IMAGE);

            }
        });
        hideSoftKeyboard();
    }
    // Hide functions on the map
    private void cleanMap(){
        mDelete.setVisibility(View.GONE);
        //mLink.setVisibility(View.GONE);
        bottomSheetBehavior.setState(bottomSheetBehavior.STATE_HIDDEN);
    }
    // Action of the search bar
    private void geolocatingLocate(){

        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> lAdress = new ArrayList<>();
        try{
            lAdress = geocoder.getFromLocationName(searchString, 1);
            Toast.makeText(this, searchString, Toast.LENGTH_SHORT).show();
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(lAdress.size() > 0){
            Address address = lAdress.get(0);
            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()),
                    address.getAddressLine(0),(float)7.0);

        }
    }
    private void getDeviceLocation(final float zoom){
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    "My Location",zoom);

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MainActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    /**
     * --------Display my location------------------------------------------------------------------
     **/
    private void moveCamera(LatLng latLng, String title,float zoom){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            LatLng lg = new LatLng(latLng.latitude-10.0,latLng.longitude);
            MarkerOptions options = new MarkerOptions()
                    .position(lg)
                    .title(title);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    /**
     * ------------Display Maps---------------------------------------------------------------------
     **/
    private void initializingMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }

    /**
     * -------Location permission user for using maps------------------------------------------------
     **/
    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initializingMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;
        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initializingMap();
                }
            }
        }
    }
    private void hideSoftKeyboard(){
        InputMethodManager imm = (InputMethodManager) mSearchText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * ---------google places API autocomplete suggestions------------------------------------------
     **/
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try{
                PlaceInfo mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.setAddress(Objects.requireNonNull(place.getAddress()).toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id:" + place.getId());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.setDateArrived("Date arrived");
                mPlace.setDateDeparture("Date departure");
                mPlace.setUri("");
                // Write on DataBase
                writeNewPlaceInfo(mPlace.getName(), mPlace.getAddress(), mPlace.getId(), mPlace.getLatlng(), mPlace.getDateArrived(), mPlace.getDateDeparture(), mPlace.getUri());
            }catch (NullPointerException e){
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Objects.requireNonNull(place.getViewport()).getCenter().latitude,
                    place.getViewport().getCenter().longitude), 6f));
            mSearchText.setText("");
            places.release();
        }
    };

    /**
     * ---------------------DATABASE----------------------------------------------------------------
     */
    // Recovery name of count Google
    public String nameCompte(){
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            return acct.getDisplayName();
        }
        return null;
    }
    // Write Into Database
    public void writeNewPlaceInfo(String name, String address, String id, LatLng latlng,  String dateArrived, String dateDeparture, String uri) {
        // Assign FirebaseDatabase instance into database object for further use.
        myDbRef = FirebaseDatabase.getInstance().getReference("users");
        // Build Objet PlaceInfo
        PlaceInfo placeInfo = new PlaceInfo(name, address, id, latlng, dateArrived, dateDeparture, uri);
        // Insert Objet PlaceInfo
        myDbRef.child(nameCompte()).child(id).setValue(placeInfo);
    }
    // Recovery all information from User
    public void readDatabase(){
        myDbRef = FirebaseDatabase.getInstance().getReference("users").child(nameCompte());
        lPlaceInfo = new ArrayList<>();

        final ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                //Loop of nodes id Maps
                for (DataSnapshot child: dataSnapshot.getChildren())
                    if (child.child("id").getValue(String.class) != null) {
                        // Recovers the value of the fields
                        String id = child.child("id").getValue(String.class);
                        String name = child.child("name").getValue(String.class);
                        String address = child.child("address").getValue(String.class);
                        Double latitude = child.child("latlng").child("latitude").getValue(Double.class);
                        Double longitude = child.child("latlng").child("longitude").getValue(Double.class);
                        LatLng latlng = new LatLng(latitude, longitude);
                        String dateArrived = child.child("dateArrived").getValue(String.class);
                        String dateDeparture = child.child("dateDeparture").getValue(String.class);
                        String uri = child.child("uri").getValue(String.class);
                        // Instance PlaceInfo
                        PlaceInfo placeInfo = new PlaceInfo(name, address, id, latlng, dateArrived, dateDeparture, uri);
                        // Display the mark on the map
                        addMarker(new LatLng(latitude, longitude), placeInfo);
                        // Add PlaceInfo into a ArrayList of PlaceInfo
                        lPlaceInfo.add(placeInfo);
                    }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled: ",databaseError.toException());
                Toast.makeText(MainActivity.this, "Problem charging the list",Toast.LENGTH_SHORT).show();
            }
        };
        myDbRef.addValueEventListener(userListener);
        mUserListListener = userListener;
    }
    // Display Marker
    private void addMarker(LatLng latLng, PlaceInfo placeInfo){
        mMap.setInfoWindowAdapter(new WindowInfoCustomAdapter(MainActivity.this));  // Initialise marker info window
        if(placeInfo != null){
            try{
                String snippet = placeInfo.getAddress();       // Display Address
                MarkerOptions options = new MarkerOptions()
                        .position(latLng)   // Sets the location for the marker
                        .title(placeInfo.getName()) // Sets the title for the marker
                        .snippet(snippet)   // Sets Address location
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place));
                mMap.addMarker(options);
            }catch (NullPointerException e){
                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage() );
            }
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }
        hideSoftKeyboard();
    }
    public void deleteMarker(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = "Are you sure you want to delete "+lPlaceInfo.get(Integer.parseInt(idTracer)).getName()+"?";
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String idMarker = lPlaceInfo.get(Integer.parseInt(idTracer)).getId();
                        myDbRef = FirebaseDatabase.getInstance().getReference("users").child(nameCompte()).child(idMarker);
                        myDbRef.removeValue();
                        lPlaceInfo.remove(idTracer);
                        reloadMarkers();
                        cleanMap();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    // Actualize Map with all markers
    public void reloadMarkers(){
        mMap.clear();
        for (int nb=0;nb>=lPlaceInfo.size();nb++){
            addMarker(lPlaceInfo.get(nb).getLatlng(),lPlaceInfo.get(nb));
        }
    }

    /**
     * ---------Display Help Windows OnClick icon help----------------------------------------------
     **/
    public void showPopupHelp(View v) {
        TextView txtclose;
        myHelpDialog.setContentView(R.layout.window_help);
        txtclose = myHelpDialog.findViewById(R.id.txtclose);
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myHelpDialog.dismiss();
            }
        });
        Objects.requireNonNull(myHelpDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myHelpDialog.show();
    }

    /**
     * ---------------------Bottom Sheet------------------------------------------------------------
     */
    private void chargeDataIntoBottomSheet(int id) {
        placeName.setText(lPlaceInfo.get(id).getName());
        placeAddress.setText(lPlaceInfo.get(id).getAddress());
        placeDateDeparture.setText(lPlaceInfo.get(id).getDateDeparture());
        placeDateArrived.setText(lPlaceInfo.get(id).getDateArrived());
        Uri imgUri = Uri.parse(lPlaceInfo.get(id).getUri());
        placeImage.setImageURI(imgUri);
        //Toast.makeText(this, lPlaceInfo.get(id).getUri(), Toast.LENGTH_LONG).show();
    }
    // Action for charging picture into ImageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            try{
                imageURI = data.getData();
                placeImage.setImageURI(imageURI);

                String idMarker = lPlaceInfo.get(Integer.parseInt(idTracer)).getId();
                myDbRef = FirebaseDatabase.getInstance().getReference("users").child(nameCompte()).child(idMarker);
                myDbRef.child("uri").setValue(imageURI.toString());
                lPlaceInfo.get(Integer.parseInt(idTracer)).setUri(imageURI.toString());
            }catch (Exception e) {
                Toast.makeText(this, "Error Message : ", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * ---------------------QUIT Application--------------------------------------------------------
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
}


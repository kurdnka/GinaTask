package com.tomi.ginatask;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nutiteq.core.MapPos;
import com.nutiteq.core.MapRange;
import com.nutiteq.datasources.HTTPTileDataSource;
import com.nutiteq.datasources.LocalVectorDataSource;
import com.nutiteq.datasources.TileDataSource;
import com.nutiteq.graphics.Color;
import com.nutiteq.layers.Layers;
import com.nutiteq.layers.RasterTileLayer;
import com.nutiteq.layers.VectorLayer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.styles.LineJointType;
import com.nutiteq.styles.LineStyleBuilder;
import com.nutiteq.styles.MarkerStyle;
import com.nutiteq.styles.MarkerStyleBuilder;
import com.nutiteq.ui.MapView;
import com.nutiteq.utils.BitmapUtils;
import com.nutiteq.vectorelements.BalloonPopup;
import com.nutiteq.vectorelements.Line;
import com.nutiteq.wrappedcommons.MapPosVector;
import com.nutiteq.wrappedcommons.VectorElementVector;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "MainActivity";
    private final String LICENSE_KEY = "XTUMwQ0ZEcGhzd3ZkbHZBUGI2OUdJT2pXUnNEUU9TZ1lBaFVBaUcvZkYydDFOUFM0MzRjUzJNWXhxcTUza05rPQoKcHJvZHVjdHM9c2RrLWFuZHJvaWQtMy4qCnBhY2thZ2VOYW1lPWNvbS50b21pLmdpbmF0YXNrCndhdGVybWFyaz1udXRpdGVxCnVzZXJLZXk9Y2E2YjI4ZTUwOTNjNGMyOTExNGZkNzIwNTYyYzIyYTcK";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private EditableMapView mapView;
    private EPSG3857 proj;

    Button cameraButton;
    Button qrButton;
    Button deviceInfoButton;
    ToggleButton drawButton;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.tomi.ginatask.R.layout.activity_main);

        // 0. The initial step: register your license. This must be done before using MapView!
        // You can get your free/commercial license from: http://developer.nutiteq.com
        // The license string used here is intended only for Nutiteq demos and WILL NOT WORK with other apps!
        MapView.registerLicense(LICENSE_KEY, getApplicationContext());

        // 1. Basic map setup
        // Create map view 
        mapView = (EditableMapView) this.findViewById(com.tomi.ginatask.R.id.map_view);

        // Set the base projection, that will be used for most MapView, MapEventListener and Options methods
        proj = new EPSG3857();
        mapView.getOptions().setBaseProjection(proj); // note: EPSG3857 is the default, so this is actually not required

        // General options
        mapView.getOptions().setRotatable(true); // make map rotatable (this is also the default)
        mapView.getOptions().setTileThreadPoolSize(2); // use 2 download threads for tile downloading

        mapView.getOptions().setTileDrawSize(128);
        // Set initial location and other parameters, don't animate
        mapView.setFocusPos(proj.fromWgs84(new MapPos(16.9, 49.2)), 0); // Berlin
        mapView.setZoom(13, 0); // zoom 2, duration 0 seconds (no animation)
        mapView.setMapRotation(0, 0);
        mapView.setTilt(90, 0);

        // following goes normally to onCreate() of your Activity with map
        Layers retainObject =  (Layers) getLastNonConfigurationInstance();
        if (retainObject != null) {
            // just restore configuration, skip other initializations
            for (int i = 0; i < retainObject.count(); i++) {
                mapView.getLayers().add(retainObject.get(i));
                retainObject.get(i).refresh();

            }
        } else {
            // Create base layer. Use vector style from assets (osmbright.zip)
            TileDataSource source = new HTTPTileDataSource(0, 20, "http://a.tile.openstreetmap.org/{zoom}/{x}/{y}.png");
            RasterTileLayer baseLayer = new RasterTileLayer(source);
            mapView.getLayers().add(baseLayer);
        }

        cameraButton = (Button) findViewById(com.tomi.ginatask.R.id.cameraButton);
        qrButton = (Button) findViewById(com.tomi.ginatask.R.id.qrButton);
        deviceInfoButton = (Button) findViewById(com.tomi.ginatask.R.id.deviceInfoButton);
        drawButton = (ToggleButton) findViewById(com.tomi.ginatask.R.id.drawButton);


        cameraButton.setOnClickListener(cameraButtonListener);
        qrButton.setOnClickListener(qrButtonListener);
        deviceInfoButton.setOnClickListener(deviceInfoButtonListener);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"tomisky008@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "new token");
                i.putExtra(Intent.EXTRA_TEXT, sharedPreferences.getString(QuickstartPreferences.TOKEN, ""));
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ignored) {
                }
            }
        };


        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    private File file;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;

    public View.OnClickListener cameraButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            file = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE); // create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file)); // set the image file name

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    };

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");

        //debug
        Log.i("Directory name: " ,(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)).getPath());


        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.mapView.getLayers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == 0){
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_LONG).show();
                makePhotoMarker(file);
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Log.i("Image capture:","user interrupted");
                Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_LONG).show();
            } else {
                // Image capture failed, advise user
                Log.e("Image capture:","failed");
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG).show();
            }
        }else if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (result != null) {
                if (result.getContents() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("QR scan results")
                            .setMessage(result.getContents())
                            .show();
                } else if (resultCode == RESULT_CANCELED) {
                    // User cancelled the image capture
                    Log.i("QR scan:", "cancelled");
                    Toast.makeText(this, "QR scan cancelled", Toast.LENGTH_LONG).show();
                }else {
                    Log.e("QR scan:","failed");
                    Toast.makeText(this, "QR scan failed", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void makePhotoMarker(File file) {

// Initialize an local vector data source
        LocalVectorDataSource vectorDataSource1 = new LocalVectorDataSource(proj);

// Initialize a vector layer with the previous data source
        VectorLayer vectorLayer1 = new VectorLayer(vectorDataSource1);

// Add the previous vector layer to the map
        mapView.getLayers().add(vectorLayer1);

// Set limited visible zoom range for the vector layer
        vectorLayer1.setVisibleZoomRange(new MapRange(10, 24));

// Load bitmaps for custom markers
        Bitmap photoBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        com.nutiteq.graphics.Bitmap markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(
                getResizedBitmap(photoBitmap, 100, 75)
        );

// Create marker style
        MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
        markerStyleBuilder.setBitmap(markerBitmap);

//markerStyleBuilder.setHideIfOverlapped(false);
        markerStyleBuilder.setSize(30);
        MarkerStyle sharedMarkerStyle = markerStyleBuilder.buildStyle();

// Add marker
        PhotoMarker marker = new PhotoMarker(mapView.getFocusPos(), sharedMarkerStyle);
        marker.setMetaDataElement(file.toString(), "Photo");
        vectorDataSource1.add(marker);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // RECREATE THE NEW BITMAP
        return Bitmap.createBitmap(bm, 0, 0, width, height,
                matrix, false);

    }


    public View.OnClickListener qrButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
            scanIntegrator.initiateScan();
        }
    };


    public View.OnClickListener deviceInfoButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showDeviceInfoDialog();
        }
    };

    private void showDeviceInfoDialog(){
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, intentFilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = (level / (float)scale) * 100;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        builder.setTitle("Device info")
                .setMessage("IMEI: " + telephonyManager.getDeviceId() + "\n\nBattery level: " + batteryPct + "% " + (isCharging ? "(charging)" : "(not charging)"))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }

    private LocalVectorDataSource tempVectorDataSource;
    private VectorLayer vectorLayer;

    public void onToggleClicked(View view){
        if(((ToggleButton) view).isChecked()) {
            tempVectorDataSource = new LocalVectorDataSource(proj);
            vectorLayer = new VectorLayer(tempVectorDataSource);
            mapView.getLayers().add(vectorLayer);
            Toast.makeText(this, "Tap points on map to make a line", Toast.LENGTH_LONG).show();
            mapView.setMapEventListener(new PointMakingMapEventListener(mapView, tempVectorDataSource));
        } else {
            mapView.setMapEventListener(new BasicMapEventListener(mapView, new LocalVectorDataSource(proj)));
            if(tempVectorDataSource != null && vectorLayer != null){
                mapView.getLayers().remove(vectorLayer);
                Log.i("Toggle button","line drawing started");
                LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
                lineStyleBuilder.setColor(new Color(0xFFFF0000));
                lineStyleBuilder.setLineJointType(LineJointType.LINE_JOINT_TYPE_ROUND);
                lineStyleBuilder.setStretchFactor(2);
                lineStyleBuilder.setWidth(8);

                MapPosVector linePoses = new MapPosVector();
                VectorElementVector elements = tempVectorDataSource.getAll();
                for(int i=0; i < elements.size(); i++){

                    linePoses.add(((BalloonPopup) elements.get(i)).getRootGeometry().getCenterPos());
                    Log.i("Toggle button", "added with " + mapView.getOptions().getBaseProjection().toWgs84(((BalloonPopup) elements.get(i)).getRootGeometry().getCenterPos()).toString()
                            + " to line #" + i + " coordinates");
                }
                Line line = new Line(linePoses, lineStyleBuilder.buildStyle());
                line.setMetaDataElement("ClickText", "Line");
                Log.i("Toggle button", "line should be drawn now");
                tempVectorDataSource = new LocalVectorDataSource(proj);
                tempVectorDataSource.add(line);
                vectorLayer = new VectorLayer(tempVectorDataSource);
                mapView.getLayers().add(vectorLayer);
            }
        }
    }

}

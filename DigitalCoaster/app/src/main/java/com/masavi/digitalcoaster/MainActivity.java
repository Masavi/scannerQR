package com.masavi.digitalcoaster;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView barcodeResult;
    TextView requestResult;
    private Handler mHandler;
    private final int CAMERA_REQUEST_CODE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        barcodeResult = (TextView)findViewById(R.id.barcode_result);
        requestResult = (TextView)findViewById(R.id.request_result);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void askPermission(String permission, int requestCode){
        if (ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){
            // We don't have camera permission
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode); // Ask for permission
        } else {
            // We have permission
            Intent intent = new Intent(this, ScanBarcodeActivity.class);    // Start new activity
            startActivityForResult(intent, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){

            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Otorgaste permisos de cámara", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ScanBarcodeActivity.class);
                    startActivityForResult(intent, 0);
                } else {
                    Toast.makeText(this, "Has negado los permisos de cámara. Accede a configuración para otorgar los permisos manualmente", Toast.LENGTH_LONG).show();
                }
        }
    }

    /* Add click event to the scan barcode button*/
    public void scanBarcode(View v) {
        askPermission(Manifest.permission.CAMERA, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0){
            if (resultCode == CommonStatusCodes.SUCCESS){
                if(data!=null){

                    // Insert barcode value into textview
                    Barcode barcode = data.getParcelableExtra("barcode");
                    barcodeResult.setText("Valor del código de barras:\n" + barcode.displayValue);
                    requestResult.setText("Esperando respuesta del servidor...");

                    // HTTP Client setup
                    final OkHttpClient client = new OkHttpClient();
                    final MediaType MEDIA_TYPE = MediaType.parse("application/json");
                    JSONObject postdata = new JSONObject();
                    try {
                        postdata.put("qr", barcode.displayValue);
                    } catch(JSONException e){
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // Generate request body from previous JSON
                    RequestBody body = RequestBody.create(MEDIA_TYPE, postdata.toString());

                    final Request request = new Request.Builder()
                            .url("https://verificaqr.digitalcoaster.mx/")
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    // Make post request to validate barcode value
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            String mMessage = e.getMessage().toString();
                            Log.w("failure Response", mMessage);
                            //call.cancel();
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    requestResult.setText("Ocurrió un error");
                                }
                            });

                        }

                        @Override
                        public void onResponse(Call call, Response response)
                                throws IOException {

                            String mMessage = response.body().string();
                            if (response.isSuccessful()){
                                try {
                                    JSONObject json = new JSONObject(mMessage);
                                    final String serverResponse = json.getString("result");
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            requestResult.setText(serverResponse); // must be inside run()
                                        }
                                    });

                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                } else {
                    barcodeResult.setText("No se encontró el código de barras");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        ;
    }



}



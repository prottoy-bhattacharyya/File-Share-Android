package com.example.myapplication;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class receiveActivity extends AppCompatActivity {

    Button scan_button, go_button;
    String qr_text;
    long downloadID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(onDownloadComplete, filter, RECEIVER_NOT_EXPORTED);
        }

        scan_button = findViewById(R.id.scan_button);
        go_button = findViewById(R.id.go_button);

        scan_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              qr_scanner();
            }
        });

        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_download();
            }
        });

    }

    void qr_scanner(){
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner
                .startScan()
                .addOnSuccessListener(
                        barcode -> {
                            qr_text = barcode.getRawValue();
                        })
                .addOnCanceledListener(
                        () -> {
                            Toast toast = Toast.makeText(getApplicationContext(), "Scan canceled", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                )

                .addOnFailureListener(
                        e -> {
                            Toast toast = Toast.makeText(getApplicationContext(), "Scan failed", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                );


    }

    void start_download(){
        String download_link = "http://10.0.2.2:8000/download";
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri uri = Uri.parse(download_link);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setTitle("file.pdf");
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "file.pdf");
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        downloadID = downloadManager.enqueue(request);
        Toast toast = Toast.makeText(getApplicationContext(), "Download started", Toast.LENGTH_SHORT);
        toast.show();
    }

    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadID) { // Check if it's your specific download
                // Download completed, you can query its status or open the file
                Toast toast = Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }
}
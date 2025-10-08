package com.example.myapplication;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.io.File;

import okhttp3.OkHttpClient;

public class receiveActivity extends AppCompatActivity {

    Button scan_button, go_button;
    String qr_text;
    long downloadID;
    TextView title;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(
                this,
                onDownloadComplete,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        scan_button = findViewById(R.id.scan_button);
        go_button = findViewById(R.id.go_button);
        title = findViewById(R.id.title_text);

        scan_button.setOnClickListener(view -> qr_scanner());

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
                            scan_button.setText(qr_text);
                        })
                .addOnCanceledListener(
                        () -> {
                            Toast.makeText(getApplicationContext(), "Scan canceled", Toast.LENGTH_SHORT).show();
                        }
                )

                .addOnFailureListener(
                        e -> {
                            Toast.makeText(getApplicationContext(), "Scan failed", Toast.LENGTH_SHORT).show();
                        }
                );
    }


    String getFileName(){
        String file_name="";
//        import android.app.DownloadManager;
//import android.content.Context;
//import android.net.Uri;
//import android.os.Environment;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity; // Assuming this is in an Activity or similar class
//
//import java.io.IOException;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//        public class DownloadActivity extends AppCompatActivity { // or your activity name
//
//            private long downloadID;
//            private final String download_link = "http://10.0.2.2:8000/download/";
//
//            public void startDownload() {
//                // Step 1: Make a HEAD request to get headers (Content-Disposition)
//                getFilenameAndEnqueueDownload(download_link);
//            }
//
//            private void getFilenameAndEnqueueDownload(String url) {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder()
//                        .url(url)
//                        // Use HEAD method to get headers without downloading the file body
//                        .head()
//                        .build();
//
//                client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        // Handle network failure or error
//                        runOnUiThread(() -> {
//                            Toast.makeText(getApplicationContext(), "Failed to get file info: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                        });
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        if (!response.isSuccessful()) {
//                            runOnUiThread(() -> {
//                                Toast.makeText(getApplicationContext(), "Server error when getting file info: " + response.code(), Toast.LENGTH_LONG).show();
//                            });
//                            return;
//                        }
//
//                        // Get the Content-Disposition header value
//                        String contentDisposition = response.header("Content-Disposition");
//                        String suggestedFilename = extractFilenameFromContentDisposition(contentDisposition);
//
//                        // Fallback if header is missing or parsing fails
//                        if (suggestedFilename == null || suggestedFilename.isEmpty()) {
//                            suggestedFilename = "downloaded_file_" + System.currentTimeMillis() + ".dat";
//                        }
//
//                        // Step 2: Enqueue the DownloadManager request with the obtained filename
//                        enqueueDownload(url, suggestedFilename);
//                    }
//                });
//            }
//
//            // --- Utility Method to Parse Header ---
//            private String extractFilenameFromContentDisposition(String header) {
//                if (header == null) return null;
//
//                // This simple regex works for headers like: attachment; filename="pic.png"
//                // And is more robust than simple string splitting.
//                String filename = null;
//                Pattern pattern = Pattern.compile("filename=\"?([^\"\\n;]+)\"?;?", Pattern.CASE_INSENSITIVE);
//                Matcher matcher = pattern.matcher(header);
//
//                if (matcher.find()) {
//                    // Group 1 is the content inside the quotes (or after filename=)
//                    filename = matcher.group(1).replaceAll("^\"|\"$", "");
//                }
//                return filename;
//            }
//
//            // --- Step 3: Enqueue the DownloadManager Request ---
//            private void enqueueDownload(String url, String filename) {
//                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//                if (downloadManager == null) {
//                    runOnUiThread(() -> {
//                        Toast.makeText(getApplicationContext(), "Error: Download Manager service not available.", Toast.LENGTH_LONG).show();
//                    });
//                    return;
//                }
//
//                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//
//                request.setTitle(filename); // Use the extracted filename for the notification title
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//
//                // Use the extracted filename here. This is the crucial change.
//                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
//
//                request.setAllowedOverMetered(true);
//                request.setAllowedOverRoaming(true);
//
//                downloadID = downloadManager.enqueue(request);
//
//                runOnUiThread(() -> {
//                    Toast.makeText(getApplicationContext(), "Download started for: " + filename, Toast.LENGTH_SHORT).show();
//                });
//            }
//        }

        return file_name;
    }
    void start_download(){
        String download_link = "http://10.0.2.2:8000/download/";


        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(getApplicationContext(), "Error: Download Manager service not available.", Toast.LENGTH_LONG).show();
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download_link));

            request.setTitle("Downloading File");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, null);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            downloadID = downloadManager.enqueue(request);

            Toast.makeText(getApplicationContext(), "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("DownloadError", "Error in start_download", e);
            Toast.makeText(getApplicationContext(), "Failed to start download: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadID) {
//                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//                DownloadManager.Query query = new DownloadManager.Query();
//                query.setFilterById(id);
//                try (Cursor cursor = downloadManager.query(query)) {
//                    if (cursor.moveToFirst()) {
//                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
//                        if (statusIndex != -1) {
//                            int status = cursor.getInt(statusIndex);
//                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                                Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_SHORT).show();
//                            } else {
//                                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
//                                int reason = -1;
//                                if (reasonIndex != -1) {
//                                    reason = cursor.getInt(reasonIndex);
//                                }
//                                Toast.makeText(getApplicationContext(), "Download failed. Reason: " + reason, Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    }
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Cursor error", Toast.LENGTH_SHORT).show();
//                }
                Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        Toast.makeText(getApplicationContext(), "Download canceled", Toast.LENGTH_SHORT).show();
    }
}

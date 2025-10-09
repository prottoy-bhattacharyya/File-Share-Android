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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class receiveActivity extends AppCompatActivity {

    Button scan_button, go_button;
    long downloadID;
    TextView title_text;
    // Use final constants for the base links
    final String DOWNLOAD_LINK_BASE = "http://192.168.1.138:8000/download?unique_text=";
    final String FILE_COUNT_LINK_BASE = "http://192.168.1.138:8000/get_file_count?unique_text=";

    EditText type_text;
    String unique_text_value = ""; // Store the unique text globally
    boolean isScanned = false;


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
        title_text = findViewById(R.id.title_text);
        type_text = findViewById(R.id.type_text);

        scan_button.setOnClickListener(view -> qr_scanner());

        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get text from EditText for validation and to store globally
                String text = type_text.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please scan QR code or type identifier", Toast.LENGTH_SHORT).show();
                    return;
                }
                unique_text_value = text;
                start_download_process();
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
                            isScanned = true;
                            unique_text_value = barcode.getRawValue(); // Store value globally
                            type_text.setText(unique_text_value);
                            start_download_process();
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

   void start_download_process(){
       if(unique_text_value.isEmpty()){
           // Should not happen if logic in go_button is correct, but safe check
           unique_text_value = type_text.getText().toString().trim();
           if (unique_text_value.isEmpty()) {
               Toast.makeText(getApplicationContext(), "Pleasse Scan QR code or type identifier", Toast.LENGTH_SHORT).show();
               return;
           }
       }

       // --- CORRECTED: Build the URL locally using the base constant and the unique_text_value ---
       String file_count_url = FILE_COUNT_LINK_BASE + unique_text_value;

       OkHttpClient client = new OkHttpClient();
       Request request = new Request.Builder()
               .url(file_count_url) // Use the locally built URL
               .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to get file count: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                int count = 0;
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    count = jsonObject.getInt("file_count");
                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Failed to parse file count JSON.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                final int finalCount = count;
                runOnUiThread(()->{
                    next_download_process(finalCount);
                });
            }
        });

    }


    public void next_download_process(int file_count) {
        if (file_count == 0) {
            Toast.makeText(getApplicationContext(), "No files found", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < file_count; i++) {
            // Build the URL for the current file index
            String current_download_url = DOWNLOAD_LINK_BASE + unique_text_value + "&file_index=" + i;

            // Start the process for this file
            getFilenameAndEnqueueDownload(current_download_url);

        }
    }

    private void getFilenameAndEnqueueDownload(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to get file info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String contentDisposition = response.header("Content-Disposition");

                String suggestedFilename = extractFilenameFromContentDisposition(contentDisposition);

                if (suggestedFilename == null || suggestedFilename.isEmpty()) {
                    suggestedFilename = "downloaded_file_" + System.currentTimeMillis() + ".dat";
                    // Still show an error if the header was explicitly missing
                    if (contentDisposition == null) {
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), "Warning: Content-Disposition header not found. Using default filename.", Toast.LENGTH_LONG).show();
                        });
                    }
                }

                String finalFilename = suggestedFilename;
                runOnUiThread(() -> {
                    enqueueDownload(url, finalFilename);
                });
            }
        });
    }


    private String extractFilenameFromContentDisposition(String header) {
        if (header == null) return null;


        String filename = null;
        Pattern pattern = Pattern.compile("filename=\"?([^\"\\n;]+)\"?;?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(header);

        if (matcher.find()) {
            filename = matcher.group(1).replaceAll("^\"|\"$", "");
        }
        return filename;
    }


    private void enqueueDownload(String url, String filename) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(getApplicationContext(), "Error: Download Manager service not available.", Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(getApplicationContext(), "File name: " + filename, Toast.LENGTH_LONG).show();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setTitle(filename);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            downloadID = downloadManager.enqueue(request);

            Toast.makeText(getApplicationContext(), "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("DownloadError", "Error in enqueueDownload", e);
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

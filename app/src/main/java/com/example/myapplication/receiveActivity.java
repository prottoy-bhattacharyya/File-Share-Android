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
    TextView title;
    String download_link = "";
    EditText type_text;

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
        type_text = findViewById(R.id.type_text);

        scan_button.setOnClickListener(view -> qr_scanner());

        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download_link = type_text.getText().toString();
                if (download_link.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please scan QR code or type link", Toast.LENGTH_SHORT).show();
                    return;
                }
                start_download_process();
                finish();
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
                            download_link = barcode.getRawValue();
                            type_text.setText(download_link);
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


    public void start_download_process() {
        getFilenameAndEnqueueDownload(download_link);
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

package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class sendActivity extends AppCompatActivity {
    Button select_files_button, send_button;
    TextView file_count_text;
    LinearLayout fileListContainer;
    CardView file_list_card;
    private ArrayList<Uri> selectedFileUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send);

        select_files_button = findViewById(R.id.select_files_button);
        send_button = findViewById(R.id.send_button);
        file_list_card = findViewById(R.id.file_list_card);
        file_count_text = findViewById(R.id.file_count_text);
        fileListContainer = findViewById(R.id.file_list_container);

        select_files_button.setOnClickListener(view -> selectFiles());

        send_button.setOnClickListener(view -> {
            if(selectedFileUris.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please select files", Toast.LENGTH_SHORT).show();
            }
            else{
                sendFiles();
            }
        });
    }


    private void sendFiles() {
        Intent intent = new Intent(sendActivity.this, qrActivity.class);
        intent.putExtra("selectedFileUris", selectedFileUris);
        startActivity(intent);
    }

    private void selectFiles(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select files"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {

            Uri uri;
            file_list_card.setVisibility(View.VISIBLE);
            fileListContainer.removeAllViews();
            selectedFileUris.clear();

            if (data.getClipData() == null) {
                uri = data.getData();
                file_count_text.setText("1 File selected");
                showFiles(uri);
            }

            else {
                int fileCount = data.getClipData().getItemCount();
                file_count_text.setText(fileCount + " Files selected");

                for (int i = 0; i < fileCount; i++) {
                    uri = data.getClipData().getItemAt(i).getUri();
                    showFiles(uri);
                }
            }

        }
    }

    private void showFiles(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        DecimalFormat df = new DecimalFormat("0.00");

        selectedFileUris.add(uri);

        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            int nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeColumnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            if (nameColumnIndex >= 0 && sizeColumnIndex >= 0) {
                String fileName = cursor.getString(nameColumnIndex);
                long fileSize = cursor.getLong(sizeColumnIndex);


                ImageView imageView = new ImageView(this);
                imageView.setPadding(8, 8, 8, 8);
                imageView.setClickable(true);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(300, 300);
                imageView.setLayoutParams(imageParams);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        Size thumbnailSize = new Size(150, 150);
                        Bitmap thumbnailBitmap = getContentResolver().loadThumbnail(uri, thumbnailSize, null);
                        imageView.setImageBitmap(thumbnailBitmap);
                    } catch (IOException e) {

                        if(fileName.endsWith(".pdf")){
                            imageView.setImageResource(R.drawable.pdf);
                        }
                        else if(fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt")){
                            imageView.setImageResource(R.drawable.docs);
                        }
                        else if(fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".flac")){
                            imageView.setImageResource(R.drawable.audio);
                        }
                        else{
                            imageView.setImageResource(android.R.drawable.ic_menu_help);
                        }
                        e.printStackTrace();
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground);
                }


                TextView fileInfoText = new TextView(this);
                String formattedSize;

                if (fileSize < 1024) formattedSize = df.format(fileSize) + " B";
                else if (fileSize < 1024 * 1024) formattedSize = df.format(fileSize / 1024.0) + " KB";
                else formattedSize = df.format(fileSize / (1024.0 * 1024.0)) + " MB";

                fileInfoText.setText(fileName + "\n" + formattedSize);
                fileInfoText.setTextColor(Color.BLACK);

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                fileInfoText.setLayoutParams(textParams);
                fileInfoText.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.CENTER_HORIZONTAL);


                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setPadding(16, 16, 16, 16);


                itemLayout.addView(imageView);
                itemLayout.addView(fileInfoText);

                fileListContainer.addView(itemLayout);
            }
            cursor.close();
        }
    }
}
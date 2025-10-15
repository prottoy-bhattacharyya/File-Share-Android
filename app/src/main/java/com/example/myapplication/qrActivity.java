package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class qrActivity extends AppCompatActivity {

    ImageView qrImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr);

        qrImage = findViewById(R.id.qr_image);

        String qrText = "ABCD-EFGH";
        generateQR(qrText);
    }

    private void generateQR(String qrText){
        QRGEncoder qrgEncoder = new QRGEncoder(qrText, null, QRGContents.Type.TEXT, 400);
        qrgEncoder.setColorBlack(Color.BLACK);
        qrgEncoder.setColorWhite(Color.WHITE);
        try {
            Bitmap bitmap = qrgEncoder.getBitmap(0);
            qrImage.setImageBitmap(bitmap);
        } catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
    }


}
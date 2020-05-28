package com.example.testing;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CAMERA_CODE = 1000;
    private static final int PERMISSION_STORAGE_CODE = 1002;
    private static final int IMAGE_CONTENT_CODE = 1001;

    private Uri image_uri;

    private Button mCaptureBtn, mScanButton;
    private ImageView mImageView;
    private ImageButton mFileButton;

    private Intent myFileIntent;
    private  Bitmap imageBitmap;

    public static String sourceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        mCaptureBtn = findViewById(R.id.capture_image_button);
        mFileButton = findViewById(R.id.fileButton);
        mScanButton = findViewById(R.id.scanButton);

        if (imageBitmap != null) {
            mImageView.setImageBitmap(imageBitmap);
        }

        //Gallery button
        ImageButton mGalBtn = (ImageButton) findViewById(R.id.galButton); // Replace with id of your button.
        mGalBtn.setOnClickListener( new View.OnClickListener() {

            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ) {
                        //permission not enable, request
                        String[] permission = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        //show popout to request
                        requestPermissions(permission, PERMISSION_STORAGE_CODE);

                    } else {
                        //permission already granted
                        openStorageGallery();
                    }

                } else {
                    //system os < marshmallow
                }
            }
        });

        mFileButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                myFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                myFileIntent.setType("*/*");
                startActivityForResult(myFileIntent,10);
            }
        });

        mScanButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                if (imageBitmap != null) {
                    recognizeTxt();

                } else {
                    Toast.makeText(MainActivity.this, "No image found...", Toast.LENGTH_SHORT).show();

                }
            }
        });

        //button click for camera
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if system version >= mashmallow, request runtime request
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ) {
                        //permission not enable, request
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                        //show popout to request
                        requestPermissions(permission, PERMISSION_CAMERA_CODE);

                    } else {
                        //permission already granted
                        dispatchTakePictureIntent();
                    }

                } else {
                    //system os < marshmallow
                }
            }
        });
    }

    private void openStorageGallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        final int ACTIVITY_SELECT_IMAGE = 1234;
        startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
    }

    //Prev known as openCamera
    private void dispatchTakePictureIntent() {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //Camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CONTENT_CODE);
    }

    //handling permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int grantResults[]) {
        //this method is called, when user press allow or deny the Permission Rquest popout
        switch (requestCode) {
            case PERMISSION_CAMERA_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    //permission from pop out was denied
                    Toast.makeText(MainActivity.this, "Permissions denied, please accept manually in app settings", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PERMISSION_STORAGE_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openStorageGallery();
                } else {
                    Toast.makeText(MainActivity.this, "Permissions denied, please accept manually in app settings", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data ){
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 1234:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();

                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    mImageView.setImageResource(android.R.color.transparent);

                    mImageView.setImageURI(selectedImage);
                    imageBitmap = BitmapFactory.decodeFile(filePath);
                    mImageView.setImageBitmap(imageBitmap);

                    /* Now you have choosen image in Bitmap format in object "yourSelectedImage". You can use it in way you want! */
                    break;
                }
            case IMAGE_CONTENT_CODE:
                if(resultCode == RESULT_OK){
                    //set the image capture to ImageView

                    mImageView.setImageURI(image_uri);
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                }
        }
    }


    //Detect the text in image
    private void recognizeTxt () {
        //Convert image bitmap to firebase vision iamge format
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        //Declare a recognizer on device
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        //Process the image
        recognizer.processImage(image)
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //If fail print out the fail message
                    Toast.makeText( MainActivity.this, "Error : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            })
            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                @Override
                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                    //If success go to processTxt method
                    processTxt(firebaseVisionText);
                }
            });
    }

    //Process the text from image to blocks
    private void processTxt(FirebaseVisionText text) {
        //Get text as blocks
        List<FirebaseVisionText.TextBlock> blocks = text.getTextBlocks();
        //If there is no block and return to the app
        if (blocks.size() == 0)
        {
            //Make a toast message to let the user know
            Toast.makeText(this, "No text acquired in this photo", Toast.LENGTH_LONG).show();
            return;
        }
        //If block(s) exists, then add to the edit text component
        StringBuilder txt = new StringBuilder();
        for (FirebaseVisionText.TextBlock block : text.getTextBlocks()) {
            //For each of the blocks, append to the string builder
            txt.append(block.getText() + "\n");
        }

        //Then after finish the looping, set the text to source Text.
        sourceText = txt.toString();

        //Make sure the text is not empty before going to the second page
        if (!sourceText.isEmpty()) {
            //Go to second page
            Intent intent = new Intent(MainActivity.this, OutputActivity.class);
            startActivity(intent);
        }
    }



    }

package com.example.testing;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.example.testing.MainActivity.sourceText;

public class OutputActivity extends AppCompatActivity {

    private static String FILE_NAME = "example1.txt";

    //Output Activity
    private EditText editTextOriginal;
    private EditText editTextTranslate;
    private TextView txtVOriginal;
    private TextView txtVTranslate;
    private Button btnSaveOutput;
    private Button btnTranslateOutput;


    private String originalText;
    private int selectedLangCode;

    //Translate Selection Popup
    private Dialog translateDialog;
    private Button btnCancelTranslate;
    private Button btnOKTranslate;
    private Spinner spinnerLanguages;
    private TextView textVSelectLang;

    //Save Popup
    private Dialog saveDialog;
    private EditText editTxtFilename;
    private Button btnOKFile;
    private Button btnCancelSaveFile;
    private TextView txtVSaveFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output);
        translateDialog = new Dialog(OutputActivity.this);
        saveDialog = new Dialog(OutputActivity.this);

        editTextOriginal = findViewById(R.id.editTextOriginal);

        editTextOriginal.setText(sourceText);

        editTextTranslate = findViewById(R.id.editTextTranslate);
        txtVOriginal = findViewById(R.id.txtVOriginal);
        txtVTranslate = findViewById(R.id.txtVTranslate);
        btnSaveOutput = findViewById(R.id.btnSaveOutput);
        btnTranslateOutput = findViewById(R.id.btnTranslateOutput);

        btnSaveOutput.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                ShowSavePopup(view);
            }
        });

        btnTranslateOutput.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                ShowTranslatePopup(view);
            }
        });

    }


    //Show save pop up
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void ShowSavePopup(View v) {
        saveDialog.setContentView(R.layout.savepopup);

        btnOKFile = saveDialog.findViewById(R.id.btnOKSaveFile);
        btnCancelSaveFile = saveDialog.findViewById(R.id.btnCancelSaveFile);

        editTxtFilename = saveDialog.findViewById(R.id.editTxtFilename);
        txtVSaveFile = saveDialog.findViewById(R.id.txtVSaveFile);

        btnOKFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editTextTranslate.getText().toString().isEmpty())
                    saveTxtFile(editTextTranslate.getText().toString());
                else
                    saveTxtFile(editTextOriginal.getText().toString());

                saveDialog.dismiss();
            }
        });

        btnCancelSaveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveDialog.dismiss();
            }
        });

        Objects.requireNonNull(saveDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        saveDialog.show();

    }

    //Show translate pop up
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void ShowTranslatePopup(View v) {
        translateDialog.setContentView(R.layout.translatepopout);

        btnCancelTranslate = translateDialog.findViewById(R.id.btnCancelTranslate);
        btnOKTranslate = translateDialog.findViewById(R.id.btnOKTranslate);

        txtVTranslate = translateDialog.findViewById(R.id.txtVTranslate);

        spinnerLanguages = translateDialog.findViewById(R.id.spinnerLanguages);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages, android.R.layout.simple_spinner_item); //Put string arrays to this adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguages.setAdapter(adapter); //set adapter to the spinner
        spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = parent.getItemAtPosition(position).toString();

                System.out.println("SELECTED LANGUAGE : "+ selectedLang);

                switch (selectedLang){
                    case "Tamil":
                        selectedLangCode = FirebaseTranslateLanguage.TA;
                        break;
                    case "English":
                        selectedLangCode = FirebaseTranslateLanguage.EN;
                        break;
                    case "Malay":
                        selectedLangCode = FirebaseTranslateLanguage.MS;
                        break;
                    case "Chinese":
                        selectedLangCode = FirebaseTranslateLanguage.ZH;
                        break;
                    default:
                        selectedLangCode = 0;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnCancelTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                translateDialog.dismiss();
            }
        });

        btnOKTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                identifyLanguage();
            }
        });


        Objects.requireNonNull(translateDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        translateDialog.show();

    }

    private void identifyLanguage() {
        //Get text from edit text componenet
        originalText = editTextOriginal.getText().toString();
        //Create an language identifier
        FirebaseLanguageIdentification identifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        //Identify the text
        identifier.identifyLanguage(originalText).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                //If no lang could detected, undetermined is passed
                if (s.equals("und")){
                    Toast.makeText(getApplicationContext(),"Language Not Supported",Toast.LENGTH_SHORT).show();
                }
                else {
                    //If success, go to getLanguageCode()
                    getLanguageCode(s);
                }
            }
        });
    }

    private void getLanguageCode(String language) {
        int langCode;
        switch (language){
            case "ta":
                langCode = FirebaseTranslateLanguage.TA;
                System.out.println("Tamil");
                break;
            case "en":
                langCode = FirebaseTranslateLanguage.EN;
                System.out.println("English");

                break;
            case "ms":
                langCode = FirebaseTranslateLanguage.MS;
                System.out.println("Malay");

                break;
            case "zh":
                langCode = FirebaseTranslateLanguage.ZH;
                System.out.println("Chinese");

                break;
            default:
                langCode = 0;
        }
        //Check language same or not
        System.out.println("Checking..");
        //If not same, go translate
        if (langCode != selectedLangCode)
            translateText(langCode);
        else
            Toast.makeText(getApplicationContext(),"They are the same languages! " +
                    "Please select another language.",Toast.LENGTH_SHORT).show();
    }

    private void translateText(int langCode) {
        System.out.println("Translating..");
        //build a firebase translator options
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                //from language
                .setSourceLanguage(langCode)
                // to language
                .setTargetLanguage(selectedLangCode)
                .build();

        final FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        //Download model if the lang model not exist on device
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                translator.translate(originalText).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        editTextTranslate.setText(s);
                                        translateDialog.dismiss();
                                    }
                                });
                            }
                        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"Fail to download language model to device, " +
                                "please uninstall it and restart your phone",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveTxtFile(String contentTxt) {
        //Set the file name from user input
        FILE_NAME = editTxtFilename.getText().toString();

        //Create folder path
        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "PicToTextFile");

        boolean success = true;

        //If folder not exists, create the folder
        if (!folder.exists()) {
            //Create Directory Folder
            success = folder.mkdir();
        }

        if (success){
            //Create the text file
            File textFile = new File(folder, FILE_NAME + ".txt");

            try {
                //Write the content into the text file
                FileOutputStream fos = new FileOutputStream(textFile);
                fos.write(contentTxt.getBytes());
                fos.close();
                //Show the file path to the user
                Toast.makeText(this,"File Saved : " + textFile.toString(), Toast.LENGTH_LONG).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this,"File Cant be saved due to folder PicToTextFile not existed", Toast.LENGTH_LONG).show();
        }

    }

}

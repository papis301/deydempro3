package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import deydemv3.LoginActivityc;

public class DriverDocumentsActivityV3
        extends AppCompatActivity
        implements DriverDocumentsAdapter.OnDocumentClickListener {

    private RecyclerView recyclerView;

    private DriverDocumentsAdapter adapter;

    private List<DriverDocument> documentList;

    private DriverDocument currentDocument;

    private boolean isRecto = true;

    private Bitmap rectoBitmap;

    private Bitmap versoBitmap;
    private String userId;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_driver_documents_v3
        );

        SharedPreferences sp =
                getSharedPreferences("DeydemUser", MODE_PRIVATE);

        userId =
                sp.getString("user_id", "0");
        boolean savedOnline =
                sp.getBoolean(
                        "driver_online",
                        false
                );

        if (userId == null || userId.isEmpty() || userId.equals("0")) {

            Toast.makeText(this,
                    "Veuillez vous connecter",
                    Toast.LENGTH_LONG).show();

            Intent intent =
                    new Intent(
                            DriverDocumentsActivityV3.this,
                            LoginActivityc.class
                    );

            startActivity(intent);

            finish();

            return;
        }

        recyclerView =
                findViewById(
                        R.id.recyclerDocuments
                );

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        documentList = new ArrayList<>();

        loadDocuments(); // Ajoute les documents par défaut

        adapter = new DriverDocumentsAdapter(
                documentList,
                this
        );

        recyclerView.setAdapter(adapter);

        loadDocumentsFromServer();

        cameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if(result.getResultCode() == RESULT_OK
                                    && result.getData() != null){

                                Bundle extras =
                                        result.getData().getExtras();

                                if(extras == null){
                                    return;
                                }

                                Bitmap bitmap =
                                        (Bitmap) extras.get("data");

                                if(bitmap == null){
                                    return;
                                }

                                if(isRecto){
                                    if(currentDocument.getRectoBitmap() != null){

                                        currentDocument
                                                .getRectoBitmap()
                                                .recycle();
                                    }

                                    currentDocument.setRectoBitmap(bitmap);

                                    adapter.notifyDataSetChanged();

                                    Toast.makeText(
                                            this,
                                            "Recto capturé",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                }else{

                                    if(currentDocument.getVersoBitmap() != null){

                                        currentDocument
                                                .getVersoBitmap()
                                                .recycle();
                                    }
                                    currentDocument.setVersoBitmap(bitmap);

                                    adapter.notifyDataSetChanged();

                                    Toast.makeText(
                                            this,
                                            "Verso capturé",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                );
    }

    private void loadDocuments(){

        documentList.add(
                new DriverDocument(
                        1,
                        "Photo de profil",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        2,
                        "Carte d'identité",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        3,
                        "Permis de conduire",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        4,
                        "Carte grise",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        5,
                        "Assurance",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        6,
                        "Visite technique",
                        "",
                        "",
                        "pending",
                        ""
                )
        );

        documentList.add(
                new DriverDocument(
                        7,
                        "Photo du véhicule",
                        "",
                        "",
                        "pending",
                        ""
                )
        );
    }

    @Override
    public void onTakeRecto(
            DriverDocument document
    ) {

        currentDocument = document;

        isRecto = true;

        openCamera();
    }

    @Override
    public void onTakeVerso(
            DriverDocument document
    ) {

        currentDocument = document;

        isRecto = false;

        openCamera();
    }

    @Override
    public void onUploadDocument(
            DriverDocument document
    ) {

        Toast.makeText(
                this,
                "Upload : "
                        + document.getType(),
                Toast.LENGTH_SHORT
        ).show();


        uploadDocumentToServer(document);
    }

    @Override
    public void onUpdateDocument(
            DriverDocument document
    ) {

        Toast.makeText(
                this,
                "Mise à jour : "
                        + document.getType(),
                Toast.LENGTH_SHORT
        ).show();

        uploadDocumentToServer(document);
    }

    private void openCamera(){

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    100
            );

            return;
        }

        Intent intent =
                new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );

        cameraLauncher.launch(intent);
    }

    private String bitmapToBase64(Bitmap bitmap){

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                80,
                baos
        );

        byte[] imageBytes =
                baos.toByteArray();

        return Base64.encodeToString(
                imageBytes,
                Base64.DEFAULT
        );
    }

    private void uploadDocumentToServer(
            DriverDocument document
    ){

        AlertDialog loadingDialog =
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .create();

        View view =
                getLayoutInflater().inflate(
                        R.layout.dialog_loading,
                        null
                );

        loadingDialog.setView(view);

        loadingDialog.show();

        String rectoBase64 =
                bitmapToBase64(
                        document.getRectoBitmap()
                );

        String versoBase64;

        if(document.getVersoBitmap() != null){

            versoBase64 =
                    bitmapToBase64(
                            document.getVersoBitmap()
                    );
        } else {
            versoBase64 = "";
        }

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,

                        BASE_URL +
                                "upload_driver_document.php",

                        response -> {

                            loadingDialog.dismiss();

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response
                                        );

                                boolean success =
                                        json.getBoolean(
                                                "success"
                                        );

                                String message =
                                        json.getString(
                                                "message"
                                        );

                                if(success){
                                    if(document.getRectoBitmap() != null){

                                        document.getRectoBitmap().recycle();

                                        document.setRectoBitmap(null);
                                    }

                                    if(document.getVersoBitmap() != null){

                                        document.getVersoBitmap().recycle();

                                        document.setVersoBitmap(null);
                                    }

                                    adapter.notifyDataSetChanged();

                                    System.gc();

                                    document.setStatus(
                                            "pending"
                                    );

                                    adapter.notifyDataSetChanged();

                                    Toast.makeText(
                                            this,
                                            "✅ " + message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                    finish();
                                }else{

                                    Toast.makeText(
                                            this,
                                            "❌ " + message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            }catch(Exception e){

                                Toast.makeText(
                                        this,
                                        "Erreur serveur",
                                        Toast.LENGTH_LONG
                                ).show();

                                e.printStackTrace();
                            }

                        },

                        error -> {

                            loadingDialog.dismiss();

                            Toast.makeText(
                                    this,
                                    "❌ Upload échoué",
                                    Toast.LENGTH_LONG
                            ).show();

                        }

                ){

                    @Override
                    protected Map<String,String>
                    getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "driver_id",
                                userId
                        );

                        params.put(
                                "document_type",
                                document.getType()
                        );

                        params.put(
                                "recto",
                                rectoBase64
                        );

                        params.put(
                                "verso",
                                versoBase64
                        );

                        return params;
                    }
                };

        request.setRetryPolicy(

                new DefaultRetryPolicy(

                        30000,

                        2,

                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT

                )
        );

        Volley.newRequestQueue(this)
                .add(request);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        for(DriverDocument doc : documentList){

            if(doc.getRectoBitmap() != null){

                doc.getRectoBitmap().recycle();
            }

            if(doc.getVersoBitmap() != null){

                doc.getVersoBitmap().recycle();
            }
        }

        System.gc();
    }

    private void loadDocumentsFromServer() {

        String url = BASE_URL +
                "get_driver_documents.php?driver_id=" + userId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,

                response -> {
                    Log.d("DOC_API", response);
                    try {

                        JSONObject json =
                                new JSONObject(response);

                        if(json.getBoolean("success")) {

                            JSONArray docs =
                                    json.getJSONArray("documents");

                           // documentList.clear();

                            for(int i = 0; i < docs.length(); i++) {

                                JSONObject obj = docs.getJSONObject(i);

                                String type = obj.getString("type");

                                for(DriverDocument document : documentList){

                                    if(document.getType().equalsIgnoreCase(type)){

                                        document.setRectoUrl(
                                                obj.getString("recto_url")
                                        );

                                        document.setVersoUrl(
                                                obj.getString("verso_url")
                                        );

                                        document.setStatus(
                                                obj.getString("status")
                                        );

                                        document.setReason(
                                                obj.getString("reason")
                                        );

                                        break;
                                    }
                                }
                            }

                            adapter.notifyDataSetChanged();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Toast.makeText(
                        this,
                        "Erreur chargement documents",
                        Toast.LENGTH_LONG
                ).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

}
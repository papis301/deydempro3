package com.pisco.deydempro3;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int PERMISSION_IMAGE = 200;

    private Uri cameraUri;
    private ImageView currentImage;
    private DocType currentType;
    private ProgressDialog dialog;

    private int driverId;

    private Map<DocType, Uri> imageUris = new HashMap<>();

    private Map<String, ImageView> imageMap = new HashMap<>();

    Button btnUploadId, btnUploadPermit, btnUploadCg, btnUploadVeh;

    enum DocType {
        PROFILE("profile_photo"),
        ID_FRONT("id_card_front"),
        ID_BACK("id_card_back"),
        PERMIT_FRONT("permit_front"),
        PERMIT_BACK("permit_back"),
        CG_FRONT("vehicle_card_front"),
        CG_BACK("vehicle_card_back"),
        VEH1("vehicle_photo_1"),
        VEH2("vehicle_photo_2"),
        VEH3("vehicle_photo_3"),
        VEH4("vehicle_photo_4");

        public final String value;
        DocType(String v){ value=v; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        driverId = getSharedPreferences("user", MODE_PRIVATE).getInt("driver_id",0);

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        initViews();
        bindClicks();
        loadDriverDocuments();
    }

    private void initViews(){

        imageMap.put("profile_photo", findViewById(R.id.imgProfile));
        imageMap.put("id_card_front", findViewById(R.id.imgIdFront));
        imageMap.put("id_card_back", findViewById(R.id.imgIdBack));
        imageMap.put("permit_front", findViewById(R.id.imgPermitFront));
        imageMap.put("permit_back", findViewById(R.id.imgPermitBack));
        imageMap.put("vehicle_card_front", findViewById(R.id.imgCgFront));
        imageMap.put("vehicle_card_back", findViewById(R.id.imgCgBack));
        imageMap.put("vehicle_photo_1", findViewById(R.id.imgVeh1));
        imageMap.put("vehicle_photo_2", findViewById(R.id.imgVeh2));
        imageMap.put("vehicle_photo_3", findViewById(R.id.imgVeh3));
        imageMap.put("vehicle_photo_4", findViewById(R.id.imgVeh4));

        btnUploadId = findViewById(R.id.btnUploadId);
        btnUploadPermit = findViewById(R.id.btnUploadPermit);
        btnUploadCg = findViewById(R.id.btnUploadCg);
        btnUploadVeh = findViewById(R.id.btnUploadVeh);
    }

    private void bindClicks(){

        for(Map.Entry<String, ImageView> entry : imageMap.entrySet()){
            entry.getValue().setOnClickListener(v -> {
                currentImage = entry.getValue();
                currentType = getDocType(entry.getKey());
                chooseImage();
            });
        }

        btnUploadId.setOnClickListener(v -> {
            uploadDocument(DocType.ID_FRONT);
            uploadDocument(DocType.ID_BACK);
        });

        btnUploadPermit.setOnClickListener(v -> {
            uploadDocument(DocType.PERMIT_FRONT);
            uploadDocument(DocType.PERMIT_BACK);
        });

        btnUploadCg.setOnClickListener(v -> {
            uploadDocument(DocType.CG_FRONT);
            uploadDocument(DocType.CG_BACK);
        });

        btnUploadVeh.setOnClickListener(v -> {
            uploadDocument(DocType.VEH1);
            uploadDocument(DocType.VEH2);
            uploadDocument(DocType.VEH3);
            uploadDocument(DocType.VEH4);
        });
    }

    private DocType getDocType(String value){
        for(DocType t : DocType.values()){
            if(t.value.equals(value)) return t;
        }
        return null;
    }

    private void chooseImage(){
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Caméra","Galerie"},(d,i)->{
                    if(i==0) openCamera();
                    else openGallery();
                }).show();
    }

    private void openCamera(){

        if(!checkPermission()) return;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"doc_"+System.currentTimeMillis());
        cameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
        startActivityForResult(i,CAMERA_REQUEST);
    }

    private void openGallery(){
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i,GALLERY_REQUEST);
    }

    private boolean checkPermission(){

        String perm = Build.VERSION.SDK_INT >=33 ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if(ContextCompat.checkSelfPermission(this,perm)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{perm},PERMISSION_IMAGE);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode!=RESULT_OK) return;

        Uri uri = requestCode==CAMERA_REQUEST ? cameraUri :
                data!=null ? data.getData() : null;

        if(uri==null) return;

        currentImage.setImageURI(uri);
        imageUris.put(currentType,uri);
    }

    /* ================= UPLOAD ================= */

    private void uploadDocument(DocType type){

        Uri uri = imageUris.get(type);
        if(uri==null){
            Toast.makeText(this,"Sélectionnez image "+type.value,Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.setMessage("Upload...");
        dialog.show();

        VolleySingleton req = new VolleySingleton(
                Request.Method.POST,
                Constants.BASE_URL+"upload_driver_documents.php",
                response -> {
                    dialog.dismiss();
                    Toast.makeText(this,"Upload réussi "+type.value,Toast.LENGTH_SHORT).show();

                },
                error -> {
                    dialog.dismiss();
                    Toast.makeText(this,"Erreur upload",Toast.LENGTH_SHORT).show();
                }
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> p = new HashMap<>();
                p.put("driver_id",String.valueOf(driverId));
                p.put("document_type",type.value);
                return p;
            }

            @Override
            public Map<String,DataPart> getByteData(){

                Map<String,DataPart> map = new HashMap<>();

                try(InputStream is=getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream buffer=new ByteArrayOutputStream()){

                    byte[] data=new byte[4096];
                    int n;
                    while((n=is.read(data))!=-1) buffer.write(data,0,n);

                    map.put("file", new DataPart(
                            type.value+"_"+System.currentTimeMillis()+".jpg",
                            buffer.toByteArray(),
                            "image/jpeg"
                    ));

                }catch(Exception e){ e.printStackTrace(); }

                return map;
            }
        };

        VolleySingleton.getInstance(this).add(req);
    }

    /* ================= LOAD ================= */

    private void loadDriverDocuments(){

        StringRequest req = new StringRequest(Request.Method.GET,
                Constants.BASE_URL+"get_driver_documents.php?driver_id="+driverId,
                response -> {
                    try{
                        JSONObject json = new JSONObject(response);
                        if(!json.getBoolean("success")) return;

                        JSONArray docs = json.getJSONArray("documents");

                        for(int i=0;i<docs.length();i++){

                            JSONObject d = docs.getJSONObject(i);
                            String type = d.getString("document_type");
                            String path = d.getString("file_path");
                            Log.d("DOC_FULL", type+" -> "+path);
                            ImageView img = imageMap.get(type);
                            if(img!=null){

                                Glide.with(this)
                                        .load(Constants.BASE_URL+"uploads/driver_documents/"+path)
                                        .placeholder(R.drawable.ic_upload)
                                        .error(R.drawable.ic_error)
                                        .into(img);
                            }
                        }

                    }catch(Exception e){ e.printStackTrace(); }

                },error -> {}
        );

        VolleySingleton.getInstance(this).add(req);
    }
}
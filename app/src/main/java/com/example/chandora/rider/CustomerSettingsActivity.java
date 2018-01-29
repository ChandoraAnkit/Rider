package com.example.chandora.rider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerSettingsActivity extends AppCompatActivity {
    String userId;
    String mName;
    String mPhone;
    String mProfileImageUrl;

    FirebaseAuth mAuth;
    DatabaseReference mCustomerDatabase;
    private Button mConfirm;

    private EditText mNameField, mPhoneField;
    private CircleImageView mProfileImage;
    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);

        mConfirm = findViewById(R.id.comfirm);


        mProfileImage =(CircleImageView) findViewById(R.id.profile_image);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        getUserInformation();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInformation();

            }
        });


        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent imageIntent =new Intent(Intent.ACTION_PICK);
                imageIntent.setType("image/*");
                startActivityForResult(imageIntent,1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();
            mProfileImage.setImageURI(imageUri);
        }
    }

    private void saveUserInformation() {
        mName = mNameField.getText().toString().trim();
        mPhone = mPhoneField.getText().toString().trim();

        if (!TextUtils.isEmpty(mName) && !TextUtils.isEmpty(mPhone)) {
            HashMap map = new HashMap();
            map.put("name", mName);
            map.put("phone", mPhone);
            mCustomerDatabase.updateChildren(map);
            if(imageUri!=null){
                final StorageReference mRef = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
                Bitmap bitmap = null;

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(),imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = mRef.putBytes(data);
                 uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Map map = new HashMap<>();
                        map.put("profileImageUrl",downloadUrl.toString());
                        mCustomerDatabase.updateChildren(map);
                        Toast.makeText(CustomerSettingsActivity.this, "Successfully uploaded!", Toast.LENGTH_SHORT).show();
                        finish();
                        return;

                    }
                });
                 uploadTask.addOnFailureListener(new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception e) {
                         Toast.makeText(CustomerSettingsActivity.this, "Successfully uploaded!", Toast.LENGTH_SHORT).show();

                         finish();
                         return;
                     }
                 });

            }
            finish();
        } else {
            Toast.makeText(this, "Field must not be empty", Toast.LENGTH_SHORT).show();
        }


    }

    private void getUserInformation() {
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    HashMap<String, Object> map = (HashMap<String, Object>) dataSnapshot.getValue();

                    if (map.get("name") != null) {
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if (map.get("phone") != null) {
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if (map.get("profileImageUrl") != null){
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Picasso.with(getApplication()).load(mProfileImageUrl).placeholder(R.drawable.default_user).into(mProfileImage);

                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}

package com.example.chandora.rider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class CustomerLoginActivity extends RootAnimActivity {
    private EditText mEmail, mPassword;
    private Button mLogin, mSignUp;
    private FirebaseAuth mAuth;
    private String email, password;

    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ProgressDialog mDialog;
    private GpsCheckBroadcasteReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        mEmail = findViewById(R.id.et_email);
        mPassword = findViewById(R.id.et_passwd);
        mLogin = findViewById(R.id.btn_login);
        mSignUp = findViewById(R.id.btn_sign_up);

        mDialog = new ProgressDialog(this);
        receiver = new GpsCheckBroadcasteReceiver();

        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent driverLoginIntent = new Intent(CustomerLoginActivity.this, CustomersMapActivity.class);
                    startActivity(driverLoginIntent);
                    finish();
                    return;
                }

            }
        };


        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();

                if (validateDetails(email, password)) {
                    loginDialog();
                    signInUser(email, password);
                }

            }
        });

        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();

                if (validateDetails(email, password)) {
                    signUpDialog();
                    createUser(email, password);
                }
            }
        });

    }

    private boolean validateDetails(final String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fields should not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (password.length() <= 5) {
            Toast.makeText(this, "Password length must be equal or greater than 7", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void createUser(final String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    mDialog.dismiss();
                    Toast.makeText(CustomerLoginActivity.this, "Failed to sign up...", Toast.LENGTH_SHORT).show();
                } else {
                    mDialog.dismiss();
                    String userId = mAuth.getCurrentUser().getUid();
                    DatabaseReference current_user = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
                    current_user.setValue(email);
                    Toast.makeText(CustomerLoginActivity.this, "Successfully signed up!", Toast.LENGTH_SHORT).show();

                }
            }
        });


    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    mDialog.dismiss();
                    Toast.makeText(CustomerLoginActivity.this, "Failed to login...", Toast.LENGTH_SHORT).show();
                } else {
                    mDialog.dismiss();
                    Toast.makeText(CustomerLoginActivity.this, "Successfully login! ", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver,new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        }catch (Exception e){
            Log.i("Register  exception ",e+"");
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthStateListener);
    }

    private void loginDialog(){

        mDialog.setTitle("Signing in...");
        mDialog.setMessage("You must have got a nice smile ,so always smile... :)");
        mDialog.show();


    }
    private void signUpDialog(){

        mDialog.setTitle("Signing up...");
        mDialog.setMessage("You'r about to becoming a Rider :)");
        mDialog.show();

    }

}

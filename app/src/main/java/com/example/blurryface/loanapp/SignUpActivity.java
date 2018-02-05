package com.example.blurryface.loanapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {
    EditText firstName,lastName,id,birth,email,password;
    DatabaseReference userDatabase;
    FirebaseUser currentUser;
    FirebaseAuth auth;
    Toolbar signToolBar;
    Calendar calendar;
    DatePickerDialog.OnDateSetListener date;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        signToolBar = findViewById(R.id.signToolbar);
        firstName = findViewById(R.id.editTextFname);
        lastName = findViewById(R.id.editTextLname);
        id = findViewById(R.id.editTextId);
        birth = findViewById(R.id.editTextDob);
        email = findViewById(R.id.editTextemail);
        password = findViewById(R.id.editTextpass);
        //initialise the calender
        calendar = Calendar.getInstance();
        date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,day);
                updateDateEditText();
            }
        };
        //initialise firebase elements
        userDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        auth = FirebaseAuth.getInstance();

        //give our toolbar a name and back button
        setSupportActionBar(signToolBar);
        getSupportActionBar().setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    public void onSignUp(View view){
        final String fName = firstName.getText().toString();
        final String lName = lastName.getText().toString();
        final String nationalId = id.getText().toString();
        final String dob = birth.getText().toString();
        String mEmail = email.getText().toString();
        String pass = password.getText().toString();
        if(TextUtils.isEmpty(fName)||TextUtils.isEmpty(lName)||TextUtils.isEmpty(nationalId)||TextUtils.isEmpty(dob)||TextUtils.isEmpty(mEmail)||TextUtils.isEmpty(pass)){
            Toast.makeText(SignUpActivity.this,"All Fields must be filled",Toast.LENGTH_LONG).show();
            return;
        }
        auth.createUserWithEmailAndPassword(mEmail,pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //once user has created the an account with email and password add all the information to the realtime database
                currentUser = auth.getCurrentUser();
                HashMap<String,String> userData = new HashMap<>();
                userData.put("FirstName",fName);
                userData.put("LastName",lName);
                userData.put("DateOfBirth",dob);
                userDatabase.child(currentUser.getUid()).setValue(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(SignUpActivity.this,LogInActivity.class);
                        startActivity(intent);

                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //error for adding to the real-time database
                        Toast.makeText(SignUpActivity.this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error for creating user with email and password
                Toast.makeText(SignUpActivity.this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
            }
        });

    }
    public void onDatePicker(View view){
        new DatePickerDialog(SignUpActivity.this, date, calendar
                .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    public void updateDateEditText(){
        String dateFormat = "MMMM dd, YYYY";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        birth.setText(simpleDateFormat.format(calendar.getTime()));
    }
}

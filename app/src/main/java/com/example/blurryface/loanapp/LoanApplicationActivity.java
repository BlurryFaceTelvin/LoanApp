package com.example.blurryface.loanapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.africastalking.AfricasTalking;
import com.africastalking.models.payment.B2CResponse;
import com.africastalking.models.payment.Consumer;
import com.africastalking.models.payment.checkout.MobileCheckoutRequest;
import com.africastalking.services.PaymentService;
import com.africastalking.utils.Callback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class LoanApplicationActivity extends AppCompatActivity {
    Toolbar toolbar;
    FirebaseUser currentUser;
    DatabaseReference loanDatabase;
    EditText amountBorowed,time;
    int loans;
    int CURRENT_LIMIT;

    private PaymentService paymentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_application);
        //initialise
        toolbar = findViewById(R.id.loanApplicationToolbar);
        amountBorowed = findViewById(R.id.AmountEditText);
        time = findViewById(R.id.periodEditText);
        //give the toolbar a name and back button
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Application For Loan");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //initialise firebase database ref
        loanDatabase = FirebaseDatabase.getInstance().getReference().child("Loans");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        CURRENT_LIMIT = Integer.parseInt(getIntent().getStringExtra("limit"));
        loans =0;


    }
    public void onApply(View view){
        String money = amountBorowed.getText().toString();
        String months = time.getText().toString();
        if(TextUtils.isEmpty(money)||TextUtils.isEmpty(months)){
            Toast.makeText(LoanApplicationActivity.this,"All fields must be filled",Toast.LENGTH_LONG).show();
            return;
        }
        if(Integer.parseInt(months)>12){
            time.setError("Your Payment period cannot be more than 12 months");
            return;
        }
        if(Integer.parseInt(money)>CURRENT_LIMIT){
            amountBorowed.setError("The amount you borrow cannot be more than our current limit");
            return;
        }

        String current_user_id = currentUser.getUid();
        //calculate the expected payment period
        int expectedMonth = Calendar.getInstance().get(Calendar.MONTH) + Integer.parseInt(months);
        int expectedYear = Calendar.getInstance().get(Calendar.YEAR);
        int expectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if(expectedMonth>12){
            expectedMonth-=12;
            expectedYear++;
        }
        String expectedDate = expectedMonth+"/"+expectedDay+"/"+expectedYear;


        //check whether user has ever applied for a loan
        if(loanDatabase!=null){
            //new Loan Applicant
            loans++;
            String loansTaken = String.valueOf(loans);
            final HashMap<String,String> loanData = new HashMap<>();
            loanData.put("date",expectedDate);
            loanData.put("amount",money);
            loanData.put("loansTaken",loansTaken);
            loanDatabase.child(current_user_id).setValue(loanData).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    BustoCons();
                    Toast.makeText(LoanApplicationActivity.this,"You have successfully applied for a loan",Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(LoanApplicationActivity.this,LoansActivity.class);
                    intent.putExtra("expectedDate",loanData.get("date"));
                    intent.putExtra("loan",loanData.get("amount"));
                    intent.putExtra("loansTaken",loanData.get("loansTaken"));
                    startActivity(intent);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(LoanApplicationActivity.this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
                }
            });
        }
        else {
            //check how many
            Toast.makeText(LoanApplicationActivity.this,"You have already Applied for a loan. Pay it to get a new one",Toast.LENGTH_LONG).show();
        }
    }


    public void BustoCons(){
        try {
            String amount = amountBorowed.getText().toString();
            Consumer consumer = new Consumer("Telvin","0703280748","KES "+amount,Consumer.REASON_BUSINESS);
            List<Consumer> list = new ArrayList<>();
            list.add(consumer);
            paymentService = AfricasTalking.getPaymentService();
            paymentService.mobileB2C("LoanApp", list, new Callback<B2CResponse>() {
                @Override
                public void onSuccess(B2CResponse data) {
                    Toast.makeText(LoanApplicationActivity.this,data.entries.get(0).errorMessage,Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

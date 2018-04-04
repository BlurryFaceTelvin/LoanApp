package com.example.blurryface.loanapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.africastalking.AfricasTalking;
import com.africastalking.models.payment.B2CResponse;
import com.africastalking.models.payment.Consumer;
import com.africastalking.models.payment.checkout.MobileCheckoutRequest;
import com.africastalking.services.PaymentService;
import com.africastalking.utils.Callback;
import com.africastalking.utils.Logger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class LoanApplicationActivity extends AppCompatActivity {
    Toolbar toolbar;
    FirebaseUser currentUser;
    DatabaseReference loanDatabase;
    EditText amountBorowed,time;
    int loans;
    int CURRENT_LIMIT;
    SpotsDialog applyDialog;

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
        //initialise the spot dialog
        applyDialog = new SpotsDialog(this,"Applying For Loan");
        //initialize the Africa's Talking API ip address of my machine
        try {
            AfricasTalking.initialize("10.66.19.88",35897, true);
            AfricasTalking.setLogger(new Logger() {
                @Override
                public void log(String message, Object... args) {
                    Log.e("BBB", message);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void onApply(View view){
        //when button is clicked call b2c transaction and send specified money to consumer
        applyDialog.show();
        BustoCons();
    }
    public void onApplyLoan(){

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
        Log.e("month",String.valueOf(Calendar.getInstance().get(Calendar.MONTH)));
        int expectedMonth = Calendar.getInstance().get(Calendar.MONTH) + Integer.parseInt(months);
        int expectedYear = Calendar.getInstance().get(Calendar.YEAR);
        int expectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if(expectedMonth>12){
            expectedMonth-=12;
            expectedYear++;
        }
        Log.e("expectedMonth",String.valueOf(expectedMonth));
        Log.e("expectedDay",String.valueOf(expectedDay));
        Log.e("expectedYear",String.valueOf(expectedYear));
        String expectedDate = expectedDay+"/"+expectedMonth+"/"+expectedYear;
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
                    applyDialog.dismiss();
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
                    applyDialog.dismiss();
                    Toast.makeText(LoanApplicationActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
        else {
            //check how many
            Toast.makeText(LoanApplicationActivity.this,"You have already Applied for a loan. Pay it to get a new one",Toast.LENGTH_LONG).show();
        }
    }

    //method for B2C transaction
    public void BustoCons(){
        try {
            PaymentService paymentService;
            String amount = amountBorowed.getText().toString();
            Consumer consumer = new Consumer("Telvin","0703280748","KES "+amount,Consumer.REASON_BUSINESS);
            List<Consumer> list = new ArrayList<>();
            list.add(consumer);
            paymentService = AfricasTalking.getPaymentService();
            paymentService.mobileB2C("LoanApp", list, new Callback<B2CResponse>() {
                @Override
                public void onSuccess(B2CResponse data) {

                    //if you don't have enough money
                    if(data.entries.get(0).errorMessage.equals("Insufficient funds in the wallet")){
                        applyDialog.dismiss();
                        Toast.makeText(LoanApplicationActivity.this,"We dont have that kind of money now,Sorry for the inconvenience",Toast.LENGTH_LONG).show();
                    }else {
                        onApplyLoan();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    applyDialog.dismiss();
                    Log.e("fail",throwable.getMessage());
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

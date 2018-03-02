package com.example.blurryface.loanapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.africastalking.AfricasTalking;
import com.africastalking.models.payment.checkout.CheckoutResponse;
import com.africastalking.models.payment.checkout.MobileCheckoutRequest;
import com.africastalking.services.PaymentService;
import com.africastalking.utils.Callback;
import com.africastalking.utils.Logger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;

import dmax.dialog.SpotsDialog;

public class LoansActivity extends AppCompatActivity {

    Toolbar toolbar;
    FirebaseUser currentUser;
    TextView limitTextView,date,loanAmount,loansTaken;
    DatabaseReference loansRef;
    SpotsDialog spotsDialog;
    boolean isFirstResume;
    int status;
    OkHttpClient client;
    Request request;
    String current_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans);
        toolbar = findViewById(R.id.mainLoanToolbar);
        limitTextView = findViewById(R.id.loanPaymentStaustextview);
        date = findViewById(R.id.dateTextView);
        loanAmount = findViewById(R.id.amountTextView);
        loansTaken = findViewById(R.id.currentLoantextView);
        //initialise the spotDialog
        spotsDialog = new SpotsDialog(this,"Wait for a while");
        spotsDialog.setCanceledOnTouchOutside(false);
        //initialise the user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loansRef = FirebaseDatabase.getInstance().getReference().child("Loans");
        //give the toolbar a title
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("GetALoan");
        current_user_id = currentUser.getUid();
        //check if user is signed in if not send him to our welcome activity
        if(currentUser==null){
            welcomeActivity();
        }
        Log.e("loanstat",loansRef.child(current_user_id).toString());
        loansRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child("date").getValue()!=null) {
                        String mydate = dataSnapshot.child("date").getValue().toString();
                        String myloanAmount = dataSnapshot.child("amount").getValue().toString();
                        String myloanTaken = dataSnapshot.child("loansTaken").getValue().toString();
                        date.setText(mydate);
                        loanAmount.setText(myloanAmount);
                        loansTaken.setText(myloanTaken);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(LoansActivity.this,databaseError.getMessage(),Toast.LENGTH_LONG).show();
                }
            });

        //initialize the Africa's Talking API ip address
        try {
            AfricasTalking.initialize("192.168.137.236",35897, true);
            AfricasTalking.setLogger(new Logger() {
                @Override
                public void log(String message, Object... args) {
                    Log.e("AAA", message);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //first resume is true status to zero for first onResume
        isFirstResume = true;
        status = 0;
    }

    public void welcomeActivity(){
        Intent intent = new Intent(LoansActivity.this,WelcomeActivity.class);
        //makes sure when you press back button you cant go back to LogInActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    public void onLoanApply(View view){
        //one can only apply for a loan if the loan Amount us zero
        int amount = Integer.parseInt(loanAmount.getText().toString());
        if(amount==0){
            String currentLimit = limitTextView.getText().toString();
            Intent in = new Intent(LoansActivity.this,LoanApplicationActivity.class);
            in.putExtra("limit",currentLimit);
            startActivity(in);
        }else {
            Toast.makeText(LoansActivity.this,"You have already applied for a loan. Pay First",Toast.LENGTH_LONG).show();
        }

    }
    public void onPayLoan(View view)
    {
        int amount = Integer.parseInt(loanAmount.getText().toString());
        if(amount!=0) {
            spotsDialog.show();
            new Paying().execute();
        }else{
            Toast.makeText(LoansActivity.this,"You havent taken a loan yet",Toast.LENGTH_LONG).show();
        }
        status=5;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case R.id.log_out_item:
                //sign out
                FirebaseAuth.getInstance().signOut();
                welcomeActivity();
                break;
        }
        return true;
    }

    public void payProcess(){
        PaymentService paymentService;
        int amount = Integer.parseInt(loanAmount.getText().toString());
        //if user has an amount that he loaned use Africastalking API to pay
            try {
                AfricasTalking.setLogger(new Logger() {
                    @Override
                    public void log(String message, Object... args) {
                        Log.e("ERrr",message);
                    }
                });
                paymentService = AfricasTalking.getPaymentService();
                MobileCheckoutRequest checkoutRequest = new MobileCheckoutRequest("LoanApp", "KES "+amount, "0703280748");
                paymentService.checkout(checkoutRequest, new Callback<CheckoutResponse>() {
                    @Override
                    public void onSuccess(CheckoutResponse data) {
                        spotsDialog.dismiss();
                        Toast.makeText(LoansActivity.this,data.status,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("errror",throwable.getMessage());
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
                //Toast.makeText(LoansActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }


    }
    public class Paying extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            payProcess();
            return null;
        }
    }
    //on resume method
    @Override
    protected void onResume() {
        super.onResume();
        if(isFirstResume){
            Log.e("resume","first resume"+String.valueOf(status));
            isFirstResume=false;
        }else if(!isFirstResume&&status==5){
            //make sure to change the status
            Log.e("resume",String.valueOf(status));
            status = 3;
            spotsDialog.show();
            //we want to wait for confirmation 10 seconds
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    confirmPayment();
                }
            }, 10000);

        }else {
            //user pauses the app
            Log.e("resume","normal"+String.valueOf(status));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        super.onPause();
        if(status==5){
            Log.e("pause",String.valueOf(status));
            status = 5;
        }
        else {
            status=3;
        }
    }
    //method to confirm payment.....whether successful or not
    public void confirmPayment(){
        client = new OkHttpClient();
        request = new Request.Builder().url("http://192.168.137.236:30001/transactionLoan/status").build();
        client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                spotsDialog.dismiss();
                Log.e("message",e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                //remove the expected payment date and the the amount
                spotsDialog.dismiss();
                String status = response.body().string();
                if (status.equals("Success")) {
                    Log.e("message", "Payment was successful");
                    loanAmount.post(new Runnable() {
                        @Override
                        public void run() {
                            loanAmount.setText(String.valueOf(0));
                        }
                    });
                    loansTaken.post(new Runnable() {
                        @Override
                        public void run() {
                            loansTaken.setText(String.valueOf(0));
                        }
                    });
                    date.post(new Runnable() {
                        @Override
                        public void run() {
                            date.setText("0/0/0");
                        }
                    });
                    Log.e("response", response.body().string());

                    //remove the loan from the database
                    loansRef.child(current_user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LoansActivity.this, "Payment was successful", Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoansActivity.this, "Payment was unsuccessful", Toast.LENGTH_LONG).show();
                        }
                    });
                }else if(status.equals("Failed")){
                    showFailedMessage();
                }
            }
        });

    }
    //message if unsuccessful with paying
    public void showFailedMessage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoansActivity.this, "Payment Failed", Toast.LENGTH_LONG).show();
            }
        });
    }

}

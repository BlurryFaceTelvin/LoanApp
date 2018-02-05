package com.example.blurryface.loanapp;

import android.content.Intent;
import android.os.AsyncTask;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class LoansActivity extends AppCompatActivity {

    Toolbar toolbar;
    FirebaseUser currentUser;
    TextView limitTextView,date,loanAmount,loansTaken;
    DatabaseReference loansRef;
    PaymentService paymentService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans);
        toolbar = findViewById(R.id.mainLoanToolbar);
        limitTextView = findViewById(R.id.loanPaymentStaustextview);
        date = findViewById(R.id.dateTextView);
        loanAmount = findViewById(R.id.amountTextView);
        loansTaken = findViewById(R.id.currentLoantextView);
        //initialise the user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loansRef = FirebaseDatabase.getInstance().getReference().child("Loans");
        //give the toolbar a title
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("GetALoan");
        //make sure our intent data comes from the LoanApplicationActivity
        Bundle extras = getIntent().getExtras();
        //if(snapshot.getRef()==null){

            String current_user_id = currentUser.getUid();
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
                    Toast.makeText(LoansActivity.this,databaseError.getMessage().toString(),Toast.LENGTH_LONG).show();
                }
            });


            //Toast.makeText(LoansActivity.this,snapshot.getRef().toString(),Toast.LENGTH_LONG).show();
        //}

        //initialize the Africa's Talking API ip address of my machine
        try {
            AfricasTalking.initialize("192.168.1.130",35897, true);
            AfricasTalking.setLogger(new Logger() {
                @Override
                public void log(String message, Object... args) {
                    Log.e("AAA", message);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //check if user is signed in if not send him to our welcome activity
        if(currentUser==null){
            welcomeActivity();
        }
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
        new Paying().execute();
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
    public class Paying extends AsyncTask<Void,String,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            int amount = Integer.parseInt(loanAmount.getText().toString());
            if(amount!=0) {
                try {

                    paymentService = AfricasTalking.getPaymentService();
                    MobileCheckoutRequest checkoutRequest = new MobileCheckoutRequest("LoanApp", "KES 10", "0703280748");

                    paymentService.checkout(checkoutRequest, new Callback<CheckoutResponse>() {
                        @Override
                        public void onSuccess(CheckoutResponse data) {
                            Toast.makeText(LoansActivity.this, data.description.toString(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {

                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LoansActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }else
                Toast.makeText(LoansActivity.this,"You do not have an active account",Toast.LENGTH_LONG).show();
            return null;
        }
    }
}

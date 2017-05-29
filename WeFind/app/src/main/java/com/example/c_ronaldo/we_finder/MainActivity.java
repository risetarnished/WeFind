package com.example.c_ronaldo.we_finder;

import android.content.Intent;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private final String TITLE = "WeFind";
    private final String TAG = this.getClass().getSimpleName();
    private static final int SIGNIN = 0;
    private static final int SIGNUP = 1;
    private static final int SIGNIN_INTENT_REQUEST = 0;
    private static final int SIGNUP_INTENT_REQUEST = 1;
    private EditText mEmail;
    private EditText mPassword;
    private Button mSignInButton;
    private Button mSignUpButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(TITLE);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
        mEmail = (EditText)this.findViewById(R.id.email);
        mPassword = (EditText)this.findViewById(R.id.password);
        mSignInButton = (Button) findViewById(R.id.signin_button);
        mSignInButton.setOnClickListener(this);
        mSignUpButton = (Button) findViewById(R.id.signup_button);
        mSignUpButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signin_button:
                String userEmail = mEmail.getText().toString();
                String userPassword = mPassword.getText().toString();
                signIn(userEmail, userPassword);
                break;
            case R.id.signup_button:
                go(SIGNUP);
                break;
        }
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(getApplicationContext(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            String msg = "Please check your email address and password";
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        } else {
                            go(SIGNIN);
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private boolean validateForm() {
        boolean valid = true;
        String email = mEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email Address Required.");
            valid = false;
        } else {
            mEmail.setError(null);
        }

        String password = mPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPassword.setError("Password Required.");
            valid = false;
        } else {
            mPassword.setError(null);
        }
        return valid;
    }

    private void go(int button) {
        switch (button) {
            case SIGNIN:
                String msg = "Signed in successfully\nWelcome!";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
//                startActivityForResult(new Intent(this, TinderActivity.class), SIGNIN_INTENT_REQUEST);
                Intent signInIntent = new Intent(this, TinderActivity.class);
                startActivityForResult(signInIntent, SIGNIN_INTENT_REQUEST);
                break;
            case SIGNUP:
                Intent signUpIntent = new Intent(this, SignUpActivity.class);
                startActivityForResult(signUpIntent, SIGNUP_INTENT_REQUEST);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != SIGNUP_INTENT_REQUEST) {
            Log.d(TAG, "Intent did not come back from SignUpActivity");
            return;
        }
        switch (resultCode) {
            case RESULT_CANCELED:
                Log.d(TAG, "Did not create account");
                break;
            case RESULT_OK:
                Log.d(TAG, "Getting email and password");
                mEmail.setText(data.getStringExtra("email"));
                mPassword.setText(data.getStringExtra("password"));
                break;
        }
    }
}

package com.example.c_ronaldo.we_finder;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private final String TITLE = "Sign Up";
    private Spinner mYearSpinner;
    private Spinner mZodiacSpinner;
    private Spinner mGenderSpinner;
    private EditText mUsername;
    private EditText mEmail;
    private EditText mPassword;
    private EditText mRepeatPassword;
    private ImageView mProfile;
    private String zodiacSign;
    private String gender;
    private String year;
    private ArrayAdapter<String> yearAdapter;
    private ArrayAdapter<String> zodiacAdapter;
    private ArrayAdapter<String> genderAdapter;
    private List<String> yearList = new ArrayList<>();
    private String[] signList = {"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Pisces", "Aquarius",
            "Libra", "Sagittarius", "Scorpio", "Capricorn", "Virgo"};
    private String[] genderList = {"Male", "Female"};
    private static final int PICK_IMAGE  = 666;
    private static final int CAMERA_REQUEST_CODE = 999;
    private Uri imageUri;
    private String username;
    private String email;
    private Boolean hasImage;
    ProgressDialog progress;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    StorageReference mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        setTitle(TITLE);

        mYearSpinner = (Spinner)this.findViewById(R.id.year_spinner);
        mZodiacSpinner = (Spinner)this.findViewById(R.id.zodiac_spinner);
        mGenderSpinner = (Spinner)this.findViewById(R.id.gender_spinner);
        mUsername = (EditText)this.findViewById(R.id.username);
        mEmail = (EditText)this.findViewById(R.id.email);
        mPassword = (EditText)this.findViewById(R.id.password);
        mRepeatPassword = (EditText)this.findViewById(R.id.repeat_password);
        mProfile = (ImageView)this.findViewById(R.id.profile_picture);

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
            }
        };

        mStorage = FirebaseStorage.getInstance().getReference();
        hasImage = false;

        generateYearList();
        setYearAdapter();
        setZodiacAdapter();
        setGenderAdapter();

        //show progress
        progress = new ProgressDialog(this);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.saveuser_menu, menu);
        return true;
    }

    //save user menu onClick
    public void saveUserMenuClicked(MenuItem selectedMenu){
        Log.i("menuLog","saveMenuClicked!");
        username = mUsername.getText().toString();
        email = mEmail.getText().toString();
        // All fields must be filled before signing up
        if (username.isEmpty() || email.isEmpty() || year.isEmpty() || zodiacSign.isEmpty() ||
                gender.isEmpty() || !hasImage) {
            Log.d(TAG, "Some fields missing\nNot creating new user");
            String msg = "Please complete the required fields";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference firepath = mStorage.child("Photos").child(username);

        // Upload bitmap (photo taken by user)
        mProfile.setDrawingCacheEnabled(true);
        mProfile.buildDrawingCache();
        Bitmap bitmap = mProfile.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = firepath.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String password = mPassword.getText().toString();
                if (!createAccount(email, password)) {
                    Log.d(TAG, "Create account failed.");
                } else {
                    progress.setMessage("Adding user to Firebase, please wait");
                    progress.show();
                    // Get the image uri
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    Toast.makeText(getApplication(),"Upload done!",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Create account succeeded");
                    // Pass the email and password back to login page for quick login
                    Intent toPassBack = getIntent();
                    toPassBack.putExtra("email", email);
                    toPassBack.putExtra("password", password);
                    setResult(RESULT_OK, toPassBack);
                    Log.d(TAG, "Adding current user info to Firebase Database");
                    upLoadToFirebase(username, email, year, zodiacSign, gender, downloadUri.toString());
                    progress.dismiss();
                    Toast.makeText(getApplication(), "User Created on Firebase. Please sign in.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    public void onUploadPortraitClicked(View button){
        openGallery();
    }

    public void onTakePhotoClicked(View view) {
        Intent goToTakePhoto = new Intent();
        goToTakePhoto.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(goToTakePhoto,CAMERA_REQUEST_CODE);
    }

    public void openGallery(){
        Intent goToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(goToGallery,PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            hasImage = true;
            Log.i("forImage","whereIsImage");
            imageUri = data.getData();
            Log.i("forImage",imageUri.toString());
            mProfile.setImageURI(imageUri);
        }
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            hasImage = true;
            Log.i("forImage","whereIsImage");
            Bundle extras = data.getExtras();
            Bitmap photoBitmap = (Bitmap) extras.get("data");
            mProfile.setImageBitmap(photoBitmap);
        }

    }

    public void generateYearList(){
        yearList.clear();
        for(int i=1900;i<=2017;i++){
            yearList.add(Integer.toString(i));
        }
        Collections.reverse(yearList);
    }

    public void setYearAdapter(){
        yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, yearList);
        mYearSpinner.setAdapter(yearAdapter);
        mYearSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                year = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }});
    }

    public void setZodiacAdapter(){
        zodiacAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, signList);
        mZodiacSpinner.setAdapter(zodiacAdapter);
        mZodiacSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                zodiacSign = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }});
    }

    public void setGenderAdapter(){
        genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, genderList);
        mGenderSpinner.setAdapter(genderAdapter);
        mGenderSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                gender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }});
    }

    private boolean createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return false;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Firebase Authentication Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        return true;
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
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            mPassword.setError("Password must have at least 6 characters.");
            valid = false;
        } else {
            mPassword.setError(null);
        }

        String rePassword = mRepeatPassword.getText().toString();
        if (TextUtils.isEmpty(rePassword) || rePassword.length() < 6 ||
                !rePassword.equals(password)) {
            mRepeatPassword.setError("Passwords don't match");
            valid = false;
        } else {
            mPassword.setError(null);
        }
        return valid;
    }

    private void upLoadToFirebase(String username, String email, String year, String zodiac,
                                  String gender, String uri) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        database.setPersistenceEnabled(true);   // Enable offline writing
        DatabaseReference userTable = database.getReference("users");
        User currentUser = new User(username, email, year, zodiac, gender, uri);
        Log.d(TAG, "User being uploaded: " + currentUser);
        userTable.child(username).setValue(currentUser);
    }
}

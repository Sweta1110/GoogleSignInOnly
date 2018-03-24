package com.example.swedish_sweta.signinwithgoogle;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Toast;

import com.example.swedish_sweta.signinwithgoogle.models.Users;
import com.example.swedish_sweta.signinwithgoogle.utils.Constants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private SignInButton mGoogleBtn;
    private static final int RC_SIGN_IN=9001;
    private GoogleApiClient mGooogleApiClient;
   // FirebaseAuth mAuth;
    private static final String TAG=MainActivity.class.getSimpleName();
    private FirebaseAuth.AuthStateListener mAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.googleButton).setOnClickListener(this);

        mAuthListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser=firebaseAuth.getCurrentUser();
                     if(mFirebaseUser!=null) {
                         if (mFirebaseUser != null) {
                             if (BuildConfig.DEBUG)
                                 Log.d(TAG, "onAuthStateChanged:signed_in" + mFirebaseUser.getDisplayName());
                         } else {
                             if (BuildConfig.DEBUG) Log.d(TAG, "onAuthStateChanged:signed_out");
                         }
                     }


            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener!=null)mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.googleButton:
                showProgressDialog();
                signIn();
        }
    }
    private void signIn(){
        Intent signInIntent= Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            if(requestCode==RC_SIGN_IN){
                GoogleSignInResult result=Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if(result.isSuccess()){
                    GoogleSignInAccount account=result.getSignInAccount();
                    firebaseAuthWithGoogle(account);
                }else{
                    hideProgressDialog();
                }
            }else{
                hideProgressDialog();
            }
        }else{
            hideProgressDialog();
        }
    }
    private void firebaseAuthWithGoogle(final GoogleSignInAccount account){
        if(BuildConfig.DEBUG)Log.d(TAG,"firebaseAuthWithGoogle:"+account.getDisplayName());
        AuthCredential credential= GoogleAuthProvider.getCredential((account.getIdToken()),null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(BuildConfig.DEBUG)Log.d(TAG,"signInWithCredential:onComplete:"+task.isSuccessful());
                        if(task.isSuccessful()){
                            String photoUrl=null;
                            if(account.getPhotoUrl()!=null){
                                photoUrl=account.getPhotoUrl().toString();
                            }
                            Users user=new Users(
                                    account.getDisplayName()+" "+account.getFamilyName(),
                                    account.getEmail(),
                                    photoUrl,FirebaseAuth.getInstance().getCurrentUser().getUid()
                            );
                            FirebaseDatabase database=FirebaseDatabase.getInstance();
                            DatabaseReference userRef=database.getReference(Constants.USER_KEY);
                            userRef.child(account.getEmail().replace(".",","))
                                    .setValue(user,new DatabaseReference.CompletionListener(){
                                        @Override
                                        public void onComplete(DatabaseError databaseError,DatabaseReference databaseReference){
                                            startActivity(new Intent(MainActivity.this,NavDrawerActivity.class));
                                        }
                                            }

                                    );
                            if(BuildConfig.DEBUG)Log.d(TAG,"Authentication successful");
                        }
                        else{
                            hideProgressDialog();
                            if(BuildConfig.DEBUG){
                                Log.d(TAG,"signInWithCredential",task.getException());
                                Log.d(TAG,"Authentication failed");
                                Toast.makeText(MainActivity.this,"Athentication Failed.",Toast.LENGTH_SHORT).show();
                                signOut();
                            }
                        }
                    }
                });
    }
}

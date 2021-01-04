package uk.co.thomasroe.officequiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.multidex.BuildConfig;

import java.util.ArrayList;

public class MenuActivity extends AppCompatActivity implements BillingHelper.BillingClientStarted, BillingHelper.QueryPurchasesCompleted {
    private static final String TAG = "MenuActivity";

    public static final String KEY_PREMIUM_PURCHASED = "premiumPurchased"; // Don't ever change this
    public static final String AUDIO_PREFERENCE = "audioPreference";
    public static final String SHARED_PREFS = "sharedPrefs";

    private BillingHelper billingHelper;
    private SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        prefsEditor = prefs.edit();

        // Get current sound effects preference
        SwitchCompat switchSoundEffects = findViewById(R.id.switchMenuSoundEffects);
        int audioPreferences = prefs.getInt(AUDIO_PREFERENCE, 1);
        switchSoundEffects.setChecked(audioPreferences == 1);

        // Switch to turn sound effects on and off
        switchSoundEffects.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefsEditor.putInt(AUDIO_PREFERENCE, 1);
                    prefsEditor.apply();
                }
                if (!isChecked) {
                    prefsEditor.putInt(AUDIO_PREFERENCE, 0);
                    prefsEditor.apply();
                }
            }
        });

        // Destroy the Billing Client and exit back to the previous activity
        ImageView imageViewExitCross = findViewById(R.id.imageViewMenuExitCross);
        imageViewExitCross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != billingHelper) {
                    billingHelper.destroyBillingClient();
                }
                finish();
            }
        });

        // Open the activity which explains how to play
        Button buttonHowToPlay = findViewById(R.id.buttonMenuHowToPlay);
        buttonHowToPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, HowToPlayActivity.class);
                startActivity(intent);
            }
        });

        // Use an intent to send an email with a pre-populated subject heading
        Button buttonEmailUs = findViewById(R.id.buttonMenuEmailUs);
        buttonEmailUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendEmail = new Intent(android.content.Intent.ACTION_SEND);
                sendEmail.setType("plain/text");
                sendEmail.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"tom@scribbleapps.co.uk"});
                sendEmail.putExtra(android.content.Intent.EXTRA_SUBJECT, "The Office Trivia Quiz enquiry");
                startActivity(Intent.createChooser(sendEmail, "Send email.."));

            }
        });

        // Open an activity that displays a list of credits for people who have provided icons and sounds
        Button buttonViewCredits = findViewById(R.id.buttonMenuViewCredits);
        buttonViewCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, CreditsActivity.class);
                startActivity(intent);
            }
        });

        // Use the BillingHelper class to check if premium has been paid for
        // queryPurchasesCompleted below shows the outcome
        Button buttonReinstatePurchase = findViewById(R.id.buttonMenuRestorePurchase);
        buttonReinstatePurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingHelper = new BillingHelper(MenuActivity.this, MenuActivity.this, MenuActivity.this);
                billingHelper.startBillingClient();
            }
        });

        // Share a link to this app on the Google Play Store through WhatsApp
        Button buttonShareOnWhatsApp = findViewById(R.id.buttonMenuShareOnWhatsapp);
        buttonShareOnWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager pm = MenuActivity.this.getPackageManager();
                boolean isInstalled = isPackageInstalled("com.whatsapp", pm);
                if(isInstalled) {
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        String shareMessage= "Play The Office Trivia Quiz on Android!\n\n";
                        shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        shareIntent.setType("text/plain");
                        shareIntent.setPackage("com.whatsapp");
                        startActivity(shareIntent);
                    } catch(Exception e) {
                        Toast.makeText(MenuActivity.this, "Sorry, this action couldn't be performed right now.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "onClick: error sharing on WhatsApp: " + e.getMessage() );
                    }
                } else {
                    Toast.makeText(MenuActivity.this, "Sorry, it doesn't look as if you have WhatsApp installed.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    // Used to ensure WhatsApp is installed, prior to starting shareIntent
    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // BillingHelper Callback
    @Override
    public void billingClientStarted() {
        billingHelper.queryUserPurchases();
    }

    // BillingHelper Callback
    @Override
    public void queryPurchasesCompleted(ArrayList<String> productIDList) {
        if (productIDList.contains("premium")) {
            prefsEditor.putBoolean(KEY_PREMIUM_PURCHASED, true).apply();
            Toast.makeText(MenuActivity.this, "You have purchased premium - ads should no longer be displayed.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MenuActivity.this, "It doesn't look like you've purchased premium. Ads will still show until it has been purchased.", Toast.LENGTH_LONG).show();
        }
    }

    // Ensure the Billing Client is destroyed when exiting this activity, then return to the previous activity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (null != billingHelper) {
            billingHelper.destroyBillingClient();
        }
        finish();
    }
}

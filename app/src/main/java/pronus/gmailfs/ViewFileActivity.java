package pronus.gmailfs;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.DragEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import gmailfs.base.FilterDialogListener;
import gmailfs.framework.AppContext;
import gmailfs.framework.File;
import gmailfs.framework.FileSystem;
import gmailfs.framework.Filter;
import gmailfs.tasks.FileLoader;
import gmailfs.tasks.ListLoader;
import gmailfs.view.ProgressListView;
import pub.devrel.easypermissions.EasyPermissions;

public class ViewFileActivity extends Activity {

    public static final String FILE_EXTRA = "file";

    private Activity activity = this;
    private File viewed;
    private TextView contents, sender, subject, timestamp;
    private WebView email;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature( Window.FEATURE_ACTION_BAR );
        getWindow().requestFeature( Window.FEATURE_PROGRESS );
        setContentView( R.layout.item_view_layout );

        viewed = ( File ) getIntent().getSerializableExtra( FILE_EXTRA );
        email = ( WebView ) findViewById( R.id.item_webview );
        contents = ( TextView ) findViewById( R.id.item_view_contents );
        sender = ( TextView ) findViewById( R.id.item_view_sender );
        subject = ( TextView ) findViewById( R.id.item_view_subject );
        timestamp = ( TextView ) findViewById( R.id.item_view_timestamp );

        email.getSettings().setJavaScriptEnabled(true);

        email.setWebChromeClient( new WebChromeClient() {
            public void onProgressChanged( WebView view, int progress ) {
                activity.setProgress( progress * 1000 );
            }
        } );
        email.setWebViewClient( new WebViewClient() {
            public void onReceivedError( WebView view, int errorCode, String description, String failingUrl ) {
                Toast.makeText( activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }
        });

        email.loadUrl("https://mail.google.com/mail/u/0/#inbox/" + viewed.getFileID() );

        //loadMessage();
    }

    private void loadMessage() {
        sender.setText( viewed.getSender() );
        subject.setText( viewed.getSubject() );
        timestamp.setText( viewed.getTimestamp() );
        new FileLoader( contents, this ).execute( viewed.getFileID() );
    }

}
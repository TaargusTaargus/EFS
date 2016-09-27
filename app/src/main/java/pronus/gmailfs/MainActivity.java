package pronus.gmailfs;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.gmail.GmailScopes;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import gmailfs.base.FilterDialogListener;
import gmailfs.framework.AppContext;
import gmailfs.framework.FileSystem;
import gmailfs.framework.Filter;
import gmailfs.tasks.ListLoader;
import gmailfs.view.ProgressListView;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks, FilterDialogListener {

    private Filter dragged;
    private GoogleAccountCredential credential;
    private ListLoader itemRequest;
    private ImageButton backButton, homeButton, searchButton;
    private ImageView currentFilterIcon, trashView, editView;
    private LinearLayout listContainer, current, topNormal, topDrag;
    private ProgressListView list;
    private Spinner spinner;
    private TextView currentFilterTitle;

    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String [] SCOPES = { GmailScopes.GMAIL_READONLY };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main );

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( metrics );
        AppContext.screenHeight = metrics.heightPixels;
        AppContext.screenWidth = metrics.widthPixels;
        AppContext.credential = GoogleAccountCredential
                .usingOAuth2( getApplicationContext(), Arrays.asList( SCOPES ) )
                .setBackOff( new ExponentialBackOff() );

        AppContext.fs = new FileSystem( this );

        spinner = ( Spinner ) findViewById( R.id.account_lister );

        searchButton = ( ImageButton ) findViewById( R.id.search_messages_button );
        searchButton.setOnClickListener( new SearchClickListener() );
        backButton = ( ImageButton ) findViewById( R.id.back_filter_button );
        backButton.setOnClickListener( new BackFilterListener() );
        homeButton = ( ImageButton ) findViewById( R.id.home_filter_button );
        homeButton.setOnClickListener( new HomeFilterListener() );

        topNormal = ( LinearLayout ) findViewById( R.id.normal_top_layout );
        topDrag = ( LinearLayout ) findViewById( R.id.drag_top_layout );
        trashView = ( ImageView ) findViewById( R.id.remove_filter_area );
        trashView.setOnDragListener( new RemoveDragListener() );
        editView = ( ImageView ) findViewById( R.id.edit_filter_area );
        editView.setOnDragListener( new EditDragListener() );

        current = ( LinearLayout ) findViewById( R.id.current_filter_scrollview );
        currentFilterIcon = ( ImageView ) findViewById( R.id.current_filter_icon );
        currentFilterTitle = ( TextView ) findViewById( R.id.current_filter_title );
        listContainer = ( LinearLayout ) findViewById( R.id.message_list_container );
        list = new ProgressListView( listContainer, this );

        getAvailableAccounts();
    }

    @Override
    public void onCreateFilter( Filter filter ) {
        ProgressDialog tmp = new ProgressDialog( this );
        tmp.setMessage( "Saving filter..." );
        tmp.setCanceledOnTouchOutside( false );
        tmp.show();

        AppContext.fs.addFilter( filter );
        refreshFilterViews();
        tmp.dismiss();
    }

    @Override
    public void onEditFilter( Filter old, Filter newFilter ) {
        ProgressDialog tmp = new ProgressDialog( this );
        tmp.setMessage( "Saving filter..." );
        tmp.setCanceledOnTouchOutside( false );
        tmp.show();

        AppContext.fs.editFilter( old, newFilter );
        refreshFilterViews();
        tmp.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppContext.fs.close();
    }

    private void refreshItemViews() {
        if( itemRequest != null && !itemRequest.isCancelled() )
            itemRequest.cancel( true );
        ( itemRequest = new ListLoader( list, this ) )
                .execute( AppContext.fs.getPath().getFilterText() );
    }

    private void refreshFilterViews() {
        Log.d( "refreshing filter views", "....");
        if( AppContext.fs.isRoot() ) {
            backButton.setEnabled( false );
            homeButton.setEnabled( false );
            currentFilterTitle.setText( "" );
            currentFilterIcon.setImageResource( 0 );
        } else {
            currentFilterIcon.setImageResource( AppContext.fs.getCurrentFilter().getIconResource() );
            currentFilterTitle.setText( AppContext.fs.getCurrentFilter().getFilterID() );
            backButton.setEnabled( true );
            homeButton.setEnabled( true );
        }
        current.removeAllViews();
        LinkedList< Filter > currentFilters = AppContext.fs.currentFilters();
        TreeMap< String, LinearLayout > map = new TreeMap();
        for ( Filter child : currentFilters ) {
            LinearLayout item = ( LinearLayout ) getLayoutInflater()
                    .inflate( R.layout.filter_item_layout, current, false );
            ( ( ImageView ) item.findViewById( R.id.filter_image ) )
                    .setImageResource( child.getIconResource() );
            ( ( TextView ) item.findViewById( R.id.filter_title ) )
                    .setText( child.getFilterID() );
            item.setOnClickListener( new FilterClickListener( child ) );
            item.setOnLongClickListener( new FilterLongClickListener( child ) );
            item.setOnDragListener( new FilterDragListener() );
            map.put( child.getFilterID(), item );
        }
        for( String key : map.keySet() )
            current.addView( map.get( key ) );

    }

    private void getAvailableAccounts() {
        Account[] accounts = AccountManager.get( this ).getAccounts();
        HashMap< String, Account > accountMap = new HashMap();
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        for ( Account account : accounts ) {
            if ( emailPattern.matcher( account.name ).matches() )
                accountMap.put( account.name, account );
        }

        Set< String > keys = accountMap.keySet();
        String [] items = keys.toArray( new String[ keys.size() ] );
        ArrayAdapter< String > spinnerArrayAdapter = new ArrayAdapter( this, android.R.layout.simple_spinner_item, items );
        spinnerArrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        spinner.setOnItemSelectedListener( new AccountSelectListener( items ) );
        spinner.setAdapter( spinnerArrayAdapter );
    }

    private void getResultsFromApi() {
        //AppContext.credential.setSelectedAccountName( ( String ) spinner.getSelectedItem() );
        if ( !isGooglePlayServicesAvailable() )
            acquireGooglePlayServices();
        else if ( !isDeviceOnline()  )
            Toast.makeText( this, "No network connection available.", Toast.LENGTH_LONG ).show();
        else {
            refreshFilterViews();
            refreshItemViews();
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                   Toast.makeText( this, 
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.", Toast.LENGTH_LONG );
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString( ( String ) spinner.getSelectedItem(), accountName );
                        editor.apply();
                        AppContext.credential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult( int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        EasyPermissions.onRequestPermissionsResult( requestCode, permissions, grantResults, this );
    }

    @Override
    public void onPermissionsGranted( int requestCode, List<String> list ) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied( int requestCode, List<String> list ) {
        // Do nothing.
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                ( ConnectivityManager ) getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    public void showGooglePlayServicesAvailabilityErrorDialog( int connectionStatusCode ) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public class AccountSelectListener implements AdapterView.OnItemSelectedListener {

        private String [] accounts;

        public AccountSelectListener( String [] accounts ) {
            this.accounts = accounts;
        }

        @Override
        public void onItemSelected( AdapterView< ? > parent, View view, int position, long id ) {
            if ( EasyPermissions.hasPermissions( getApplication(), Manifest.permission.GET_ACCOUNTS ) ) {
                AppContext.credential.setSelectedAccountName( accounts[ position ] );
                AppContext.fs.changeAccount( accounts[ position ] );
                getResultsFromApi();
            } else {
                // Request the GET_ACCOUNTS permission via a user dialog
                EasyPermissions.requestPermissions(
                        this,
                        "This app needs to access your Google account (via Contacts).",
                        REQUEST_PERMISSION_GET_ACCOUNTS,
                        Manifest.permission.GET_ACCOUNTS );
            }
        }

        @Override
        public void onNothingSelected( AdapterView<?> parent ) { }
    }

    public class BackFilterListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AppContext.fs.back();
            refreshItemViews();
            refreshFilterViews();
        }
    }

    public class SearchClickListener implements View.OnClickListener {
        @Override
        public void onClick( View v ) {
            new SearchDialog().show( getFragmentManager(), "message_search" );
        }
    }

    public class FilterClickListener implements View.OnClickListener {

        private Filter filter;

        public FilterClickListener( Filter filter ) {
            this.filter = filter;
        }

        public void onClick( View v ) {
            AppContext.fs.forward( filter.getFilterID() );
            refreshItemViews();
            refreshFilterViews();
        }

    }

    public class FilterLongClickListener implements View.OnLongClickListener {

        private Filter filter;

        public FilterLongClickListener( Filter filter ) { this.filter = filter; }

        @Override
        public boolean onLongClick( View view ) {
            dragged = filter;
            ClipData data = ClipData.newPlainText( "", "" );
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder( view );
            view.startDrag( data, shadowBuilder, view, 0 );
            return true;
        }

    }

    public class FilterDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag( View view, DragEvent event ) {
            switch ( event.getAction() ) {
                case DragEvent.ACTION_DRAG_STARTED:
                    editView.setImageResource( R.drawable.edit_unselected_icon );
                    trashView.setImageResource( R.drawable.trash_unselected_icon);
                    topNormal.setVisibility( View.GONE );
                    topDrag.setVisibility( View.VISIBLE );
                    view.setVisibility( View.INVISIBLE );
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    editView.setImageResource( R.drawable.edit_unselected_icon );
                    trashView.setImageResource( R.drawable.trash_unselected_icon);
                    topDrag.setVisibility( View.GONE );
                    topNormal.setVisibility( View.VISIBLE );
                    view.setVisibility( View.VISIBLE );
            }
            return true;
        }

    }

    public class EditDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag( View view, DragEvent event ) {
            switch ( event.getAction() ) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    editView.setImageResource( R.drawable.edit_selected_icon );
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    editView.setImageResource( R.drawable.edit_unselected_icon );
                    break;
                case DragEvent.ACTION_DROP:
                    FilterDialog dialog = new FilterDialog();
                    dialog.insertFilterToEdit( dragged );
                    dialog.show( getFragmentManager(), "add_filter" );
                    break;
            }
            return true;
        }

    }

    public class RemoveDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag( View view, DragEvent event ) {
            switch ( event.getAction() ) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    trashView.setImageResource( R.drawable.trash_selected_icon );
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    trashView.setImageResource( R.drawable.trash_unselected_icon );
                    break;
                case DragEvent.ACTION_DROP:
                    if( AppContext.fs.getCurrentNode().getChild( dragged.getFilterID() ).hasChildren() )
                        ( new WarningDialog() {
                            @Override
                            public String getWarningMessage() {
                                return "This filter has children, deleting it will also delete them. Do you want to continue?";
                            }

                            @Override
                            public String getConfirmationMessage() {
                                return "Filter successfully removed.";
                            }

                            @Override
                            public void onEvent() {
                                removeFilter();
                            }
                        } ).show( getFragmentManager(), "warning_dialog" );
                    else
                        removeFilter();
                    break;
            }
            return true;
        }

        private void removeFilter() {
            AppContext.fs.removeFilter( dragged );
            refreshFilterViews();
        }

    }

    public class HomeFilterListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            AppContext.fs.root();
            refreshItemViews();
            refreshFilterViews();
        }

    }

}
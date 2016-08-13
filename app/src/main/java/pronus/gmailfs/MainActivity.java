package pronus.gmailfs;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.gmail.GmailScopes;

import com.google.api.services.gmail.model.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import gmailfs.base.AddFilterListener;
import gmailfs.framework.FileSystem;
import gmailfs.framework.Filter;
import gmailfs.framework.Filter.FilterKey;
import gmailfs.framework.Item;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks, AddFilterListener {

    private InitItemsListTask itemRequest;
    private FileSystem fs;
    private GoogleAccountCredential credential;
    private ImageButton addBin, addFilter, addItem, backButton, homeButton;
    private ImageView currentFilterIcon;
    private ItemListAdapter adapter;
    private LinearLayout current, progress, listProgress;
    private LinkedList< Item > userItems;
    private ListView itemList;
    private Spinner spinner;
    private String nextPage, startPage;
    private TextView currentFilterTitle;

    private static final long ITEMS_PER_REQUEST = 20l;
    private static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String [] SCOPES = { GmailScopes.GMAIL_READONLY };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main );

        addBin = ( ImageButton ) findViewById( R.id.add_bin_button );
        addBin.setOnClickListener( new AddBinClickListener( getFragmentManager() ) );
        addFilter = ( ImageButton ) findViewById( R.id.add_filter_button );
        addFilter.setOnClickListener( new AddFilterClickListener( getFragmentManager() ) );
        addItem = ( ImageButton ) findViewById( R.id.add_item_button );
        addItem.setOnClickListener( new AddItemClickListener() );
        backButton = ( ImageButton ) findViewById( R.id.back_filter_button );
        backButton.setOnClickListener( new BackFilterListener() );
        homeButton = ( ImageButton ) findViewById( R.id.home_filter_button );
        homeButton.setOnClickListener( new HomeFilterListener() );

        spinner = ( Spinner ) findViewById( R.id.account_lister );
        currentFilterIcon = ( ImageView ) findViewById( R.id.current_filter_icon );
        currentFilterTitle = ( TextView ) findViewById( R.id.current_filter_title );


        progress = ( LinearLayout ) findViewById( R.id.item_list_progress_layout );
        itemList = ( ListView ) findViewById( R.id.item_list );
        listProgress = ( LinearLayout ) getLayoutInflater().inflate( R.layout.progress_layout, null, false );
        itemList.addFooterView( listProgress );
        current = ( LinearLayout ) findViewById( R.id.current_filter_scrollview );


        credential = GoogleAccountCredential
                .usingOAuth2( getApplicationContext(), Arrays.asList( SCOPES ) )
                .setBackOff( new ExponentialBackOff() );

        fs = new FileSystem( this );

        populatePicker();
        refreshFilterViews();
        getResultsFromApi();
    }

    @Override
    public void onCreateFilter( Filter filter ) {
        fs.addChild( filter );
        refreshFilterViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fs.close();
    }

    private void fetchItems( Filter filter ) {
        if( itemRequest != null )
            itemRequest.cancel( true );
        ( itemRequest = new InitItemsListTask( filter.getFilterText(), credential, this ) )
                                .execute();
    }

    private void refreshFilterViews() {
        if( fs.isRoot() ) {
            backButton.setEnabled( false );
            homeButton.setEnabled( false );
            currentFilterTitle.setText( "" );
            currentFilterIcon.setImageResource( 0 );
        } else {
            currentFilterIcon.setImageResource( fs.getCurrentFilter().getIconResource() );
            currentFilterTitle.setText( fs.getCurrentFilter().getFileTitle() );
            backButton.setEnabled( true );
            homeButton.setEnabled( true );
        }
        current.removeAllViews();
        LinkedList< Filter > currentFilters = fs.currentFilters();
        for ( Filter child : currentFilters ) {
            LinearLayout item = ( LinearLayout ) getLayoutInflater()
                    .inflate( R.layout.filter_item_layout, current, false );
            ( ( ImageView ) item.findViewById( R.id.filter_image ) )
                    .setImageResource( child.getIconResource() );
            ( ( TextView ) item.findViewById( R.id.filter_title ) )
                    .setText( child.getFileTitle() );
            item.setOnClickListener( new FilterClickListener( child ) );
            current.addView( item );
        }
    }

    private void populatePicker() {
        Account[] accounts = AccountManager.get( this ).getAccounts();
        HashMap< String, Account > accountMap = new HashMap< String, Account >();
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Spinner spinner = ( Spinner ) findViewById( R.id.account_lister );
        for ( Account account : accounts ) {
            if ( emailPattern.matcher( account.name ).matches() )
                accountMap.put( account.name, account );
        }

        Set< String > keys = accountMap.keySet();
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item,
                keys.toArray( new String[ keys.size() ] ) );
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter( spinnerArrayAdapter );
    }

    private void getResultsFromApi() {
        credential.setSelectedAccountName( ( String ) spinner.getSelectedItem() );
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (! isDeviceOnline()) {
            Toast.makeText( this, "No network connection available.", Toast.LENGTH_LONG ).show();
        } else {
            fetchItems( fs.getCurrentFilter() );
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
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
                        credential.setSelectedAccountName(accountName);
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

    void showGooglePlayServicesAvailabilityErrorDialog( int connectionStatusCode ) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private class FetchFSTask extends AsyncTask< Void, Void, FileSystem > {

        private Context context;

        public FetchFSTask( Context context ) {
            this.context = context;
        }

        @Override
        protected FileSystem doInBackground( Void... params ) {
            FileSystem fs = new FileSystem( null );
            return fs;
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void onPostExecute( FileSystem output ) {
            refreshFilterViews();
        }

        @Override
        protected void onCancelled() {
            Toast.makeText( context, "Unable to retrieve recent filters..."
                    , Toast.LENGTH_LONG ).show();
        }

    }

    private class InitItemsListTask extends AsyncTask<Void, Void, List<Item> > {

        private Context context;
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private String query;

        public InitItemsListTask( String query, GoogleAccountCredential credential, Context context ) {
            this.context = context;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential )
                    .setApplicationName( "GmailFS" )
                    .build();
            this.query = query;
        }

        @Override
        protected List<Item> doInBackground(Void... params) {

            try {
                String user = "me";
                LinkedList< Item > items = new LinkedList< Item >();
                ListMessagesResponse response = mService.users().messages().list( user )
                                                        .setMaxResults( ITEMS_PER_REQUEST ).setQ( query ).execute();

                nextPage = startPage = response.getNextPageToken();
                for( Message res : response.getMessages() )
                    items.add( new Item( mService.users().messages()
                            .get( user, res.getId() ).execute() ) );
                return items;
            } catch ( Exception e ) {
                mLastError = e;
                cancel( true );
                return null;
            }
        }

        @Override
        protected void onPreExecute() { progress.setVisibility( View.VISIBLE ); }

        @Override
        protected void onPostExecute( List<Item> output ) {
            progress.setVisibility( View.GONE );
            if ( output == null || output.size() == 0 ) {
                Toast.makeText( context, "No results found.", Toast.LENGTH_LONG ).show();
            } else {
                userItems = new LinkedList( output );
                adapter = new ItemListAdapter( userItems, context );
                itemList.setAdapter( adapter );
                itemList.setOnScrollListener( adapter );
                if ( nextPage != null ) {
                    adapter.enableListener();
                    Log.d("called from " + fs.getCurrentFilter().getFileTitle(), "enabled");
                } else {
                    adapter.disableListener();
                    Log.d("called from " + fs.getCurrentFilter().getFileTitle(), "disabled");
                }
            }
        }

        @Override
        protected void onCancelled() {
            adapter.disableListener();
            if ( mLastError != null ) {
                if ( mLastError instanceof GooglePlayServicesAvailabilityIOException ) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if ( mLastError instanceof UserRecoverableAuthIOException ) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText( context, "Error fetching related items.", Toast.LENGTH_LONG ).show();
                }
            } else {
                Toast.makeText( context, "Request cancelled.", Toast.LENGTH_LONG ).show();
            }
        }

    }

    private class FetchMoreItemsTask extends AsyncTask<Void, Void, List<Item> > {

        private Context context;
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private String query;

        public FetchMoreItemsTask( String query, GoogleAccountCredential credential, Context context ) {
            this.context = context;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential )
                    .setApplicationName( "GmailFS" )
                    .build();
            this.query = query;
        }

        @Override
        protected List<Item> doInBackground(Void... params) {
            try {
                LinkedList< Item > items = new LinkedList< Item >();
                ListMessagesResponse response = mService.users().messages().list( "me" )
                                                    .setMaxResults( ITEMS_PER_REQUEST ).setQ( query )
                                                    .setPageToken( nextPage ).execute();

                nextPage = response.getNextPageToken();
                for ( Message res : response.getMessages() )
                    items.add( new Item( mService.users().messages()
                            .get( "me", res.getId() ).execute() ) );
                return items;
            } catch ( Exception e ) {
                mLastError = e;
                cancel( true );
                return null;
            }
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void onPostExecute( List<Item> output ) {
            if ( output == null || output.size() == 0 ) {
                Toast.makeText( context, "No results found.", Toast.LENGTH_LONG ).show();
            } else {
                if( nextPage == null )
                    adapter.disableListener();
                else {
                    userItems.addAll( output );
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        protected void onCancelled() {
            if ( mLastError != null ) {
                if ( mLastError instanceof GooglePlayServicesAvailabilityIOException ) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if ( mLastError instanceof UserRecoverableAuthIOException ) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText( context, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG ).show();
                }
            } else {
                Toast.makeText( context, "Request cancelled.", Toast.LENGTH_LONG ).show();
            }
        }

    }

    private class FetchRecentTask extends AsyncTask< Void, Void, List< Filter > > {

        private Context context;

        public FetchRecentTask( Context context ) {
            this.context = context;
        }

        @Override
        protected List< Filter > doInBackground( Void... params ) {
            LinkedList< Filter > filters = new LinkedList< Filter >();
            return filters;
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void onPostExecute( List< Filter > output ) {
            refreshFilterViews();
        }

        @Override
        protected void onCancelled() {
            Toast.makeText( context, "Encounter problem retrieving recent filters..."
                                        , Toast.LENGTH_LONG ).show();
        }

    }

    public class AddBinClickListener implements View.OnClickListener {

        private FragmentManager calling;

        public AddBinClickListener( FragmentManager calling ) {
            this.calling = calling;
        }

        @Override
        public void onClick( View v ) {
            new AddBinDialog().show( calling, "add_filter" );
        }
    }

    public class AddFilterClickListener implements View.OnClickListener {

        private FragmentManager calling;

        public AddFilterClickListener( FragmentManager calling ) {
            this.calling = calling;
        }

        @Override
        public void onClick( View v ) {
            new AddFolderDialog().show( calling, "add_filter" );
        }
    }

    public class AddItemClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent sendIntent = new Intent( Intent.ACTION_VIEW );
            sendIntent.setType( "plain/text" );
            sendIntent.setData( Uri.parse( ( String ) spinner.getSelectedItem() ) );
            sendIntent.setClassName( "com.google.android.gm", "com.google.android.gm.ComposeActivityGmail" );
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String [] { ( String ) spinner.getSelectedItem() } );
            startActivity(sendIntent);
        }
    }

    public class BackFilterListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            fs.back();
            fetchItems( fs.getCurrentFilter() );
            refreshFilterViews();
        }
    }

    public class FilterClickListener implements View.OnClickListener, View.OnTouchListener {

        private Filter filter;

        public FilterClickListener( Filter filter ) {
            this.filter = filter;
        }

        public void onClick( View v ) {
            fetchItems( filter );
            fs.forward( filter.getFileTitle() );
            refreshFilterViews();
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    }

    public class HomeFilterListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            fs.root();
            fetchItems( fs.getCurrentFilter() );
            refreshFilterViews();
        }
    }

    public class ItemListAdapter extends BaseAdapter implements AbsListView.OnScrollListener {

        private boolean loading = true, enabled = false;
        private Context context;
        private int visibleThreshold = 15;
        private int previousTotal = 0;
        private LinkedList< Item > items;

        public ItemListAdapter( LinkedList< Item > items, Context context ) {
            this.context = context;
            this.items = items;
        }

        @Override
        public void onScroll( AbsListView view, int firstVisibleItem,
                              int visibleItemCount, int totalItemCount ) {
            if ( loading ) {
                if ( totalItemCount > previousTotal ) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if ( enabled && !loading && nextPage != null
                    && ( totalItemCount - visibleItemCount ) <= ( firstVisibleItem + visibleThreshold ) ) {
                new FetchMoreItemsTask( fs.getCurrentFilter().getFilterText(), credential, context ).execute();
                loading = true;
            }

        }

        @Override
        public void onScrollStateChanged( AbsListView view, int scrollState ) { }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Item getItem( int position ) { return items.get( position ); }

        @Override
        public long getItemId( int position ) { return position; }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            Item current = items.get( position );
            LinearLayout itemLayout = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.item_list_layout, parent, false);
            ( ( ImageView ) itemLayout.findViewById( R.id.list_item_icon ) )
                    .setImageResource( current.getIconResource() );
            ( ( TextView ) itemLayout.findViewById( R.id.list_item_label ) )
                    .setText( current.getHeaderValue( FilterKey.SUBJECT ) );
            ( ( TextView ) itemLayout.findViewById( R.id.list_item_sublabel ) )
                    .setText( current.getHeaderValue( FilterKey.SENDER ) );
            return itemLayout;
        }

        public void enableListener() {
            listProgress.setVisibility( View.VISIBLE );
            enabled = true;
        }

        public void disableListener() {
            listProgress.setVisibility( View.GONE );
            enabled = false;
        }

    }

}
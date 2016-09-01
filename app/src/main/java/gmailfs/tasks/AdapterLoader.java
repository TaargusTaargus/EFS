package gmailfs.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.util.LinkedList;
import java.util.List;

import gmailfs.adapter.ProgressListAdapter;
import gmailfs.framework.AppContext;
import gmailfs.framework.File;

public class AdapterLoader extends AsyncTask< String, Void, List< File > > {

    private static final long ITEMS_PER_REQUEST = 20l;

    private Context context;
    private Exception mLastError = null;
    private Gmail mService = null;
    private ProgressListAdapter adapter;

    public AdapterLoader( ProgressListAdapter adapter, Context context ) {
        this.context = context;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new Gmail.Builder(
                transport, jsonFactory, AppContext.credential )
                .setApplicationName( "GmailFS" )
                .build();
        this.adapter = adapter;
    }

    @Override
    protected List< File > doInBackground( String... query ) {

        try {
            String user = "me";
            LinkedList< File > items = new LinkedList();
            ListMessagesResponse response;
            if( query.length > 0 && query[ 0 ] != null )
                response = mService.users().messages().list( user )
                        .setMaxResults( ITEMS_PER_REQUEST ).setQ( query[ 0 ] )
                        .setPageToken( adapter.nextPage ).execute();
            else
                response = mService.users().messages().list( user )
                        .setMaxResults( ITEMS_PER_REQUEST ).setPageToken( adapter.nextPage )
                        .execute();

            adapter.nextPage = response.getNextPageToken();
            for( Message res : response.getMessages() )
                items.add(
                        File.FileFactory.parse( mService.users().messages().get( "me", res.getId() ).execute() )
                );
            return items;
        } catch ( Exception e ) {
            Log.d("ERROR", e + "");
            mLastError = e;
            cancel( true );
            return null;
        }
    }

    @Override
    protected void onPostExecute( List< File > output ) {
        if ( output == null || output.size() == 0 ) {
            Toast.makeText( context, "No results found.", Toast.LENGTH_LONG ).show();
        } else {
            AppContext.fs.addFiles( output );
            if( adapter.nextPage != adapter.startPage )
                adapter.addFiles( output );
        }
    }

    @Override
    protected void onCancelled() {
        if ( mLastError != null )
                Toast.makeText(context, "The following error occurred:\n"
                        + mLastError.getMessage(), Toast.LENGTH_LONG).show();
    }

}

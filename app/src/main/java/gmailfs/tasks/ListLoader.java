package gmailfs.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import gmailfs.adapter.ProgressListAdapter;
import gmailfs.framework.AppContext;
import gmailfs.framework.File;
import gmailfs.view.ProgressListView;
import pronus.gmailfs.MainActivity;

public class ListLoader extends AsyncTask< String, Void, List< File > > {

    private static final long ITEMS_PER_REQUEST = 50l;

    private Activity context;
    private Exception mLastError = null;
    private Gmail mService = null;
    private ProgressListView list;
    private String nextPage = null;

    public ListLoader( ProgressListView list, Activity context ) {
        this.context = context;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new Gmail.Builder(
                transport, jsonFactory, AppContext.credential )
                .setApplicationName( "GmailFS" )
                .build();
        this.list = list;
    }

    @Override
    protected List< File > doInBackground( String... query ) {

        try {
            HashMap< String, File > db = AppContext.fs.currentFiles();
            String q = ( query.length > 0 && query[ 0 ] != null ) ? query[ 0 ] : "";
            ListMessagesResponse response = mService.users().messages().list( "me" )
                    .setMaxResults( ITEMS_PER_REQUEST )
                    .setQ( q )
                    .execute();

            if( response.getResultSizeEstimate() == 0l )
                return null;

            LinkedList< File > files = new LinkedList();
            for( Message res : response.getMessages() ) {
                if( ! db.containsKey( res.getId() ) ) {
                    File newFile = File.FileFactory.parse( mService.users().messages()
                                            .get( "me", res.getId() ).execute() );
                    files.add( newFile );
                    AppContext.fs.addFile( newFile );
                }
                else
                    files.add( db.get( res.getId() ) );
            }

            if( files.size() < ITEMS_PER_REQUEST ) {
                nextPage = null;
                Log.d("NEXTPAGE","NEXT");
            } else
                nextPage = response.getNextPageToken();
            return files;
        } catch ( Exception e ) {
            e.printStackTrace();
            mLastError = e;
            cancel( true );
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        list.showProgress();
    }

    @Override
    protected void onPostExecute( List< File > output ) {
        list.showList();
        if ( output == null || output.size() == 0 ) {
            Toast.makeText( context, "No results found.", Toast.LENGTH_LONG ).show();
            list.showEmpty();
        } else {
            ProgressListAdapter adapter = new ProgressListAdapter( output, context );
            adapter.startPage = adapter.nextPage = nextPage;
            list.attachAdapter( adapter );
        }
    }

    @Override
    protected void onCancelled() {
        if ( mLastError instanceof UserRecoverableAuthIOException) {
            context.startActivityForResult(
                    ( ( UserRecoverableAuthIOException ) mLastError ).getIntent(),
                        MainActivity.REQUEST_AUTHORIZATION);
        } else if ( mLastError != null )
                    Toast.makeText(context, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
    }

}

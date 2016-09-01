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
import gmailfs.framework.FileDB;
import gmailfs.view.ProgressListView;

public class ListLoader extends AsyncTask< String, Void, List< File > > {

    private static final long MAX_ITEMS_IN_DB = 200l;
    private static final long ITEMS_PER_REQUEST = 20l;

    private Context context;
    private Exception mLastError = null;
    private Gmail mService = null;
    private int dbEntries;
    private ProgressListView list;
    private String nextPage = null;

    public ListLoader( ProgressListView list, Context context ) {
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
            LinkedList< File > items = new LinkedList();
            String q = ( query.length > 0 && query[ 0 ] != null ) ? query[ 0 ] : "";
            ListMessagesResponse response = mService.users().messages().list( "me" )
                    .setMaxResults( ITEMS_PER_REQUEST )
                    .setQ( q )
                    .execute();

            if( response.getResultSizeEstimate() == 0l )
                return items;

            for( Message res : response.getMessages() )
                items.addFirst(
                        File.FileFactory.parse( mService.users().messages().get( "me", res.getId() ).execute() )
                );

            if( items.size() < ITEMS_PER_REQUEST )
                nextPage = null;
            else
                nextPage = response.getNextPageToken();

            return items;
        } catch ( Exception e ) {
            e.printStackTrace();
            mLastError = e;
            cancel( true );
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        dbEntries = AppContext.fs.totalFilesInDB();
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
        if ( mLastError != null )
                Toast.makeText(context, "The following error occurred:\n"
                        + mLastError.getMessage(), Toast.LENGTH_LONG).show();
    }

}
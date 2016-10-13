package gmailfs.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import gmailfs.adapter.ProgressListAdapter;
import gmailfs.framework.AppContext;
import gmailfs.framework.File;

public class FileLoader extends AsyncTask< String, Void, String > {

    private static final String LOADING_MESSAGE = "Retrieving message contents...";

    private Context context;
    private Exception mLastError;
    private Gmail service = null;
    private ProgressDialog progress;
    private TextView container;

    public FileLoader( TextView container, Context context ) {
        this.container = container;
        this.service = new Gmail.Builder(
                                            AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(),
                                            AppContext.credential
                                        )
                                .setApplicationName( "GmailFS" )
                                .build();
        this.progress = new ProgressDialog( context );
    }

    @Override
    protected void onPreExecute() {
        progress.setMessage( LOADING_MESSAGE );
        progress.show();
    }

    @Override
    protected String doInBackground( String... id) {
        try {
            if( id == null )
                throw new IllegalArgumentException( "message id was not included" );
            return StringUtils.newStringUtf8(
                        Base64.decodeBase64(
                                service.users().messages().get( "me", id[ 0 ] ).execute().getPayload().toPrettyString()
                        )
            );
        } catch ( Exception e ) {
            Log.d("ERROR", e + "");
            mLastError = e;
            cancel( true );
            return null;
        }
    }

    @Override
    protected void onPostExecute( String output ) {
        progress.hide();
        container.setText( output );
    }

    @Override
    protected void onCancelled() {
        progress.hide();
        if ( mLastError != null )
                Toast.makeText( context, "Was unable to retrieve message body.", Toast.LENGTH_LONG ).show();
    }

}

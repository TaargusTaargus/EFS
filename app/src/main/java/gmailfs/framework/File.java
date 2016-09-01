package gmailfs.framework;

import java.text.ParseException;
import android.util.Log;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pronus.gmailfs.R;

public class File {

    public final static String DATE_HEADER_KEY = "Date";
    public final static String SUBJECT_HEADER_KEY = "Subject";
    public final static String SENDER_HEADER_KEY = "From";
    public final static int ITEM_RESOURCE_ICON = R.drawable.item_icon;

    private String id, sender, subject, timestamp;

    public File( String id, String sender, String subject, String timestamp ) {
        this.id = id;
        this.sender = sender;
        this.subject = subject;
        this.timestamp = timestamp;
    }

    public Integer getIconResource() { return File.ITEM_RESOURCE_ICON; }
    public String getFileID() { return id; }
    public String getSender() { return sender; }
    public String getSubject() { return subject; }
    public String getTimestamp() { return timestamp; }

    public static class FileFactory {

        public final static SimpleDateFormat in = new SimpleDateFormat("dd MMM yyyy kk:mm:ss");
        public final static SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

        public static File parse( Message message ) throws ParseException {
            HashMap< String, String > headers = new HashMap();
            for( MessagePartHeader header : message.getPayload().getHeaders() )
                headers.put( header.getName(), header.getValue() );
            Pattern pat = Pattern.compile( "([0-9]?[0-9] [A-Z][a-z][a-z] \\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d)" );
            Matcher m = pat.matcher( headers.get( "Date" ) );
            if( m.find() ) {
                String tmp = m.group();
                return new File(message.getId(), headers.get(File.SENDER_HEADER_KEY),
                        headers.get( File.SUBJECT_HEADER_KEY ),
                        out.format(in.parse(tmp)));
            } else
                throw new ParseException( "Unable to parse email timestamp.", 0 );
        }

    }

}
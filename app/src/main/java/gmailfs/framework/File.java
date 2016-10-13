package gmailfs.framework;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import pronus.gmailfs.R;

public class File implements Serializable {

    public final static String DATE_HEADER_KEY = "Date";
    public final static String SUBJECT_HEADER_KEY = "Subject";
    public final static String SENDER_HEADER_KEY = "From";
    public final static int ITEM_RESOURCE_ICON = R.drawable.item_icon;

    private String fileId, sender, subject, timestamp;

    public File( String id, String sender, String subject, String timestamp ) {
        this.fileId = id;
        this.sender = sender;
        this.subject = subject;
        this.timestamp = timestamp;
    }

    public Integer getIconResource() { return File.ITEM_RESOURCE_ICON; }
    public String getFileID() { return fileId; }
    public String getSender() { return sender; }
    public String getSubject() { return subject; }
    public String getTimestamp() { return timestamp; }

    public static class FileFactory {

        public final static SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

        public static File parse( Message message ) throws ParseException, IOException {
            HashMap< String, String > headers = new HashMap();
            for( MessagePartHeader header : message.getPayload().getHeaders() )
                headers.put( header.getName(), header.getValue() );
            String sender = headers.get( File.SENDER_HEADER_KEY );
            String subject = headers.get( File.SUBJECT_HEADER_KEY );
            return new File( message.getId(),
                                ( sender != null && sender != "" ) ? sender : "( unknown sender )",
                                ( subject != null && subject != "" ) ? subject : "( no subject )",
                                    out.format( new Date( message.getInternalDate() ) ) );
        }

    }

}
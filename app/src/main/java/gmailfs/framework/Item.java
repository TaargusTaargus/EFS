package gmailfs.framework;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.util.HashMap;

import gmailfs.framework.Filter.FilterKey;
import gmailfs.base.File;
import pronus.gmailfs.R;

public class Item implements File {

    public final static int ITEM_RESOURCE_ICON = R.drawable.item_icon;

    private Message message;
    private HashMap<String, String> headers = new HashMap<String, String>();

    public Item( Message message ) {
        this.message = message;
        for( MessagePartHeader header : message.getPayload().getHeaders() )
            headers.put( header.getName(), header.getValue() );
    }

    public Integer getIconResource() { return Item.ITEM_RESOURCE_ICON; }
    public String getHeaderValue( FilterKey key ) { return headers.get( key.getHeaderKeyword() ); }
    public String getFileTitle() { return message.getId(); }

}
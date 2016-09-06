package gmailfs.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import gmailfs.framework.File;
import pronus.gmailfs.R;

public class ProgressListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List< File > items;
    public String startPage, nextPage, query;

    public ProgressListAdapter( List< File > items, Context context ) {
        this.inflater = ( LayoutInflater ) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        this.items = items;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public File getItem( int position ) { return items.get( position ); }

    @Override
    public long getItemId( int position ) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent ) {
        File current = items.get( position );
        LinearLayout itemLayout = ( LinearLayout ) inflater
                .inflate( R.layout.item_list_layout, parent, false );
        ( ( ImageView ) itemLayout.findViewById( R.id.list_item_icon ) )
                .setImageResource( current.getIconResource() );
        ( ( TextView ) itemLayout.findViewById( R.id.list_item_label ) )
                .setText( current.getSubject() );

        String date = new SimpleDateFormat( "yyyyMMddHHmmss" ).format( new Date() );
        long diff = Long.parseLong( date )
                        - Long.parseLong( current.getTimestamp().replace( "-", "" ).replace( " ","" ).replace( ":", "" ) );
        if( diff >= 10000000000l )
            date = current.getTimestamp().substring( 5, 7 ) + "-" + current.getTimestamp().substring( 0, 4 );
        else if( diff >= 1000000l )
            date = current.getTimestamp().substring( 5, 10 );
        else
            date = current.getTimestamp().substring( 11, 16 );

        ( ( TextView ) itemLayout.findViewById( R.id.list_item_date ) )
                .setText( date );
        ( ( TextView ) itemLayout.findViewById( R.id.list_item_sublabel ) )
                .setText( current.getSender() );
        return itemLayout;
    }

    public void addFiles( List< File > files ) {
        items.addAll( files );
        notifyDataSetChanged();
    }

}

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
        ( ( TextView ) itemLayout.findViewById( R.id.list_item_sublabel ) )
                .setText( current.getSender() );
        return itemLayout;
    }

    public void addFiles( List< File > files ) {
        items.addAll( files );
        notifyDataSetChanged();
    }

}

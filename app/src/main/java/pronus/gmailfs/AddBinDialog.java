package pronus.gmailfs;

import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import gmailfs.framework.Filter;
import gmailfs.framework.Filter.FilterKey;
import gmailfs.framework.Filter.FilterType;

public class AddBinDialog extends AddFilterDialog {

    public Filter constructFilter( String title, String [] terms ) {
        return new Filter( TextUtils.join( " OR ", terms ), title,
                            FilterKey.SUBJECT, FilterType.BIN );
    }

    public LinearLayout title() {
        LinearLayout title = ( LinearLayout ) getActivity().getLayoutInflater()
                                            .inflate( R.layout.add_filter_dialog_title_layout, null );
        ( ( ImageView ) title.findViewById( R.id.add_item_dialog_icon ) )
                .setImageResource( Filter.BIN_RESOURCE_ICON);
        ( ( TextView ) title.findViewById( R.id.add_item_dialog_title ) )
                .setText( R.string.add_bin_dialog_title );
        return title;
    }

}

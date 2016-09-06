package gmailfs.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import gmailfs.adapter.ProgressListAdapter;
import gmailfs.tasks.AdapterLoader;
import pronus.gmailfs.R;

public class ProgressListView {

    private boolean hasFooter;
    private Context context;
    private LinearLayout footerProgress, listProgress, empty, parent;
    private ListView list;

    public ProgressListView( LinearLayout container, Context context ) {
        this.context = context;
        LayoutInflater inflater = ( LayoutInflater )
                                        context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        footerProgress = ( LinearLayout ) inflater.inflate( R.layout.footer_progress_layout, null );
        parent = ( LinearLayout ) inflater.inflate( R.layout.list_progress_layout, container, false );
        empty = ( LinearLayout ) parent.findViewById( R.id.empty_layout );
        listProgress = ( LinearLayout ) parent.findViewById( R.id.progress_layout );
        list = ( ListView ) parent.findViewById( R.id.results_list );

        container.addView( parent );
    }

    public void showEmpty() {
        empty.setVisibility( View.VISIBLE );
        list.setVisibility( View.GONE );
        listProgress.setVisibility( View.GONE );
    }

    public void showList() {
        empty.setVisibility( View.GONE );
        listProgress.setVisibility( View.GONE );
        list.setVisibility( View.VISIBLE );
    }

    public void showProgress() {
        listProgress.setVisibility( View.VISIBLE );
        list.setVisibility( View.GONE );
        empty.setVisibility( View.GONE );
    }

    public void attachAdapter( ProgressListAdapter adapter ) {
        if( ! hasFooter ) {
            list.addFooterView( footerProgress );
            hasFooter = true;
        }
        list.setAdapter( adapter );
        list.setOnScrollListener( new ListScrollListener( adapter ) );
    }

    public class ListScrollListener implements AbsListView.OnScrollListener {

        private boolean loading = true;
        private int visibleThreshold = 15;
        private int previousTotal = 0;
        private ProgressListAdapter adapter;

        public ListScrollListener( ProgressListAdapter adapter ) { this.adapter = adapter; }

        @Override
        public void onScroll( AbsListView view, int firstVisibleItem,
                              int visibleItemCount, int totalItemCount ) {
            if ( loading ) {
                if ( totalItemCount > previousTotal ) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if( adapter.nextPage == null ) {
                list.removeFooterView( footerProgress );
                hasFooter = false;
            }
            else {
                if ( !loading && ( totalItemCount - visibleItemCount ) <= ( firstVisibleItem + visibleThreshold ) ) {
                    new AdapterLoader( adapter, context ).execute( adapter.query );
                    loading = true;
                }
            }

        }

        @Override
        public void onScrollStateChanged( AbsListView view, int scrollState ) { }

    }

}
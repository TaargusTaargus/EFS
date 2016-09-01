package pronus.gmailfs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SearchView;

import gmailfs.framework.AppContext;
import gmailfs.framework.Filter;
import gmailfs.tasks.ListLoader;
import gmailfs.view.ProgressListView;

public class SearchDialog extends DialogFragment {

    private ListLoader loader;
    private ProgressListView results;
    private SearchView search;
    private String baseQ = AppContext.fs.getPath().getFilterPath();

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity );
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                                            new ContextThemeWrapper( getActivity(), R.style.FullHeightDialog )
        );
        LinearLayout layout = ( LinearLayout ) getActivity().getLayoutInflater()
                                            .inflate( R.layout.search_dialog_layout, null );
        results = new ProgressListView( ( LinearLayout ) layout.findViewById( R.id.search_results_container ),
                                                getActivity() );
        search = ( SearchView ) layout.findViewById( R.id.search_messages_view );
        ( loader = new ListLoader( results, getActivity() ) ).execute( baseQ );

        search.setOnQueryTextListener( new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit( String query ) {
                if( loader != null )
                    loader.cancel( true );
                ( loader = new ListLoader( results, getActivity() ) ).execute( baseQ + " " + query );
                return true;
            }

            @Override
            public boolean onQueryTextChange( String newText ) {
                return false;
            }
        });

        Dialog dialog = builder.setView( layout )
                .setPositiveButton( R.string.save_filter_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        FilterDialog add = new FilterDialog();
                        add.insertFilterHints( new Filter( search.getQuery().toString(),
                                                                search.getQuery().toString(),
                                                                Filter.FilterKey.SUBJECT ) );
                        add.show( getFragmentManager(), "add_filter" );
                    }
                })
                .setNegativeButton( R.string.cancel_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })
                .create();

        // fix for bug with KitKat dialogs
        dialog.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                        WindowManager.LayoutParams.FLAG_FULLSCREEN );
        return dialog;
    }

}
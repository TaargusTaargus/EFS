package pronus.gmailfs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.LinkedList;

import gmailfs.base.AddFilterListener;
import gmailfs.framework.Filter;

public abstract class AddFilterDialog extends DialogFragment {

    public abstract LinearLayout title();
    public abstract Filter constructFilter( String title, String [] terms );

    private AddFilterListener listener;
    private EditText filterTitle;
    private LinearLayout termList;
    private LinkedList< EditText > terms = new LinkedList();
    private LinkedList< ImageButton > buttons = new LinkedList();

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity );
        try {
            listener = ( AddFilterListener ) activity;
        } catch ( ClassCastException e ) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {

        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        LinearLayout layout = ( LinearLayout ) getActivity().getLayoutInflater()
                                            .inflate( R.layout.add_filter_dialog_layout, null );
        filterTitle = ( EditText ) layout.findViewById( R.id.add_filter_title_input );
        termList = ( LinearLayout ) layout.findViewById( R.id.term_list_layout );
        ImageButton addButton = ( ImageButton ) layout.findViewById( R.id.add_term_button );
        addButton.setOnClickListener( new AddTermListener( termList ) );
        addButton.callOnClick();
        builder.setView( layout )
                .setCustomTitle( title() )
                // Add action buttons
                .setPositiveButton( R.string.save_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        String [] text = new String[ terms.size() ];
                        for( int i = 0; i < text.length; i++ )
                            text[ i ] = terms.get( i ).getText().toString();
                        listener.onCreateFilter( constructFilter( filterTitle.getText().toString(), text ) );
                    }
                })
                .setNegativeButton( R.string.cancel_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        cancel();
                    }
                });
        return builder.create();
    }

    private void cancel() {
        dismiss();
    }

    private void refresh() {
        if( terms.size() == 1 )
            buttons.getFirst().setVisibility( View.GONE );
        else
            buttons.getFirst().setVisibility( View.VISIBLE );
    }

    public class AddTermListener implements View.OnClickListener {

        private LinearLayout termList;

        public AddTermListener( LinearLayout termList ) { this.termList = termList; }

        @Override
        public void onClick( View v ) {
            LinearLayout term = ( LinearLayout ) getActivity().getLayoutInflater()
                                                        .inflate( R.layout.filter_term_layout, termList, false );
            EditText label = ( EditText ) term.findViewById( R.id.term_label );
            ImageButton button = ( ImageButton ) term.findViewById( R.id.remove_term_button );
            button.setOnClickListener( new RemoveTermListener( label, term, termList ) );
            terms.add( label );
            buttons.add( button );
            termList.addView( term );
            refresh();
        }

    }

    public class RemoveTermListener implements View.OnClickListener {

        private EditText termText;
        private LinearLayout term, termList;

        public RemoveTermListener( EditText termText, LinearLayout term, LinearLayout termList ) {
            this.termText = termText;
            this.term = term;
            this.termList = termList;
        }

        @Override
        public void onClick( View v ) {
            buttons.remove( terms.indexOf( termText ) );
            terms.remove( termText );
            termList.removeView( term );
            refresh();
        }

    }

}
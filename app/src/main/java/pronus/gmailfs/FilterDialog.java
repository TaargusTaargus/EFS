package pronus.gmailfs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;

import gmailfs.base.FilterDialogListener;
import gmailfs.framework.Filter;

public class FilterDialog extends DialogFragment {

    private FilterDialogListener listener;
    private boolean edit = false;
    private EditText filterTitle;
    private Filter existing;
    private LinearLayout termList;
    private LinkedList< EditText > terms = new LinkedList();
    private LinkedList< ImageButton > buttons = new LinkedList();

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity );
        try {
            listener = (FilterDialogListener) activity;
        } catch ( ClassCastException e ) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {

        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        LinearLayout title = ( LinearLayout ) getActivity().getLayoutInflater()
                .inflate( R.layout.add_filter_dialog_title_layout, null );
        ( (ImageView) title.findViewById( R.id.add_item_dialog_icon ) )
                .setImageResource( Filter.FOLDER_RESOURCE_ICON);
        ( (TextView) title.findViewById( R.id.add_item_dialog_title ) )
                .setText( R.string.add_filter_dialog_title );

        LinearLayout layout = ( LinearLayout ) getActivity().getLayoutInflater()
                                            .inflate( R.layout.add_filter_dialog_layout, null );
        filterTitle = ( EditText ) layout.findViewById( R.id.add_filter_title_input );
        if( existing != null )// && existing.getFilterID() != null )
            filterTitle.setText( existing.getFilterID() );

        termList = ( LinearLayout ) layout.findViewById( R.id.term_list_layout );
        ImageButton addButton = ( ImageButton ) layout.findViewById( R.id.add_term_button );
        addButton.setOnClickListener( new AddTermListener( termList ) );
        addButton.callOnClick();

        if( existing != null )// && existing.getFilterText() != null )
            terms.getFirst().setText( existing.getFilterText() );


        builder.setView( layout )
                .setCustomTitle( title )
                .setPositiveButton( R.string.save_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        String [] text = new String[ terms.size() ];
                        for( int i = 0; i < text.length; i++ )
                            text[ i ] = terms.get( i ).getText().toString();

                        Filter nf = constructFilter( filterTitle.getText().toString(), text );
                        if( !edit )
                            listener.onCreateFilter( nf );
                        else
                            listener.onEditFilter( existing, nf );
                    }
                })
                .setNegativeButton( R.string.cancel_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if( existing == null || !edit )
                            dismiss();
                        else
                            listener.onCreateFilter( constructFilter( existing.getFilterID(),
                                                                        new String [] { existing.getFilterText() } ) );
                    }
                });
        Dialog dialog = builder.create();

        // fix for bug with KitKat dialogs
        dialog.getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN );

        return dialog;
    }

    private void refresh() {
        if( terms.size() == 1 )
            buttons.getFirst().setVisibility( View.GONE );
        else
            buttons.getFirst().setVisibility( View.VISIBLE );
    }

    private Filter constructFilter( String title, String [] terms ) {
        return new Filter( TextUtils.join( " ", terms ), title, Filter.FilterKey.SUBJECT );
    }

    public void insertFilterHints( Filter filter ) { this.existing = filter; }
    public void insertFilterToEdit( Filter filter ) {
        this.edit = true;
        this.existing = filter;
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
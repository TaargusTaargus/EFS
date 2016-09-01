package pronus.gmailfs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public abstract class WarningDialog extends DialogFragment {

    public abstract String getWarningMessage();
    public abstract String getConfirmationMessage();
    public abstract void onEvent();

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {

        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );

        LinearLayout layout = ( LinearLayout ) getActivity().getLayoutInflater()
                                                .inflate( R.layout.warning_dialog_layout, null );
        ( ( TextView ) layout.findViewById( R.id.warning_label_message ) ).setText( getWarningMessage() );
        builder.setView( layout )
                .setPositiveButton( R.string.okay_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        onEvent();
                        Toast.makeText( getActivity(), getConfirmationMessage(), Toast.LENGTH_LONG );
                    }
                })
                .setNegativeButton( R.string.cancel_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }

}
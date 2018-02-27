package us.cognice.secrets.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import us.cognice.secrets.R;
import us.cognice.secrets.utils.Utils;

/**
 * Created by Kirill Simonov on 11.10.2017.
 */
public class MessageDialog extends DialogFragment {

    private String message;
    private LocationMessageListener listener;

    public static MessageDialog newInstance(String message) {
        MessageDialog f = new MessageDialog();
        Bundle args = new Bundle();
        args.putString("message", message);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        message = getArguments().getString("message");
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.message_dialog, null);
        final EditText edit = view.findViewById(R.id.dialogMessage);
        edit.setText(message);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Utils.hideKeyboard(view);
                        message = edit.getText().toString();
                        listener.messageChanged(message);
                    }
                })
                .setNegativeButton("Cancel", null);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (LocationMessageListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement LocationMessageListener");
        }
    }

    public interface LocationMessageListener {
        void messageChanged(String message);
    }
}

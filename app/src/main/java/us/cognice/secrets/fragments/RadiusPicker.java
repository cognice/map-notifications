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
import com.shawnlin.numberpicker.NumberPicker;
import us.cognice.secrets.R;

import java.util.Locale;

/**
 * Created by Kirill Simonov on 11.10.2017.
 */
public class RadiusPicker extends DialogFragment {

    private int radius;
    private RadiusPickerListener listener;

    public static RadiusPicker newInstance(int radius) {
        RadiusPicker f = new RadiusPicker();
        Bundle args = new Bundle();
        args.putInt("radius", radius);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        radius = getArguments().getInt("radius");
        if (radius == 0) radius = 5;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.radius_picker, null);
        final NumberPicker tp = view.findViewById(R.id.t_picker);
        final NumberPicker hp = view.findViewById(R.id.h_picker);
        final NumberPicker dp = view.findViewById(R.id.d_picker);
        final NumberPicker up = view.findViewById(R.id.u_picker);
        String value = String.format(Locale.ROOT, "%04d", radius);
        tp.setValue(Character.getNumericValue(value.charAt(0)));
        hp.setValue(Character.getNumericValue(value.charAt(1)));
        dp.setValue(Character.getNumericValue(value.charAt(2)));
        up.setValue(Character.getNumericValue(value.charAt(3)));
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String value = "" + tp.getValue() + hp.getValue() + dp.getValue() + up.getValue();
                        radius = Integer.parseInt(value);
                        listener.radiusChanged(radius);
                    }
                })
                .setNegativeButton("Cancel", null);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (RadiusPickerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement RadiusPickerListener");
        }
    }

    public interface RadiusPickerListener {
        void radiusChanged(int radius);
    }
}

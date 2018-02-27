package us.cognice.secrets.utils;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

/**
 * Created by Kirill Simonov on 13.10.2017.
 */
public class UnderscoreRemover implements View.OnFocusChangeListener {

    private FloatingActionButton[] buttons;
    private Runnable focusAction;

    public UnderscoreRemover(FloatingActionButton... buttons) {
        this.buttons = buttons;
    }

    public UnderscoreRemover(Runnable focusAction, FloatingActionButton... buttons) {
        this.focusAction = focusAction;
        this.buttons = buttons;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            if (focusAction != null) focusAction.run();
            v.getBackground().clearColorFilter();
            for (FloatingActionButton button : buttons) {
                button.hide();
            }
        } else {
            v.getBackground().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
        }
    }
}
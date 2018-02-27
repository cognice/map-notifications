package us.cognice.secrets.drawable;

/**
 * Created by Kirill Simonov on 08.10.2017.
 */
import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import de.hdodenhof.circleimageview.CircleImageView;
import us.cognice.secrets.MainActivity;
import us.cognice.secrets.R;
import us.cognice.secrets.fragments.ManageFragment;

@SuppressWarnings("unused")
public class AvatarBehavior extends CoordinatorLayout.Behavior<CircleImageView> {

    private MainActivity context;
    private float startYPosition, startXPosition, endYPosition, endXPosition;
    private boolean initialized = false;
    private float barSize;
    private int avaSize;
    private boolean appBarLocked;

    public AvatarBehavior(Context context, AttributeSet attrs) {
        this.context = (MainActivity) context;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarBehavior);
            startYPosition = a.getDimension(R.styleable.AvatarBehavior_startYPosition, 0);
            startXPosition = a.getDimension(R.styleable.AvatarBehavior_startXPosition, 0);
            endYPosition = a.getDimension(R.styleable.AvatarBehavior_endYPosition, 0);
            endXPosition = a.getDimension(R.styleable.AvatarBehavior_endXPosition, 0);
            a.recycle();
        }
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, CircleImageView child, View dependency) {
        return dependency instanceof Toolbar;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, CircleImageView child, View dependency) {
        if (!initialized) {
            barSize = dependency.getY();
            avaSize = child.getHeight();
            initialized = true;
        }
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (context != null && !context.getManageFragment().isAppBarLocked()) {
            float ratio = dependency.getY() / barSize;
            parent.findViewById(R.id.nickname).setAlpha(ratio * ratio);
            parent.findViewById(R.id.phone).setAlpha(ratio * ratio);
            child.setY(endYPosition + startYPosition * ratio);
            child.setX(endXPosition + startXPosition * ratio);
            lp.width = lp.height = (int) Math.max(100, avaSize * ratio);
        } else {
            //prevent resizing if AppBarLayout is locked
            parent.findViewById(R.id.nickname).setAlpha(1);
            parent.findViewById(R.id.phone).setAlpha(1);
            child.setY(endYPosition + startYPosition);
            child.setX(endXPosition + startXPosition);
            lp.width = lp.height = Math.max(100, avaSize);
        }
        child.setLayoutParams(lp);
        return true;
    }

    public boolean isAppBarLocked() {
        return appBarLocked;
    }

    public void setAppBarLocked(boolean appBarLocked) {
        this.appBarLocked = appBarLocked;
    }
}
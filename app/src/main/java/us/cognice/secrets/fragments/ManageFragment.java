package us.cognice.secrets.fragments;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.system.ErrnoException;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.google.android.gms.internal.ma;
import com.google.gson.Gson;
import com.theartofdev.edmodo.cropper.CropImage;
import de.hdodenhof.circleimageview.CircleImageView;
import us.cognice.secrets.LocationActivity;
import us.cognice.secrets.MainActivity;
import us.cognice.secrets.ManageListAdapter;
import us.cognice.secrets.R;
import us.cognice.secrets.data.Data;
import us.cognice.secrets.data.Location;
import us.cognice.secrets.drawable.AvatarBehavior;
import us.cognice.secrets.utils.UnderscoreRemover;
import us.cognice.secrets.utils.Utils;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static android.Manifest.permission.*;
import static android.app.Activity.RESULT_OK;
import static android.support.v4.content.PermissionChecker.checkSelfPermission;
import static com.theartofdev.edmodo.cropper.CropImageView.RequestSizeOptions.RESIZE_FIT;

public class ManageFragment extends Fragment implements WelcomeDialog.WelcomeDialogListener, ManageListAdapter.LocationListener, AppBarLayout.OnOffsetChangedListener {

    private static final String PHONE_REGEX = "^((\\+\\d{1,3}(-| )?\\(?\\d\\)?(-| )?\\d{1,3})|(\\(?\\d{2,3}\\)?))(-| )?(\\d{3,4})(-| )?(\\d{4})(( x| ext)\\d{1,5})?$";
    private Gson gson = new Gson();
    private FloatingActionButton fab, add;
    private ManageListAdapter placesAdapter;
    private CustomLayoutManager layoutManager;
    private AppBarLayout barLayout;
    private Switch locationServiceSwitch;
    private RecyclerView list;
    private View mainView;
    private Uri croppedImageUri;
    private boolean fabVisible = true;
    private Animation openAnimation;
    private Data data = new Data();
    private int barOffset;
    private boolean appBarLocked;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ManageFragment() {
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
    }

    @Override
    public void afterWelcome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (
                checkSelfPermission(getContext(), READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(getContext(), READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(getContext(), READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED)) {
            // request permissions and handle the result in onRequestPermissionsResult()
            requestPermissions(Build.VERSION.SDK_INT >= 26 ? new String[]{READ_SMS, READ_PHONE_STATE, READ_PHONE_NUMBERS} : new String[]{READ_SMS, READ_PHONE_STATE}, 1);
        } else {
            TelephonyManager tMgr = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String phone = tMgr.getLine1Number();
            if (!phone.matches(PHONE_REGEX)) phone = "";
            data.setPhone(phone);
            saveData();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.settings_fragment, container, false);
        fab = getActivity().findViewById(R.id.fab);
        add = mainView.findViewById(R.id.add);
        locationServiceSwitch = mainView.findViewById(R.id.locationServiceSwitch);
        barLayout = mainView.findViewById(R.id.app_bar_layout);
        barLayout.addOnOffsetChangedListener(this);
        add.hide();
        list = mainView.findViewById(R.id.list);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View button) {
                Intent i = new Intent(getActivity(), LocationActivity.class);
                i.putExtra(LocationActivity.NEW_LOCATION, true);
                startActivityForResult(i, LocationActivity.REQUEST_CODE);
            }
        });
        locationServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                data.setLocationServiceOn(isChecked);
                saveData();
                if (isChecked && !data.isLocationServiceRunning()) {
                    ((MainActivity) getActivity()).startLocationService();
                } else if (!isChecked && data.isLocationServiceRunning()) {
                    ((MainActivity) getActivity()).stopLocationService();
                }
            }
        });
        layoutManager = new CustomLayoutManager(getContext());
        list.setLayoutManager(layoutManager);
        return mainView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        data = ((MainActivity) getActivity()).getData();
        if (data == null) {
            data = new Data();
            new WelcomeDialog().show(getFragmentManager(), "WelcomeDialog");
        }
        //all initializations that need data go here
        locationServiceSwitch.setChecked(data.isLocationServiceOn());
        placesAdapter = new ManageListAdapter(data.getPlaces(), this);
        list.setAdapter(placesAdapter);
        EditText phoneInput = mainView.findViewById(R.id.phone);
        EditText nicknameInput = mainView.findViewById(R.id.nickname);
        phoneInput.setText(data.getPhone());
        nicknameInput.setText(data.getNickname());
        initEditText(nicknameInput);
        initEditText(phoneInput);
        ImageView avatar = mainView.findViewById(R.id.ava);
        if (data.getAvaPath() != null && !data.getAvaPath().isEmpty()) {
            setAvatar(data.getAvaPath());
        }
        avatar.setClickable(true);
        final ManageFragment fragment = this;
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setMinCropResultSize(100,100)
                        .setMaxCropResultSize(2000,2000)
                        .setRequestedSize((int) getResources().getDimension(R.dimen.image_width), (int) getResources().getDimension(R.dimen.image_height), RESIZE_FIT)
                        .start(getContext(), fragment);
            }
        });
        add.startAnimation(openAnimation);
        add.show();
    }


    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int dy = barOffset - verticalOffset;
        barOffset = verticalOffset;
        if (dy > 0 && fabVisible) {
            // scrolling up
            toggleButtons(false);
        } else if (dy < 0 && !fabVisible){
            // scrolling down
            toggleButtons(true);
        }
        lockAppBar();
    }

    public void lockAppBar() {
        if (barOffset == 0) {
            // if AppBar is expanded and all RecyclerView items are visible then lock AppBar
            appBarLocked = layoutManager.getItemCount() == 0 ||
                    viewIsVisible(layoutManager.findViewByPosition(layoutManager.getItemCount() - 1));
            layoutManager.setScrollEnabled(!appBarLocked);
        }
        if (appBarLocked) barLayout.setExpanded(true, false);
    }

    private boolean viewIsVisible(View view) {
        Rect scrollBounds = new Rect();
        list.getHitRect(scrollBounds);
        return view != null && view.getLocalVisibleRect(scrollBounds);
    }

    private void toggleButtons(boolean visible) {
        if (visible) {
            fab.show();
            add.show();
        } else {
            fab.hide();
            add.hide();
        }
        fab.setClickable(visible);
        add.setClickable(visible);
        fabVisible = visible;
    }

    private void initEditText(final EditText edit) {
        edit.getBackground().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
        edit.setOnFocusChangeListener(new UnderscoreRemover(fab, add));
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    edit.clearFocus();
                    Utils.hideKeyboard(edit);
                    if (edit.getId() == R.id.nickname) {
                        data.setNickname(edit.getText().toString());
                    } else if (edit.getId() == R.id.phone) {
                        data.setPhone(edit.getText().toString());
                    }
                    saveData();
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    fab.show();
                                    add.show();
                                }
                            }, 300);
                }
                return false;
            }
        });
    }

    private void saveData() {
        ((MainActivity) getActivity()).setData(data);
    }

    private void setAvatar(String path) {
        Bitmap myBitmap = BitmapFactory.decodeFile(path);
        CircleImageView circle = mainView.findViewById(R.id.ava);
        circle.setImageBitmap(myBitmap);
    }

    private boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getActivity().getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Exception while checking permission: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(intent);
            if (resultCode == RESULT_OK) {
                croppedImageUri = result.getUri();
                boolean requirePermissions = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                        isUriRequiresPermissions(result.getUri())) {
                    // request permissions and handle the result in onRequestPermissionsResult()
                    requirePermissions = true;
                    requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 0);
                }
                if (!requirePermissions) {
                    data.setAvaPath(croppedImageUri.getPath());
                    setAvatar(croppedImageUri.getPath());
                    saveData();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(getContext(), "Error while cropping image: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == LocationActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                boolean saved = intent.getBooleanExtra(LocationActivity.RESULT_SAVED, false);
                if (saved) {
                    boolean created = intent.getBooleanExtra(LocationActivity.NEW_LOCATION, false);
                    Location upd = gson.fromJson(intent.getExtras().getString(LocationActivity.LOCATION_OBJECT), Location.class);
                    if (created) {
                        placesAdapter.addLocation(upd);
                    } else {
                        placesAdapter.updateLocation(upd);
                    }
                    if (upd.isActive()) {
                        ((MainActivity) getActivity()).addGeofence(upd);
                    } else {
                        ((MainActivity) getActivity()).removeGeofence(upd.getId());
                    }
                    data.setPlaces(placesAdapter.getPlaces());
                    saveData();
                }
            } else {
                Toast.makeText(getContext(), "Error while editing location", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (croppedImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                data.setAvaPath(croppedImageUri.getPath());
                setAvatar(croppedImageUri.getPath());
                saveData();
            } else {
                Toast.makeText(getContext(), "Required permissions are not granted", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 1) {
            EditText phoneInput = mainView.findViewById(R.id.phone);
            TelephonyManager tMgr = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String phone = PhoneNumberUtils.formatNumber(tMgr.getLine1Number());
            if (!phone.matches(PHONE_REGEX)) phone = "";
            phoneInput.setText(phone);
            data.setPhone(phone);
            saveData();
        }
    }

    @Override
    public void showOnMap(Location location) {
        //TODO
        Toast.makeText(getContext(), "Not implemented yet", Toast.LENGTH_LONG).show();
    }

    @Override
    public void edit(Location location) {
        Intent i = new Intent(getActivity(), LocationActivity.class);
        i.putExtra(LocationActivity.NEW_LOCATION, false);
        i.putExtra(LocationActivity.LOCATION_OBJECT, gson.toJson(location));
        startActivityForResult(i, LocationActivity.REQUEST_CODE);
    }

    @Override
    public void remove(final Location location) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
        adb.setTitle("Confirm action");
        adb.setMessage("Are you sure you want to delete location '" + location.getName() + "'?");
        adb.setNegativeButton("Cancel", null);
        adb.setPositiveButton("Yes", new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                placesAdapter.removeLocation(location.getId());
                data.setPlaces(placesAdapter.getPlaces());
                saveData();
                ((MainActivity) getActivity()).removeGeofence(location.getId());
            }});
        adb.show();
    }

    public boolean isAppBarLocked() {
        return appBarLocked;
    }
}

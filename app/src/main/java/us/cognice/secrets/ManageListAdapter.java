package us.cognice.secrets;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import us.cognice.secrets.data.Location;
import us.cognice.secrets.fragments.ManageFragment;
import us.cognice.secrets.utils.Utils;

import java.util.List;

public class ManageListAdapter extends RecyclerView.Adapter<ManageListAdapter.ViewHolder> {

    private final List<Location> places;
    private final LocationListener locationListener;

    public ManageListAdapter(List<Location> locations, LocationListener locationListener) {
        this.places = locations;
        this.locationListener = locationListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.place = places.get(position);
        holder.nameView.setText(holder.place.getName());
        holder.detailsView.setText(Utils.formatCoordinate(holder.place.getLatitude()) + ", " +
                        Utils.formatCoordinate(holder.place.getLongitude()) +
                        "; radius: " + Utils.formatMeters(holder.place.getRadius()));
        holder.location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != locationListener) {
                    locationListener.showOnMap(holder.place);
                }
            }
        });
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != locationListener) {
                    locationListener.edit(holder.place);
                }
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != locationListener) {
                    locationListener.remove(holder.place);
                }
            }
        });
    }

    private void lockAppBar() {
        //waiting for add/remove animation to complete
        new Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        ((ManageFragment) locationListener).lockAppBar();
                    }
                }, 300);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public void removeLocation(String id) {
        for(int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(id)) {
                places.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, places.size());
                lockAppBar();
                break;
            }
        }
    }

    public void updateLocation(Location location) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(location.getId())) {
                places.set(i, location);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void addLocation(Location location) {
        places.add(location);
        notifyItemInserted(places.size() - 1);
        lockAppBar();
    }

    public List<Location> getPlaces() {
        return places;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final TextView detailsView;
        final ImageView location;
        final ImageView edit;
        final ImageView remove;
        Location place;
        public final View view;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            this.nameView = view.findViewById(R.id.locationName);
            this.detailsView = view.findViewById(R.id.locationDetails);
            this.location = view.findViewById(R.id.locationIcon);
            this.edit = view.findViewById(R.id.locationEdit);
            this.remove = view.findViewById(R.id.locationRemove);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + nameView.getText() + "'";
        }
    }

    public interface LocationListener {
        void showOnMap(Location location);
        void edit(Location location);
        void remove(Location location);
    }
}

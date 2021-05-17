package io.rong.imkit.plugin.location;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import io.rong.imkit.R;
import io.rong.imkit.RongBaseActivity;
import io.rong.message.LocationMessage;

public class AMapPreviewActivity2D extends RongBaseActivity {
    private MapView mAMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_location_preview_activity_2d);
        mAMapView = findViewById(R.id.rc_ext_amap);
        mAMapView.onCreate(savedInstanceState);
        TextView title = findViewById(R.id.rc_action_bar_title);
        title.setText(R.string.rc_plugin_location);
        initMap();
    }

    private void initMap() {
        AMap amap;

        amap = mAMapView.getMap();
        amap.setMyLocationEnabled(true);
        amap.getUiSettings().setZoomControlsEnabled(false);
        amap.getUiSettings().setMyLocationButtonEnabled(false);

        Intent intent = getIntent();
        LocationMessage locationMessage = intent.getParcelableExtra("location");
        double lat = locationMessage.getLat();
        double lng = locationMessage.getLng();
        String poi = locationMessage.getPoi();
        MarkerOptions markerOptions = new MarkerOptions().anchor(0.5f, 0.5f)
                .position(new LatLng(lat, lng)).title(poi)
                .snippet(lat + "," + lng).draggable(false);
        Marker marker = amap.addMarker(markerOptions);
        amap.setInfoWindowAdapter(new AmapInfoWindowAdapter2D(AMapPreviewActivity2D.this));
        marker.showInfoWindow();
        amap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(lat, lng)).zoom(16).bearing(0).tilt(30).build()));
    }

    @Override
    protected void onDestroy() {
        mAMapView.onDestroy();
        super.onDestroy();
    }
}
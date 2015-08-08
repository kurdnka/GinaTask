package com.tomi.ginatask;

import android.util.Log;

import com.nutiteq.core.MapPos;
import com.nutiteq.core.ScreenPos;
import com.nutiteq.datasources.LocalVectorDataSource;
import com.nutiteq.ui.MapClickInfo;
import com.nutiteq.ui.MapEventListener;
import com.nutiteq.ui.MapView;
import com.nutiteq.ui.VectorElementClickInfo;
import com.nutiteq.ui.VectorElementsClickInfo;
import com.nutiteq.vectorelements.VectorElement;

/**
 * Created by Tomi on 6.8.2015.
 */
public class BasicMapEventListener extends MapEventListener {

    private MapView mapView;
    private LocalVectorDataSource vectorDataSource;

    private final String LOG_TAG = "Basic map view listener";

    //private BalloonPopup oldClickLabel;

    public BasicMapEventListener(MapView mapView, LocalVectorDataSource vectorDataSource) {
        this.mapView = mapView;
        this.vectorDataSource = vectorDataSource;
    }

    @Override
    public void onMapMoved() {

        final MapPos topLeft = mapView.screenToMap(new ScreenPos(0, 0));
        final MapPos bottomRight = mapView.screenToMap(new ScreenPos(mapView.getWidth(), mapView.getHeight()));
        Log.d(LOG_TAG, mapView.getOptions().getBaseProjection().toWgs84(topLeft)
                + " " + mapView.getOptions().getBaseProjection().toWgs84(bottomRight));

    }

    @Override
    public void onMapClicked(MapClickInfo mapClickInfo) {

    }

    @Override
    public void onVectorElementClicked(VectorElementsClickInfo vectorElementsClickInfo) {
        VectorElementClickInfo clickInfo = vectorElementsClickInfo.getVectorElementClickInfos().get(0);
        VectorElement vectorElement = clickInfo.getVectorElement();

        if (vectorElement instanceof PhotoMarker) {

        }
    }
}

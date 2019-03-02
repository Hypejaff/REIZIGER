package com.example.jaff.reiziger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/***CONTEXT************************************************************************************************
 *   Window Info Custom Adapter -->
 *   class allowing the management of the display of the information window on the marker click.
 ***************************************************************************************************/

public class WindowInfoCustomAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;

    public WindowInfoCustomAdapter(Context context) {
        mWindow = LayoutInflater.from(context).inflate(R.layout.window_info_custom, null);
    }

    private void rendowWindowText(Marker marker, View view){

        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.title);
        if(!title.equals("")){
            tvTitle.setText(title);
        }
        String snippet = marker.getSnippet();
        TextView tvSnippet = view.findViewById(R.id.snippet);
        if(!snippet.equals("")){
            tvSnippet.setText(snippet);
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }
}

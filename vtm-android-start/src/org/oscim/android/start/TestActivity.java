package org.oscim.android.start;

import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.Display;
import android.widget.Button;
import android.util.Log;
import android.location.Location;

import org.oscim.android.MapActivity;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.start.geolocation.FusedLocationReceiver;
import org.oscim.android.start.geolocation.FusedLocationService;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.InanimateItem;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.marker.PlayerItem;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.LinkedList;

public class TestActivity extends MapActivity {
	public static final Logger log = LoggerFactory.getLogger(TestActivity.class);

	/*protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		Map map = this.map();

		VectorTileLayer baseLayer = map.setBaseMap(new OSciMap4TileSource());
		map.layers().add(new BuildingLayer(map, baseLayer));
		map.layers().add(new LabelLayer(map, baseLayer));
		map.setTheme(VtmThemes.DEFAULT);

		//mMap.setMapPosition(49.417, 8.673, 1 << 17);
		map.setMapPosition(53.5620092, 9.9866457, 1 << 16);

		//	mMap.layers().add(new TileGridLayer(mMap));
	}*/


    private static final String TAG = "MyActivity";
    private Button btnFusedLocation;
    private FusedLocationService fusedLocationService;
    private PlayerItem myself;
    private ItemizedLayer<PlayerItem> playerLayer;
    private ItemizedLayer<InanimateItem> inanimateLayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        //show error dialog if GooglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        fusedLocationService = new FusedLocationService(this, new FusedLocationReceiver(){

            @Override
            public void onLocationChanged() {
                Log.i(TAG, "I'm the receiver, let's do my job!");
                updatePosition();
            }
        });

        /*btnFusedLocation = (Button) findViewById(R.id.btnGPSShowLocation);
        btnFusedLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) { TODO substitute for this
                updatePosition();
            }
        });*/

        Map map = this.map();

        VectorTileLayer baseLayer = map.setBaseMap(new OSciMap4TileSource());
        map.layers().add(new BuildingLayer(map, baseLayer));
        map.layers().add(new LabelLayer(map, baseLayer));

        LinkedList<PlayerItem> playerLayerList = new LinkedList<PlayerItem>();
        LinkedList<InanimateItem> inanimateLayerList = new LinkedList<InanimateItem>();

        // Setup player marker
        int myID = 1;   // TODO request unique ID from server (saved to your profile)
        this.myself = new PlayerItem("You!", "The smell of your soiled trousers permeate the surrounding area.", null, myID, false);
        android.graphics.Bitmap myselfMarker = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.wreckoman);
        myselfMarker = getResizedBitmap(myselfMarker, 1d/8d);
        Bitmap myselfBitmap = new AndroidBitmap(myselfMarker);
        MarkerSymbol myselfSymbol = new MarkerSymbol(myselfBitmap, 0.5f, 0.5f);
        this.myself.setMarker(myselfSymbol);

        // Setup default markers and player layer
        android.graphics.Bitmap playerDefault = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.uglyman);
        playerDefault = getResizedBitmap(playerDefault, 1d/8d);
        Bitmap defaultPlayerBitmap = new AndroidBitmap(playerDefault);
        MarkerSymbol defaultPlayerSymbol = new MarkerSymbol(defaultPlayerBitmap, 0.5f, 0.5f);
        this.playerLayer = new ItemizedLayer<PlayerItem>(map, playerLayerList, defaultPlayerSymbol,
                PlayerItem.playerGestureListener, "playerLayer");
        map.layers().add(this.playerLayer);

        // Setup inanimate item layer
        android.graphics.Bitmap itemDefault = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.fire);
        itemDefault = getResizedBitmap(itemDefault, 1d/8d);
        Bitmap defaultItemBitmap = new AndroidBitmap(itemDefault);
        MarkerSymbol defaultItemSymbol = new MarkerSymbol(defaultItemBitmap, 0.5f, 0.5f);
        this.inanimateLayer = new ItemizedLayer<InanimateItem>(map, inanimateLayerList, defaultItemSymbol,
                InanimateItem.inanimateItemGestureListener, "inanimateLayer");
        map.layers().add(this.inanimateLayer);

        map.setTheme(VtmThemes.DEFAULT);
    }

    private void updatePosition() {
        Location location = fusedLocationService.getLocation();
        double latitude;
        double longitude;
        if (null != location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            this.myself.setIsVisible(true);
            GeoPoint currentLocation = new GeoPoint(latitude, longitude);
            this.myself.setPoint(currentLocation);
            if(!this.playerLayer.containsItem(this.myself)) {
                this.playerLayer.addItem(this.myself);
            }
            this.map().setMapPosition(latitude, longitude, 1 << 20);
//            this.map().updateMap(true);

        } else {
            log.info("Location not available.");
            this.myself.setPoint(null);
            this.myself.setIsVisible(false);
            this.playerLayer.removeItem(this.myself);
            // FIXME refresh the UI to not include this player's location after a certain amount of time
        }
    }

    // TODO move to utilities
    private android.graphics.Bitmap getResizedBitmap(android.graphics.Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        android.graphics.Bitmap resizedBitmap = android.graphics.Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    /**
     * Scale given bitmap to have its width be a certain fraction of the screen width (screen width being the
     * shorter side of screen). Aspect ratio is preserved.
     */
    private android.graphics.Bitmap getResizedBitmap(android.graphics.Bitmap bm, double screenFraction) {
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenShortSide = size.x > size.y ? size.y : size.x;

        double scaleRatio = ((double)screenShortSide) * screenFraction / (double)bm.getWidth();
        int newWidth = (int)(scaleRatio * bm.getWidth());
        int newHeight = (int)(scaleRatio * bm.getHeight());

        return getResizedBitmap(bm, newWidth, newHeight);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }
}

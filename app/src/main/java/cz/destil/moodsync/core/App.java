package cz.destil.moodsync.core;

import android.app.Application;

import com.squareup.otto.Bus;

import cz.destil.moodsync.light.LightsController;
import cz.destil.moodsync.light.MirroringHelper;

/**
 * Main application object.
 *
 * @author David Vávra (david@vavra.me)
 */
public class App extends Application {

    static App sInstance;
    static Bus sBus;
    private MirroringHelper mMirroring;
    private LightsController mLights;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        sBus = new Bus();
        mMirroring = MirroringHelper.get();
        mLights = LightsController.get();
        mMirroring.init();
        mLights.init();
    }

    public static App get() {
        return sInstance;
    }

    public static Bus bus() {
        return sBus;
    }
}

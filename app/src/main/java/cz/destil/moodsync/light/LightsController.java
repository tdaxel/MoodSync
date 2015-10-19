package cz.destil.moodsync.light;

import android.graphics.Color;

import java.util.List;

import cz.destil.moodsync.R;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.BaseAsyncTask;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.core.GroupConfiguration;
import cz.destil.moodsync.event.ErrorEvent;
import cz.destil.moodsync.event.SuccessEvent;
import cz.destil.moodsync.util.Toas;
import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXHSBKColor;
import lifx.java.android.entities.LFXLightTarget;
import lifx.java.android.entities.LFXTypes;
import lifx.java.android.light.LFXTaggedLightCollection;
import lifx.java.android.network_context.LFXNetworkContext;

/**
 * Controller which controls LIFX lights.
 *
 * @author David Vávra (david@vavra.me)
 */
public class LightsController {

    private static final int TIMEOUT = 5000;
    private static LightsController sInstance;
    private LFXNetworkContext mNetworkContext;
    private boolean mWorkingFine;
    private boolean mDisconnected;

    public static LightsController get() {
        if (sInstance == null) {
            sInstance = new LightsController();
        }
        return sInstance;
    }

    public void changeColor(int color, GroupConfiguration groupConfiguration) {
        if (mWorkingFine && color != groupConfiguration.getPreviousColor()) {
            List<String> lamps = groupConfiguration.getLamps();
            if(lamps == null || lamps == GroupConfiguration.USEALLLAMPS)
            {
                mNetworkContext.getAllLightsCollection().setColorOverDuration(convertColor(color, groupConfiguration), Config.DURATION_OF_COLOR_CHANGE);
                groupConfiguration.setPreviousColor(color);
            }
            else
            {
                //Set all lamps (this need testing.
                for (String lampLabel: lamps )
                {
                    mNetworkContext.getAllLightsCollection().getLightsForLabel(lampLabel).get(0).setColorOverDuration(convertColor(color,groupConfiguration), Config.DURATION_OF_COLOR_CHANGE);
                    groupConfiguration.setPreviousColor(color);
                }
            }


        }
    }

    public void init() {
        mWorkingFine = false;
        mDisconnected = false;
        mNetworkContext = LFXClient.getSharedInstance(App.get()).getLocalNetworkContext();
        mNetworkContext.addNetworkContextListener(new LFXNetworkContext.LFXNetworkContextListener() {
            @Override
            public void networkContextDidConnect(LFXNetworkContext networkContext) {
                mDisconnected = false;
            }

            @Override
            public void networkContextDidDisconnect(LFXNetworkContext networkContext) {
                if (!mDisconnected && mWorkingFine) {
                    mWorkingFine = false;
                    Toas.t(R.string.lifx_disconnected);
                    App.bus().post(new ErrorEvent(R.string.lifx_disconnected));
                }
            }

            @Override
            public void networkContextDidAddTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {
                startRocking();
            }

            @Override
            public void networkContextDidRemoveTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {
            }
        });
    }

    private void startRocking() {
        App.bus().post(new SuccessEvent());
        mWorkingFine = true;
        mNetworkContext.getAllLightsCollection().setPowerState(LFXTypes.LFXPowerState.ON);
    }

    public void start() {
        mNetworkContext.connect();
        if (!mWorkingFine) {
            new TimeoutTask().start();
        } else {
            startRocking();
        }
    }

    public void stop() {
        mDisconnected = true;
        if (mNetworkContext != null && mWorkingFine) {
            mNetworkContext.disconnect();
        }
    }

    private LFXHSBKColor convertColor(int color, GroupConfiguration groupConfiguration) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return LFXHSBKColor.getColor(hsv[0], hsv[1], groupConfiguration.getBrightness(), 3500);
    }
    private LFXHSBKColor convertColorOld(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return LFXHSBKColor.getColor(hsv[0], hsv[1], Config.LIFX_BRIGHTNESS_Default, 3500);
    }

    public void signalStop() {
        int color = App.get().getResources().getColor(android.R.color.white);
        mNetworkContext.getAllLightsCollection().setColorOverDuration(convertColorOld(color), 100);
    }

    class TimeoutTask extends BaseAsyncTask {

        @Override
        public void inBackground() {
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void postExecute() {
            int numLights = mNetworkContext.getAllLightsCollection().getLights().size();

            // TODO AXEL remember was == 0;
            if (numLights != 0 || mDisconnected) {
                App.bus().post(new ErrorEvent(R.string.no_lights_found));
            } else {
                startRocking();
            }
        }
    }
}

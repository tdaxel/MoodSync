package cz.destil.moodsync.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

import cz.destil.moodsync.R;
import cz.destil.moodsync.activity.MainActivity;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.core.GroupConfiguration;
import cz.destil.moodsync.core.ScreenCapturePlacements;
import cz.destil.moodsync.event.LocalColorEvent;
import cz.destil.moodsync.light.ColorExtractor;
import cz.destil.moodsync.light.LightsController;
import cz.destil.moodsync.light.LocalColorSwitcher;
import cz.destil.moodsync.light.MirroringHelper;
import cz.destil.moodsync.util.SleepTask;

/**
 * Service which does all the work.
 *
 * @author David Vávra (david@vavra.me)
 */
public class LightsService extends Service {
    private MirroringHelper mMirroring;
    private ColorExtractor mColorExtractor;
    private LightsController mLights;
    private WifiManager.MulticastLock mMulticastLock;
    private LocalColorSwitcher mLocalSwitcher;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMirroring = MirroringHelper.get();
        mColorExtractor = ColorExtractor.get();
        mLights = LightsController.get();
        mLocalSwitcher = LocalColorSwitcher.get();
        App.bus().register(this);
    }

    @Override
    public void onDestroy() {
        App.bus().unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals("START")) {
            start();
        } else if (intent.getAction().equals("STOP")) {
            stop();
        }
        return START_REDELIVER_INTENT;
    }

    private void start() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        Notification notification = new Notification.Builder(this).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string
                .mirroring)).setContentText(getString(R.string.tap_to_change))
                .setContentIntent(pi).build();
        startForeground(42, notification);

        WifiManager wifi;
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock("lifx");
        mMulticastLock.acquire();


        //TODO this should be read from a config file.
        ArrayList<GroupConfiguration> groupConfigurationArrayList = new ArrayList<GroupConfiguration>();
        groupConfigurationArrayList.add(new GroupConfiguration("TEST NAME",-1,ScreenCapturePlacements.LEFT,new ArrayList<String>(Arrays.asList("Stue"))));
        groupConfigurationArrayList.add(new GroupConfiguration("TEST NAME2",-1, ScreenCapturePlacements.RIGHT,GroupConfiguration.USEALLLAMPS));

        mLights.start();




        for (final GroupConfiguration gp:groupConfigurationArrayList)
        {
            mColorExtractor.start(mMirroring,gp, new ColorExtractor.Listener() {
                @Override
                public void onColorExtracted(int color) {
                    if (!mLocalSwitcher.isRunning()) {
                        mLights.changeColor(color,gp);
                    }
                }
            });
        }

    }

    private void stop() {
        mColorExtractor.stop();
        mMirroring.stop();
        mLights.signalStop();

        //TODO STOP ALL THE GROUPS INDIVIDUAL.

        new SleepTask(Config.FINAL_DELAY, new SleepTask.Listener() {
            @Override
            public void awoken() {
                mLights.stop();
                if (mMulticastLock != null) {
                    mMulticastLock.release();
                }
                stopForeground(true);
                stopSelf();
            }
        }).start();
    }

    @Subscribe
    public void onNewLocalColor(LocalColorEvent event) {

        System.out.println(event);
        System.out.println("never here?");
        //mLights.changeColor(event.newColor, gp);
    }
}

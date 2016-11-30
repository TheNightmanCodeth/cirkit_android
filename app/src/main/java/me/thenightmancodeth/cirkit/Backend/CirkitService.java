package me.thenightmancodeth.cirkit.Backend;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import me.thenightmancodeth.cirkit.MainActivity;
import me.thenightmancodeth.cirkit.R;

/***************************************
 * Created by TheNightman on 11/21/16. *
 *                                     *
 * Service that runs CirkitServer in   *
 * the background. Needs to be         *
 * launched every ~12 hours or else    *
 * android will kill it.               *
 * (Samsung phones seem to kill it     *
 *   no matter what. For now just      *
 *   start the app when it stops)      *
 ***************************************/

public class CirkitService extends Service {
    CirkitServer server;
    private final IBinder binder = new LocalBinder();
    private static NotificationManagerCompat nm;
    private final Context ctx = CirkitService.this;
    private static NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
    public static boolean running;
    public static int pendingPushes = 0;
    private final int NOTIFICATION = 4200;
    private static int NEW_PUSH_NOT = 6960;

    public class LocalBinder extends Binder {
        CirkitService getService() {
            return this.getService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.i("Cirkit_Service", "Received start id " +startId +": " +intent);
        nm = NotificationManagerCompat.from(ctx);
        //Start cirkit server
        try {
            server = new CirkitServer(new MainActivity.OnPushReceivedListener() {
                @Override
                public void onPushRec(String push) {
                    pendingPushes++;
                    //Intent to launch when notification is clicked
                    //TODO: add push value to intent flags
                    final PendingIntent onNotiClick = PendingIntent
                            .getActivity(ctx, 0,
                                    new Intent(ctx, MainActivity.class), 0);
                    //Get notification sound
                    Uri alarmSound = RingtoneManager
                            .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    style.setBigContentTitle(getString(R.string.app_name));
                    style.addLine(push);
                    style.setSummaryText(pendingPushes +" new pushes");
                    //Create notification

                    Notification noti = new NotificationCompat.Builder(ctx)
                            .setSmallIcon(R.drawable.ic_noti)
                            .setStyle(style)
                            .setGroupSummary(true)
                            .setNumber(pendingPushes)
                            .setSound(alarmSound)
                            .setLights(Color.CYAN, 3000, 3000)
                            .setVibrate(new long[] {1000,1000})
                            .setContentIntent(onNotiClick)
                            .build();
                    nm.notify(NEW_PUSH_NOT, noti);
                }
            });
            //Starts server
            server.start();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        //Become foreground service
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        //Set persistent notification so service doesn't close
        Notification cirkitNoti = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_noti)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.persist_noti))
                .setContentIntent(pi)
                .build();
        //Make notification persistent
        cirkitNoti.flags|=Notification.FLAG_NO_CLEAR;
        startForeground(NOTIFICATION, cirkitNoti);
        running = true;
        return START_STICKY;
    }

    public static void resetPendingPushes() {
        pendingPushes = 0;
        style = new NotificationCompat.InboxStyle();
        if (nm != null) {
            nm.cancel(NEW_PUSH_NOT);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        //Stop server
        server.stop();
        //Close pers. notification
        nm.cancel(NOTIFICATION);
        running = false;
        Toast.makeText(this, "Cirkit service stopped...", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

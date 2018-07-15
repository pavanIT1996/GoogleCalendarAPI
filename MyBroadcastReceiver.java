package com.example.quickstart;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import java.util.Random;

/**
 * Created by Pavan on 7/14/2018.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {

    public String[] facts = new String[20];

    String CHANNEL_ID="100";
    String textTitle="Water Drinking Time";
    String textContent="Drink More Water, To Be More Healthier";
    @Override
    public void onReceive(Context context, Intent intent) {

        facts[0] = "A person can live about a month without food, but only about a week without water.";
        facts[1] = "75% of the human brain is water and 75% of a living tree is water.";
        facts[2] = "The average human body is made of 50 to 65 percent water.";
        facts[3] = "Everyone needs to drink eight glasses of water a day.";
        facts[4] = "Somewhere between 70 and 75 percent of the earth’s surface is covered with water.";
        facts[5] = "Drinking water flushes toxins from your body.";
        facts[6] = "Drinking water can help keep your skin moist.";
        facts[7] = "Drinking water helps you lose weight.";
        facts[8] = "Less than 1% of the water supply on earth can be used as drinking water.";
        facts[9] = "Yellow urine is a sign of dehydration.";
        facts[10] = "If you’re thirsty, you are already dehydrated.";
        facts[11] = "You need sports drinks, not water, to function at a high level in athletics.";
        facts[12] = "Reduces risk of osteoporosis and hip fractures.";
        facts[13] = "Reduces risk of kidney stones and ulcers.";
        facts[14] = "Aids in regulating body temperature.";
        facts[15] = "Helps in breathing and metabolism.";
        facts[16] = "Prevents cardiovascular disorders.";
        facts[17] = "Prevents constipation, heartburn and migraine.";
        facts[18] = "Helps to maintain pH balance.";
        facts[19] = "Solution to treat headaches.";


        Random r = new Random();
        int i = r.nextInt(20);
        textContent=facts[i];

        NotificationManager notificationManager=(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent repeating_intent= new Intent(context,MainActivity.class);
        repeating_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent= PendingIntent.getActivity(context,100,repeating_intent,PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.drink)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(textContent))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)      // >=API level 21
                .setLights(Color.WHITE, 2000, 3000)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // >=API level 21
        notificationManager.notify(100,mBuilder.build());
    }
}

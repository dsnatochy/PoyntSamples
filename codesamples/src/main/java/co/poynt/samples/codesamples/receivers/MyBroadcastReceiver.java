package co.poynt.samples.codesamples.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import co.poynt.os.model.Intents;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = MyBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, action, Toast.LENGTH_SHORT).show();
            }
        });
        if (Intents.ACTION_TRANSACTION_COMPLETED.equals(action)){
            if (intent.getExtras()!=null) {
                Log.d(TAG, "Received TRANSACTION_COMPLETED broadcast. Transaction id: " +
                        intent.getExtras().get(Intents.INTENT_EXTRAS_TRANSACTION_ID));
            }
        } else if (Intents.ACTION_PAYMENT_CANCELED.equals(action)) {
            Log.d(TAG, "Received broadcast: PAYMENT_CANCELED");
        }
    }
}

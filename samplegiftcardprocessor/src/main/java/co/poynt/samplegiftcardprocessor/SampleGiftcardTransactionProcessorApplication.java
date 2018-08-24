package co.poynt.samplegiftcardprocessor;

import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import co.poynt.api.model.Business;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.samplegiftcardprocessor.core.TransactionManager;


/**
 * Created by palavilli on 1/25/16.
 */
public class SampleGiftcardTransactionProcessorApplication extends Application {
    public static SampleGiftcardTransactionProcessorApplication instance;

    public static SampleGiftcardTransactionProcessorApplication getInstance() {
        return instance;
    }

    private static final String TAG = SampleGiftcardTransactionProcessorApplication.class.getSimpleName();

    TransactionManager transactionManager;

    Business business;

    IPoyntBusinessService businessService;

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            businessService = IPoyntBusinessService.Stub.asInterface(service);
            try {
                businessService.getBusiness(new IPoyntBusinessReadListener.Stub() {
                    @Override
                    public void onResponse(Business biz, PoyntError poyntError) throws RemoteException {
                        business = biz;
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };

    public Business getBusiness(){
        return business;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        transactionManager = TransactionManager.getInstance(this);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE), connection, BIND_AUTO_CREATE);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unbindService(connection);
    }
}

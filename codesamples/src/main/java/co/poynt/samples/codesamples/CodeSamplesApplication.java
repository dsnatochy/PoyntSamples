package co.poynt.samples.codesamples;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Telephony;

import co.poynt.api.model.Business;
import co.poynt.api.model.Code;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;

/**
 * Created by dennis on 10/21/18.
 */

public class CodeSamplesApplication extends Application {
    private  static CodeSamplesApplication instance;

    private IPoyntBusinessService businessService;
    private Business business;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            businessService = IPoyntBusinessService.Stub.asInterface(service);
            try {
                businessService.getBusiness(new IPoyntBusinessReadListener.Stub() {
                    @Override
                    public void onResponse(Business business, PoyntError poyntError) throws RemoteException {
                        CodeSamplesApplication.this.business = business;
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            businessService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE), connection, BIND_AUTO_CREATE);
    }

    public static CodeSamplesApplication getInstance(){
        return instance;
    }

    public Intent getTransactionServiceIntent(){
        // not safe since there are no null checks
        String transactionServicePackage = business.getStores().get(0).getAttributes().get("paymentProcessor");

        if (Intents.POYNT_SERVICES_PKG.equalsIgnoreCase(transactionServicePackage)) {
            return Intents.getComponentIntent(Intents.COMPONENT_POYNT_TRANSACTION_SERVICE);
        } else if ("com.elavon.converge".equalsIgnoreCase(transactionServicePackage)){
            // is available as Intents.COMPONENT_CONVERGE_TRANSACTION_SERVICE in October 2018 PoyntOS release
            ComponentName convergeComponentName = new ComponentName("com.elavon.converge", "com.elavon.converge.TransactionService");
            return Intents.getComponentIntent(convergeComponentName);
        }else{
            throw new IllegalStateException("Unknown transaction service: " + transactionServicePackage);
        }
    }

    public Business getBusiness(){
        return business;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unbindService(connection);
    }
}

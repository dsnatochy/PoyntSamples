package co.poynt.samples.codesamples;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.poynt.api.model.Business;
import co.poynt.os.contentproviders.orders.clientcontexts.ClientcontextsColumns;
import co.poynt.os.contentproviders.orders.clientcontexts.ClientcontextsCursor;
import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;
import co.poynt.os.contentproviders.orders.transactions.TransactionsCursor;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;

public class ContentProviderActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    TextView console;
    Button txnFromTerminalBtn;

    IPoyntBusinessService businessService;

    Business business;
    // deviceId of this terminal
    String terminalId;
    List<String> transactionIds;

    private static final int URL_LOADER = 0;
    private final String sortOrder = ClientcontextsColumns.DEFAULT_ORDER + " DESC LIMIT 200";

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            businessService = IPoyntBusinessService.Stub.asInterface(service);
            try {
                businessService.getBusiness(new IPoyntBusinessReadListener.Stub() {
                    @Override
                    public void onResponse(Business biz, PoyntError poyntError) throws RemoteException {
                        business = biz;
                        try {
                            terminalId = business.getStores().get(0).getStoreDevices().get(0).getDeviceId();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getLoaderManager().initLoader(URL_LOADER, null, ContentProviderActivity.this);
                                }
                            });
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_provider);
        console = (TextView) findViewById(R.id.consoleText);
        txnFromTerminalBtn = (Button) findViewById(R.id.txnFromTerminalBtn);
        txnFromTerminalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transactionsFromCurrentTerminal();
            }
        });
        transactionIds = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    public void transactionsFromCurrentTerminal(){
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                int argCount = transactionIds.size();
                StringBuilder inList = new StringBuilder();
                for (int i = 0; i < argCount; i++){
                    inList.append("?");
                    if (i < argCount - 1){
                        inList.append(",");
                    }
                }
                Cursor c = getContentResolver().query(
                        TransactionsColumns.CONTENT_URI,
                        null,
                        TransactionsColumns.TRANSACTIONID + " IN (" + inList.toString() + ")",
                        transactionIds.toArray(new String[0]),
                        null
                );
                TransactionsCursor tc = new TransactionsCursor(c);
                List<String> result = new ArrayList<>();
                while(tc.moveToNext()){
                    result.add(tc.getTransactionid() + " | " + tc.getStatus() + " | " + tc.getCreatedat());
                }
                return result;
            }

            @Override
            protected void onPostExecute(List<String> transactions) {
                super.onPostExecute(transactions);
                for (String transaction : transactions){
                    console.append(transaction + '\n');
                }
            }
        }.execute();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case URL_LOADER:
                return new CursorLoader(
                        this,
                        ClientcontextsColumns.CONTENT_URI,
                        null,
                        ClientcontextsColumns.STOREDEVICEID + "= ?",
                        new String[]{terminalId},
                        sortOrder
                );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ClientcontextsCursor cc = new ClientcontextsCursor(data);
        while (cc.moveToNext()){
            String transactionId = cc.getLinkedid();
            transactionIds.add(transactionId);
        }
        if (transactionIds.size() > 0){
            txnFromTerminalBtn.setEnabled(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

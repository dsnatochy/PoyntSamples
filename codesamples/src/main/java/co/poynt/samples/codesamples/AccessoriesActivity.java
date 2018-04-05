package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrintedReceiptLineFont;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.model.PrintedReceiptV2;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntPrinterService;
import co.poynt.os.services.v1.IPoyntPrinterServiceListener;
import co.poynt.os.util.AccessoryProviderServiceHelper;

public class AccessoriesActivity extends Activity {

    private static final String TAG = AccessoriesActivity.class.getSimpleName();

    private TextView mDumpTextView;
    private ScrollView mScrollView;

    private AccessoryProviderServiceHelper accessoryProviderServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();
    private List<AccessoryProvider> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessories);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        Button printReceiptBtn = (Button) findViewById(R.id.printReceiptBtn);
        printReceiptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printReceipt();
            }
        });

        try {
            // initialize capabilityProviderServiceHelper
            accessoryProviderServiceHelper = new AccessoryProviderServiceHelper(this);
            // connect to accessory manager service
            accessoryProviderServiceHelper.bindAccessoryManager(
                    new AccessoryProviderServiceHelper.AccessoryManagerConnectionCallback() {
                        @Override
                        public void onConnected(AccessoryProviderServiceHelper accessoryProviderServiceHelper) {
                            // when connected check if we have any printers registered
                            if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                                AccessoryProviderFilter filter = new AccessoryProviderFilter(AccessoryType.PRINTER);
                                Log.d(TAG, "trying to get PRINTER accessory...");
                                try {
                                    // look up the printers using the filter
                                    accessoryProviderServiceHelper.getAccessoryServiceManager().getAccessoryProviders(
                                            filter, poyntAccessoryManagerListener);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Unable to connect to Accessory Service", e);
                                    logReceivedMessage("Unable to connect to Accessory Service");
                                }
                            } else {
                                logReceivedMessage("Not connected with accessory service manager");
                            }
                        }

                        @Override
                        public void onDisconnected(AccessoryProviderServiceHelper accessoryProviderServiceHelper) {
                            logReceivedMessage("Disconnected with accessory service manager");
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to connect to capability or accessory manager", e);
            logReceivedMessage("Failed to connect to capability or accessory manager");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    // this is the accessory manager listener which gets invoked when accessory manager completes
    // scanning for the requested accessories
    private IPoyntAccessoryManagerListener poyntAccessoryManagerListener
            = new IPoyntAccessoryManagerListener.Stub() {

        @Override
        public void onError(PoyntError poyntError) throws RemoteException {
            Log.e(TAG, "Failed to connect to accessory manager: " + poyntError);
        }

        @Override
        public void onSuccess(final List<AccessoryProvider> printers) throws RemoteException {
            // now that we are connected - request service connections to each accessory provider
            if (printers != null && printers.size() > 0) {
                // save it for future reference
                providers = printers;
                if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    // for each printer accessory - request "service" connections if it's still connected
                    for (AccessoryProvider printer : printers) {
                        Log.d(TAG, "Printer: " + printer.toString());
                        if (printer.isConnected()) {
                            // request service connection binder
                            // IMP: note that this method returns service connection if it already exists
                            // hence the need for both connection callback and the returned value
                            IBinder binder = accessoryProviderServiceHelper.getAccessoryService(
                                    printer, AccessoryType.PRINTER,
                                    providerConnectionCallback);
                            //already cached connection.
                            if (binder != null) {
                                mPrinterServices.put(printer, binder);
                            }
                        }
                    }
                }
            } else {
                logReceivedMessage("No Printers found");
            }
        }
    };

    // this is the callback for the service connection to each accessory provider service in this case
    // the android service supporting  printer accessory
    private AccessoryProviderServiceHelper.ProviderConnectionCallback providerConnectionCallback
            = new AccessoryProviderServiceHelper.ProviderConnectionCallback() {

        @Override
        public void onConnected(AccessoryProvider provider, IBinder binder) {
            // in some cases multiple accessories of the same type (eg. two printers of same
            // make/model or two star printers) might be supported by the same android service
            // so here we check if we need to share the same service connection for more than
            // one accessory provider
            List<AccessoryProvider> otherProviders = findMatchingProviders(provider);
            // all of them share the same service binder
            for (AccessoryProvider matchedProvider : otherProviders) {
                mPrinterServices.put(matchedProvider, binder);
            }
        }

        @Override
        public void onDisconnected(AccessoryProvider provider, IBinder binder) {
            // set the lookup done to false so we can try looking up again if needed
            if (mPrinterServices != null && mPrinterServices.size() > 0) {
                mPrinterServices.remove(binder);
                // try to renew the connection.
                if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    IBinder binder2 = accessoryProviderServiceHelper.getAccessoryService(
                            provider, AccessoryType.PRINTER,
                            providerConnectionCallback);
                    if (binder2 != null) {//already cached connection.
                        mPrinterServices.put(provider, binder2);
                    }
                }
            }
        }
    };

    // we do this if there are multiple accessories connected of the same type/provider
    private List<AccessoryProvider> findMatchingProviders(AccessoryProvider provider) {
        ArrayList<AccessoryProvider> matchedProviders = new ArrayList<>();
        if (providers != null) {
            for (AccessoryProvider printer : providers) {
                if (provider.getAccessoryType() == printer.getAccessoryType()
                        && provider.getPackageName().equals(printer.getPackageName())
                        && provider.getClassName().equals(printer.getClassName())) {
                    matchedProviders.add(printer);
                }
            }
        }
        return matchedProviders;
    }

    public void logReceivedMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append("<< " + message + "\n\n");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    private void clearLog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.setText("");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    private void printReceipt() {
        // IMP: here we are printing using all connected printers - but typically there should be
        // an option offered to the merchant to select which printer to use. For now that's the
        // app's responsibility
        if (providers != null && providers.size() > 0) {
            logReceivedMessage("Printing...");
            for (AccessoryProvider provider : providers) {
                try {
                    IBinder binder = mPrinterServices.get(provider);
                    if (binder != null) {
                        final IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(binder);
                        if (printerService != null) {
                            Log.d(TAG, "Printing");
                            printerService.printReceipt(UUID.randomUUID().toString(),
                                    getPrintedReceipt(), new IPoyntPrinterServiceListener.Stub() {
                                        @Override
                                        public void onPrintResponse(PrinterStatus printerStatus, String s) throws RemoteException {
                                            // check printerStatus.getCode() and handle based on values below
                                            /*
                                                PRINTER_CONNECTED,
                                                PRINTER_DISCONNECTED,
                                                PRINTER_UNAVAILABLE,
                                                PRINTER_JOB_PRINTED,
                                                PRINTER_JOB_FAILED,
                                                PRINTER_JOB_QUEUED,
                                                PRINTER_ERROR_OUT_OF_PAPER,
                                                PRINTER_ERROR_OTHER,
                                                PRINTER_ERROR_IMAGE_OFFDOCK
                                             */
                                            Log.d(TAG, "onPrintResponse code: " + printerStatus.getCode());
                                        }
                                    }, null);
                        }
                    } else {
                        logReceivedMessage("No service connection found");
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to communicate with printer", e);
                    logReceivedMessage("Failed to communicate with printer");
                }
            }
        } else {
            Log.e(TAG, "printer not connected");
            logReceivedMessage("No connected printers found");
        }
    }

    private PrintedReceiptV2 getPrintedReceipt() {
        PrintedReceiptV2 receipt = new PrintedReceiptV2();
        List<PrintedReceiptLine> lines = new ArrayList<>();
        lines.add(new PrintedReceiptLine("some random text"));
        lines.add(new PrintedReceiptLine("more random text"));
        lines.add(new PrintedReceiptLine("\n"));
        lines.add(new PrintedReceiptLine("\n"));
        lines.add(new PrintedReceiptLine("\n"));
        lines.add(new PrintedReceiptLine("\n"));
        lines.add(new PrintedReceiptLine("\n"));

        PrintedReceiptLineFont font = new PrintedReceiptLineFont(PrintedReceiptLineFont.FONT_SIZE.FONT24, 26 /* line spacing */);
        PrintedReceiptSection body1 = new PrintedReceiptSection(lines, font);
        receipt.setBody1(body1);
        return receipt;
    }
}

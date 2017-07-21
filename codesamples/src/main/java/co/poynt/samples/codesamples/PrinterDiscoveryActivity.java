package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.poynt.os.Constants;
import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.printing.ReceiptPrintingPref;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntPrinterService;
import co.poynt.os.services.v1.IPoyntPrinterServiceListener;
import co.poynt.os.util.AccessoryProviderServiceHelper;

public class PrinterDiscoveryActivity extends Activity {
    private static final String TAG = PrinterDiscoveryActivity.class.getSimpleName();

    private AccessoryProviderServiceHelper accessoryProviderServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();
    private List<AccessoryProvider> providers;

    private Set<String> mPrinterNames;

    Button printBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_discovery);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        Button button = (Button) findViewById(R.id.findReceiptPrinterBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrinterNames = findReceiptPrinters();
            }
        });

        printBtn = (Button) findViewById(R.id.printBtn);
        printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printItemReceipt();
            }
        });
        printBtn.setEnabled(false);


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
    // the android service supporting printer accessory
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

    private Set<String> findReceiptPrinters() {
        Set<String> set = ReceiptPrintingPref.readReceiptPrefsFromDb(this, Constants.ReceiptPreference.PREF_ITEM_RECEIPT);
        if (set!=null && set.size() > 0) {
            for (String s : set) {
                Log.d(TAG, "findReceiptPrinter: " + s);
            }
            printBtn.setEnabled(true);
        }else{
            logReceivedMessage("No item receipt printers found");
        }

        return set;
    }

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

    private void printItemReceipt() {
        final PrintedReceipt printedReceipt = generateReceipt();

        // IMP: here we are printing on all connected item receipt printers - but typically there should be
        // an option offered to the merchant to select which printer to use. For now that's the
        // app's responsibility
        if (providers != null && providers.size() > 0) {
            logReceivedMessage("printing...");
            for (AccessoryProvider provider : providers) {
                try {
                    IBinder binder = mPrinterServices.get(provider);
                    if (mPrinterNames != null) {
                        Log.d(TAG, "printItemReceipt: provider name: " + provider.getProviderName());
                        if (mPrinterNames.contains(provider.getProviderName())) {
                            if (binder != null) {
                                IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(binder);
                                if (printerService != null) {
                                    Log.d(TAG, "printing");

                                    printerService.printReceiptJob(UUID.randomUUID().toString(), printedReceipt,
                                            new IPoyntPrinterServiceListener.Stub() {
                                                @Override
                                                public void onPrintResponse(PrinterStatus printerStatus, String s) throws RemoteException {
                                                    Log.d(TAG, "onPrintResponse: " + printerStatus.getMessage());
                                                }
                                            });
                                }
                            } else {
                                logReceivedMessage("No service connection found");
                            }
                        }
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

    public void logReceivedMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PrinterDiscoveryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * just a dummy receipt
     * @return
     */
    private PrintedReceipt generateReceipt(){
        PrintedReceipt printedReceipt = new PrintedReceipt();

        // BODY
        List<PrintedReceiptLine> body = new ArrayList<PrintedReceiptLine>();

        body.add(newLine(" Check-in REWARD  "));
        body.add(newLine(""));
        body.add(newLine("FREE Reg. 1/2 Order"));
        body.add(newLine("Nachos or CHEESE"));
        body.add(newLine("Quesadilla with min."));
        body.add(newLine("$ 15 bill."));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("Coupon#: 1234-5678"));
        body.add(newLine(" Check-in REWARD  "));
        body.add(newLine(""));
        body.add(newLine("FREE Reg. 1/2 Order"));
        body.add(newLine("Nachos or CHEESE"));
        body.add(newLine("Quesadilla with min."));
        body.add(newLine("$ 15 bill."));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("Coupon#: 1234-5678"));
        body.add(newLine("  Powered by Poynt"));
        printedReceipt.setBody(body);

        return printedReceipt;
    }
    private PrintedReceiptLine newLine(String s){
        PrintedReceiptLine line = new PrintedReceiptLine();
        line.setText(s);
        return line;
    }
}

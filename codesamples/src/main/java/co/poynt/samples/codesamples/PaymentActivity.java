package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.CaptureAllResponse;
import co.poynt.api.model.Card;
import co.poynt.api.model.CardType;
import co.poynt.api.model.CatalogDisplayMetadata;
import co.poynt.api.model.ClientContext;
import co.poynt.api.model.CustomFundingSource;
import co.poynt.api.model.CustomFundingSourceType;
import co.poynt.api.model.FundingSource;
import co.poynt.api.model.FundingSourceAccountType;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Order;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProcessorStatus;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.contentproviders.orders.transactionreferences.TransactionreferencesColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;
import co.poynt.os.services.v1.IPoyntTransactionService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;
import co.poynt.samples.codesamples.utils.Util;

public class PaymentActivity extends Activity {

    // request code for payment service activity
    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private static final int ZERO_DOLLAR_AUTH_REQUEST = 13133;
    private static final String TAG = PaymentActivity.class.getSimpleName();

    private IPoyntTransactionService mTransactionService;
    private IPoyntOrderService mOrderService;
    Type transactionType = new TypeToken<Transaction>() {
    }.getType();

    Button chargeBtn;
    Button payOrderBtn;
    Button launchRegisterBtn;
    Button zeroDollarAuthBtn;
    Button createCashTxnBtn;
    Button createCheckTxnBtn;
    Button captureAllBtn;
    TextView orderSavedStatus;

    private Gson gson;

    String lastReferenceId;

    BroadcastReceiver settlementReceiver;

    /*
     * Class for interacting with the OrderService
     */
    private ServiceConnection mOrderServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntOrderService is now connected");
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mOrderService = IPoyntOrderService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntOrderService has unexpectedly disconnected");
            mOrderService = null;
        }
    };
    private IPoyntOrderServiceListener saveOrderCallback = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            if (order == null) {
                Log.d("orderListener", "poyntError: " + (poyntError == null ? "" : poyntError.toString()));
            }else{
                Log.d(TAG, "orderResponse: " + order);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        orderSavedStatus.setText("ORDER SAVED");
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        android.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

//        gson = new GsonBuilder().setPrettyPrinting().create();
        gson = new Gson();
        chargeBtn = (Button) findViewById(R.id.chargeBtn);
        chargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPoyntPayment(100l, null);
            }
        });

        chargeBtn.setEnabled(true);

        orderSavedStatus = (TextView) findViewById(R.id.orderSavedStatus);


        payOrderBtn = (Button) findViewById(R.id.payOrderBtn);
        payOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Order order = Util.generateOrder();
                launchPoyntPayment(order.getAmounts().getNetTotal(), order);
            }
        });

        Button launchTxnList = (Button) findViewById(R.id.launchTxnList);
        launchTxnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("poynt.intent.action.VIEW_TRANSACTIONS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        zeroDollarAuthBtn = (Button) findViewById(R.id.zeroDollarAuthBtn);
        zeroDollarAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doZeroDollarAuth();
            }
        });

        createCashTxnBtn = (Button) findViewById(R.id.createCashTxnBtn);
        createCashTxnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transaction t = new Transaction();
                t.setAction(TransactionAction.SALE);

                TransactionAmounts amounts = new TransactionAmounts();
                String currency = Currency.getInstance(getResources().getConfiguration().locale).getCurrencyCode();
                amounts.setCurrency(currency);
                long amount = 1000L;
                amounts.setOrderAmount(amount);
                amounts.setTransactionAmount(amount);
                t.setAmounts(amounts);

                FundingSource fs = new FundingSource();
                fs.setType(FundingSourceType.CASH);
                t.setFundingSource(fs);

                t.setStatus(TransactionStatus.CAPTURED);

                ClientContext context = new ClientContext();
                context.setBusinessId(UUID.fromString("803833ba-cb97-434d-b158-30db6973173b"));
                t.setContext(context);

                try {
                    mTransactionService.processTransaction(t, UUID.randomUUID().toString(), new IPoyntTransactionServiceListener.Stub() {
                        @Override
                        public void onResponse(Transaction transaction, String s, PoyntError poyntError) throws RemoteException {
                            if (transaction !=null) {
                                logData(gson.toJson(transaction, transactionType));
                            }else {
                                Log.d(TAG, "onResponse: error: " + poyntError);
                            }
                        }

                        @Override
                        public void onLoginRequired() throws RemoteException {

                        }

                        @Override
                        public void onLaunchActivity(Intent intent, String s) throws RemoteException {

                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                /*
{"action":"SALE","amounts":{"cashbackAmount":0,"currency":"USD","orderAmount":1200,"tipAmount":0,"transactionAmount":1200},
"context":{"businessId":"803833ba-cb97-434d-b158-30db6973173b","businessType":"TEST_MERCHANT","employeeUserId":26835234,"mcc":"5812","mid":"34kdx7s8gs","source":"INSTORE","sourceApp":"co.poynt.sample","storeAddressCity":"Palo Alto","storeAddressTerritory":"California","storeId":"b4c4d6e6-6a5e-4ca9-86ee-a086ff2fc9c9","storeTimezone":"America/Los_Angeles","tid":"o5lp"},
"fundingSource":{"type":"CASH"},"id":"6495aa67-9c79-4686-add0-294d079f6e24","processorOptions":{"type":"emi","originalAmount":"2400","installments":"2"},"references":[{"customType":"referenceId","id":"0efa7271-0d05-4134-a1bd-5cc68ebf91e5","type":"CUSTOM"}],"status":"CAPTURED"}
                 */

            }
        });

        createCheckTxnBtn = (Button) findViewById(R.id.createCheckTxnBtn);
        createCheckTxnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Transaction t = new Transaction();
                t.setAction(TransactionAction.SALE);

                TransactionAmounts amounts = new TransactionAmounts();
                String currency = Currency.getInstance(getResources().getConfiguration().locale).getCurrencyCode();
                amounts.setCurrency(currency);
                long amount = 1000L;
                amounts.setOrderAmount(amount);
                amounts.setTransactionAmount(amount);
                t.setAmounts(amounts);

                FundingSource fs = new FundingSource();
                fs.setType(FundingSourceType.CUSTOM_FUNDING_SOURCE);
                CustomFundingSource cfs = new CustomFundingSource();
                cfs.setAccountId(UUID.randomUUID().toString());
                cfs.setProvider("Talech");
                cfs.setType(CustomFundingSourceType.CHEQUE);
                fs.setCustomFundingSource(cfs);
                t.setFundingSource(fs);

                ProcessorResponse pr = new ProcessorResponse();
                pr.setStatus(ProcessorStatus.Successful);
                pr.setStatusCode("1");
                pr.setTransactionId(UUID.randomUUID().toString());
                t.setProcessorResponse(pr);
                t.setStatus(TransactionStatus.CAPTURED);

                ClientContext context = new ClientContext();
                context.setBusinessId(UUID.fromString("803833ba-cb97-434d-b158-30db6973173b"));
                t.setContext(context);

                try {
                    mTransactionService.processTransaction(t, UUID.randomUUID().toString(), new IPoyntTransactionServiceListener.Stub() {
                        @Override
                        public void onResponse(Transaction transaction, String s, PoyntError poyntError) throws RemoteException {
                            if (transaction !=null) {
                                logData(gson.toJson(transaction, transactionType));
                            }else {
                                Log.d(TAG, "onResponse: error: " + poyntError);
                            }
                        }

                        @Override
                        public void onLoginRequired() throws RemoteException {

                        }

                        @Override
                        public void onLaunchActivity(Intent intent, String s) throws RemoteException {

                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        captureAllBtn = (Button) findViewById(R.id.captureAllBtn);
        captureAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mTransactionService.captureAllTransactions(UUID.randomUUID().toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        settlementReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                CaptureAllResponse response = intent.getParcelableExtra(Intents.INTENT_EXTRA_SETTLEMENT_ACCEPTED_RESPONSE);
                PoyntError error = intent.getParcelableExtra(Intents.INTENT_EXTRA_SETTLEMENT_ACCEPTANCE_ERROR);
                String requestId = intent.getStringExtra(Intents.INTENT_EXTRA_SETTLEMENT_REQUEST_ID);
                Log.d(TAG, "settlement response: " + response);
                Log.d(TAG, "settlement error: " + error);
            }
        };

/*

        launchRegisterBtn = (Button) findViewById(R.id.launchRegisterBtn);
        // Only works if Poynt Register does not have an active order in progress
        launchRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Product product = Util.createProduct();
                Intent intent = new Intent();
                intent.setAction(Intents.ACTION_ADD_PRODUCT_TO_CART);
                intent.putExtra(Intents.INTENT_EXTRA_PRODUCT, product);
                intent.putExtra(Intents.INTENT_EXTRA_QUANTITY, 2.0f);
                startActivity(intent);
            }
        });
*/
    }

    private void doZeroDollarAuth() {
        Payment p = new Payment();
        p.setAction(TransactionAction.VERIFY);
        p.setCurrency("USD");
        p.setAuthzOnly(true);
//        p.setVerifyOnly(true);
        //p.setManualEntry(true);

//        List<Transaction> transactions = new ArrayList<>();
//        Transaction transaction = TransactionUtil.newInstance();
//        transaction.setAction(TransactionAction.VERIFY);
//        transactions.add(transaction);
//        payment.setTransactions(transactions);

        Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
        collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, p);
        startActivityForResult(collectPaymentIntent, ZERO_DOLLAR_AUTH_REQUEST);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindServices();
    }

    private void bindServices() {
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ORDER_SERVICE),
                mOrderServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_TRANSACTION_SERVICE),
                mTransactionServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindServices();
    }

    private void unbindServices() {
        unbindService(mOrderServiceConnection);
        unbindService(mTransactionServiceConnection);
    }

    private class SaveOrderTask extends AsyncTask<Order, Void, Void> {
        protected Void doInBackground(Order... params) {
            Order order = params[0];
            String requestId = UUID.randomUUID().toString();
            if (mOrderService != null) {
                try {
                    mOrderService.createOrder(order, requestId, saveOrderCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_payment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private IPoyntTransactionServiceListener mTransactionServiceListener = new IPoyntTransactionServiceListener.Stub() {
        public void onResponse(Transaction _transaction, String s, PoyntError poyntError) throws RemoteException {
            Gson gson = new Gson();
            Type transactionType = new TypeToken<Transaction>() {
            }.getType();
            String transactionJson = gson.toJson(_transaction, transactionType);
            Log.d(TAG, "onResponse: " + transactionJson);
            Log.d(TAG, "onResponse: " + _transaction);

        }

        //@Override
        public void onLaunchActivity(Intent intent, String s) throws RemoteException {
            //do nothing
        }

        public void onLoginRequired() throws RemoteException {
            Log.d(TAG, "onLoginRequired called");
        }

    };

    public void getTransaction(String txnId) {
        try {

            mTransactionService.getTransaction(txnId, UUID.randomUUID().toString(),
                    mTransactionServiceListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mTransactionServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTransactionService = IPoyntTransactionService.Stub.asInterface(iBinder);


            try {
                mTransactionService.getTransaction("fcf98959-c188-42d1-b085-786d21e552ac", UUID.randomUUID().toString(), mTransactionServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mTransactionService = null;
        }
    };

    private void launchPoyntPayment(long amount, Order order) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        lastReferenceId = UUID.randomUUID().toString();
        payment.setReferenceId(lastReferenceId);

        payment.setCurrency(currencyCode);
        // enable multi-tender in payment options
        payment.setMultiTender(true);

        if (order != null) {
            payment.setOrder(order);
            payment.setOrderId(order.getId().toString());

            // tip can be preset
            //payment.setTipAmount(500l);
            payment.setAmount(order.getAmounts().getNetTotal());
        } else {
            // some random amount
            payment.setAmount(1200l);

            // here's how tip can be disabled for tip enabled merchants
            // payment.setDisableTip(true);
        }

        payment.setSkipSignatureScreen(true);
        payment.setSkipReceiptScreen(true);
        payment.setSkipPaymentConfirmationScreen(true);

        payment.setCallerPackageName("co.poynt.sample");
        Map<String, String> processorOptions = new HashMap<>();
        processorOptions.put("installments", "2");
        processorOptions.put("type", "emi");
        processorOptions.put("originalAmount", "2400");
        payment.setProcessorOptions(processorOptions);

        // start Payment activity for result
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Received onActivityResult (" + requestCode + ")");
        // Check which request we're responding to
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            logData("Received onActivityResult from Payment Action");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);

                    if (payment != null) {
                        //save order
                        if (payment.getOrder() != null) {
                            new SaveOrderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, payment.getOrder());
                        }

//                      Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        Gson gson = new Gson();
//                        Type paymentType = new TypeToken<Payment>() {
//                        }.getType();
//                        Log.d(TAG, gson.toJson(payment, paymentType));
                        Log.d(TAG, "onActivityResult: " + payment.getTransactions().get(0));
                        for (Transaction t : payment.getTransactions()) {
                            Type txnType = new TypeToken<Transaction>() {
                            }.getType();
                            Log.d(TAG, "onActivityResult: transaction: " + gson.toJson(t, txnType));

                            getTransaction(t.getId().toString());
                            //Log.d(TAG, "Card token: " + t.getProcessorResponse().getCardToken());
                            FundingSourceAccountType fsAccountType = t.getFundingSource().getAccountType();
                            if (t.getFundingSource().getCard() != null) {
                                Card c = t.getFundingSource().getCard();
                                String numberMasked = c.getNumberMasked();
                                String approvalCode = t.getApprovalCode();
                                CardType cardType = c.getType();
                                switch (cardType) {
                                    case AMERICAN_EXPRESS:
                                        // amex
                                        break;
                                    case VISA:
                                        // visa
                                        break;
                                    case MASTERCARD:
                                        // MC
                                        break;
                                    case DISCOVER:
                                        // discover
                                        break;
                                }
                            }

                        }

                        Log.d(TAG, "Received onPaymentAction from PaymentFragment w/ Status("
                                + payment.getStatus() + ")");
                        if (payment.getStatus().equals(PaymentStatus.COMPLETED)) {
                            logData("Payment Completed");
                        } else if (payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
                            logData("Payment Authorized");
                        } else if (payment.getStatus().equals(PaymentStatus.CANCELED)) {
                            logData("Payment Canceled");
                        } else if (payment.getStatus().equals(PaymentStatus.FAILED)) {
                            logData("Payment Failed");
                        } else if (payment.getStatus().equals(PaymentStatus.REFUNDED)) {
                            logData("Payment Refunded");
                        } else if (payment.getStatus().equals(PaymentStatus.VOIDED)) {
                            logData("Payment Voided");
                        } else {
                            logData("Payment Completed");
                        }
                    } else {
                        // This should not happen, but in case it does, handle it using Content Provider
                        getTransactionFromContentProvider();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                logData("Payment Canceled");
            }
        } else if (requestCode == ZERO_DOLLAR_AUTH_REQUEST) {
            Log.d(TAG, "onActivityResult: $0 auth request");
            if (resultCode == Activity.RESULT_OK) {
                Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                Gson gson = new Gson();
                Type paymentType = new TypeToken<Payment>() {
                }.getType();
                Log.d(TAG, gson.toJson(payment, paymentType));
            }
        }
    }

    /**
     * pulls transaction Ids by referenceId from the content provider
     */
    private void getTransactionFromContentProvider() {
        ContentResolver resolver = getContentResolver();
        String[] projection = new String[]{TransactionreferencesColumns.TRANSACTIONID};
        Cursor cursor = resolver.query(TransactionreferencesColumns.CONTENT_URI,
                projection,
                TransactionreferencesColumns.REFERENCEID + " = ?",
                new String[]{lastReferenceId},
                null);
        List<String> transactions = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                transactions.add(cursor.getString(0));
            }
        }

        cursor.close();

        // handle transactions
        // full transaction can get retrieved using IPoyntTransactionService.getTransaction
        if (!transactions.isEmpty()) {
            logData("Found the following transactions for referenceId " + lastReferenceId + ": ");
            for (String txnId : transactions) {
                logData(txnId);
            }
        } else {
            logData("No Transactions found");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_SETTLEMENT_REQUEST_ACCEPTED);
        filter.addAction(Intents.ACTION_SETTLEMENT_REQUEST_FAILED);
        registerReceiver(settlementReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(settlementReceiver);
    }

    public void logData(final String data) {
        Log.d(TAG, data);
    }

}

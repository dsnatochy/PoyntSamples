package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

import static android.view.View.GONE;

public class MainActivity extends Activity {
    private Button transactionListBtn;
    private Button terminalUserLoginBtn;
    private Button orderBtn;
    private Button tokenServiceBtn;
    private Button paymentFragmentBtn;
    private Button scannerActivityBtn;
    private Button secondScreenServiceActivityBtn, secondScreenServiceV2ActivityBtn;
    private Button receiptPrintingServiceActivityBtn;
    private Button productServiceActivityBtn;
    private Button businessServiceActivityBtn;
    private Button billingServiceActivityBtn;
    private Button accessoriesActivityBtn;
    private Button cameraActivityBtn;
    private Button nonPaymentCardReaderActivityBtn;
    private Button charge;
    private TextView prompt;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // triggers transaction sync; requires permission poynt.permission.LAST_SYNC_TIME_INFO in manifest
//        Intent sendingIntent = new Intent(Intents.ACTION_SYNC_TRANSACTIONS_FROM_CLOUD);
//        sendBroadcast(sendingIntent);

        setContentView(R.layout.activity_main);

//        Button contentProviderBtn = (Button) findViewById(R.id.contentProviderBtn);
//        contentProviderBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, ContentProviderSampleActivity.class);
//                startActivity(intent);
//            }
//        });
        transactionListBtn = (Button) findViewById(R.id.transactionListBtn);
        transactionListBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
                startActivity(intent);
            }
        });
        transactionListBtn.setVisibility(GONE);

        terminalUserLoginBtn = (Button) findViewById(R.id.terminalUserLoginBtn);
        terminalUserLoginBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        terminalUserLoginBtn.setVisibility(GONE);

        orderBtn = (Button) findViewById(R.id.orderBtn);
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                startActivity(intent);
            }
        });
        orderBtn.setVisibility(GONE);

        tokenServiceBtn = (Button) findViewById(R.id.tokenServiceBtn);
        tokenServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TokenServiceActivity.class);
                startActivity(intent);
            }
        });
        tokenServiceBtn.setVisibility(GONE);

        paymentFragmentBtn = (Button) findViewById(R.id.paymentFragmentBtn);
        paymentFragmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });
        paymentFragmentBtn.setVisibility(GONE);

        scannerActivityBtn = (Button) findViewById(R.id.scannerActivityBtn);
        scannerActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivity(intent);
            }
        });
        scannerActivityBtn.setVisibility(GONE);

        secondScreenServiceActivityBtn = (Button) findViewById(R.id.secondScreenServiceActivityBtn);
        secondScreenServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondScreenServiceActivity.class);
                startActivity(intent);
            }
        });
        secondScreenServiceActivityBtn.setVisibility(GONE);

        secondScreenServiceV2ActivityBtn = (Button) findViewById(R.id.secondScreenServiceV2ActivityBtn);
        secondScreenServiceV2ActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondScreenServiceV2Activity.class);
                startActivity(intent);
            }
        });

        secondScreenServiceV2ActivityBtn.setVisibility(GONE);

        receiptPrintingServiceActivityBtn = (Button) findViewById(R.id.receiptPrintingServiceActivityBtn);
        receiptPrintingServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReceiptPrintingServiceActivity.class);
                startActivity(intent);
            }
        });
        receiptPrintingServiceActivityBtn.setVisibility(GONE);

        productServiceActivityBtn = (Button) findViewById(R.id.productServiceActivityBtn);
        productServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProductServiceActivity.class);
                startActivity(intent);
            }
        });
        productServiceActivityBtn.setVisibility(GONE);

        businessServiceActivityBtn = (Button) findViewById(R.id.businessServiceActivityBtn);
        businessServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BusinessServiceActivity.class);
                startActivity(intent);
            }
        });
        businessServiceActivityBtn.setVisibility(GONE);

        billingServiceActivityBtn = (Button) findViewById(R.id.billingServiceActivityBtn);
        billingServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InAppBillingActivity.class);
                startActivity(intent);
            }
        });
        billingServiceActivityBtn.setVisibility(GONE);

        accessoriesActivityBtn = (Button) findViewById(R.id.accessoriesActivityBtn);
        accessoriesActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AccessoriesActivity.class);
                startActivity(intent);
            }
        });
        accessoriesActivityBtn.setVisibility(GONE);

        cameraActivityBtn = (Button) findViewById(R.id.cameraActivityBtn);
        cameraActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
        cameraActivityBtn.setVisibility(GONE);

        nonPaymentCardReaderActivityBtn = (Button) findViewById(R.id.nonPaymentCardReaderActivityBtn);
        nonPaymentCardReaderActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NonPaymentCardReaderActivity.class);
                startActivity(intent);
            }
        });
        nonPaymentCardReaderActivityBtn.setVisibility(GONE);

        charge = (Button)findViewById(R.id.charge);
        prompt = (TextView) findViewById(R.id.prompt);
        charge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prompt.setText(("INSERT CARD"));
                charge.setEnabled(false);


                // register receiver
                br = new MyBR();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intents.ACTION_CARD_FOUND);
                registerReceiver(br, intentFilter);

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        return super.onOptionsItemSelected(item);
    }

    private static final int COLLECT_PAYMENT_RESULT_CODE = 12312;
    private MyBR br;
    class MyBR extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BROADCAST", "onReceive: " + intent.getAction());

            if(Intents.ACTION_CARD_FOUND.equals(intent.getAction())) {
                unregisterReceiver(br);
                br = null;
                //TODO present tip screen
                //TODO after tip is select launch payment
                Payment p = new Payment();
                p.setAmount(1000L);
                p.setTipAmount(200L);

                p.setSkipReceiptScreen(true);
                //p.setSkipSignatureScreen(true);
                //p.setSkipPaymentConfirmationScreen(true); // skips Thank you screen on P61 and shows "Processing"
                Intent i = new Intent(Intents.ACTION_COLLECT_PAYMENT);
                i.putExtra(Intents.INTENT_EXTRAS_PAYMENT, p);
                startActivityForResult(i, COLLECT_PAYMENT_RESULT_CODE);
            }
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        if(br != null) {
            unregisterReceiver(br);
        }
        prompt.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        charge.setEnabled(true);
    }
}

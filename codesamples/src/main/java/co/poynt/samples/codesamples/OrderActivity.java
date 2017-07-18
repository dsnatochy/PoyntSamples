package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.api.model.Business;
import co.poynt.api.model.ClientContext;
import co.poynt.api.model.FulfillmentStatus;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.OrderItemStatus;
import co.poynt.api.model.OrderStatus;
import co.poynt.api.model.OrderStatuses;
import co.poynt.api.model.Tax;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionInstruction;
import co.poynt.api.model.TransactionSource;
import co.poynt.api.model.TransactionStatusSummary;
import co.poynt.api.model.UnitOfMeasure;
import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.contentproviders.orders.orders.OrdersCursor;
import co.poynt.os.contentproviders.orders.orderstatuses.OrderstatusesColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;

public class OrderActivity extends Activity {
    private IPoyntOrderService orderService;
    private static final String TAG = OrderActivity.class.getName();
    private Button createOrderBtn;

    Business b;

    @Bind(R.id.pullOpenOrders)
    Button pullOpenOrders;

    private ServiceConnection orderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected ");
            orderService = IPoyntOrderService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected ");
        }
    };
    private IPoyntOrderServiceListener orderServiceListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
        }
    };

    private IPoyntOrderServiceListener createOrderServiceListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            if (poyntError == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(OrderActivity.this, "Created Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    public void onOrderButtonClicked(View view) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        createOrderBtn = (Button) findViewById(R.id.createOrderBtn);
        String orderJson = "{ \"_id\": 6192, \"amounts\": { \"capturedTotals\": { \"cashbackAmount\": 0, \"orderAmount\": 2705, \"tipAmount\": 0, \"transactionAmount\": 2705 }, \"currency\": \"AED\", \"feeTotal\": 0, \"netTotal\": 2705, \"subTotal\": 2099, \"taxTotal\": 606 }, \"context\": { \"source\": \"WEB\", \"transactionInstruction\": \"EXTERNALLY_PROCESSED\" }, \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"id\": \"65cde091-794e-4f69-bcb2-1b4f262cb042\", \"items\": [ { \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"discount\": 0, \"fee\": 0, \"id\": 1, \"name\": \"Almond Tart\", \"productId\": \"2194f1b1-b874-4318-8686-cf81e012dcce\", \"quantity\": 1.0, \"sku\": \"082011500003\", \"status\": \"FULFILLED\", \"tax\": 118, \"taxExempted\": false, \"taxes\": [ { \"amount\": 18, \"id\": \"24da929c-8f89-4bb9-b16d-46dd4fb6627b\", \"type\": \"VAT\" }, { \"amount\": 100, \"id\": \"f87a8ed9-ecd1-4ff6-aa5d-00c86e0b2b59\", \"type\": \"state tax\" } ], \"unitOfMeasure\": \"EACH\", \"unitPrice\": 350 }, { \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"discount\": 0, \"fee\": 0, \"id\": 2, \"name\": \"BBQ Chicken\", \"productId\": \"bcf1b34c-3ec8-480a-818e-1ffbd99c5048\", \"quantity\": 1.0, \"sku\": \"082011500017\", \"status\": \"FULFILLED\", \"tax\": 137, \"taxExempted\": false, \"taxes\": [ { \"amount\": 37, \"id\": \"24da929c-8f89-4bb9-b16d-46dd4fb6627b\", \"type\": \"VAT\" }, { \"amount\": 100, \"id\": \"f87a8ed9-ecd1-4ff6-aa5d-00c86e0b2b59\", \"type\": \"state tax\" } ], \"unitOfMeasure\": \"EACH\", \"unitPrice\": 749 }, { \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"discount\": 0, \"fee\": 0, \"id\": 3, \"name\": \"BitterGourd\", \"productId\": \"3866ecb2-493e-48d8-b82e-46d7429f64a9\", \"quantity\": 1.0, \"sku\": \"bittergourd\", \"status\": \"FULFILLED\", \"tax\": 115, \"taxExempted\": false, \"taxes\": [ { \"amount\": 15, \"id\": \"24da929c-8f89-4bb9-b16d-46dd4fb6627b\", \"type\": \"VAT\" }, { \"amount\": 100, \"id\": \"f87a8ed9-ecd1-4ff6-aa5d-00c86e0b2b59\", \"type\": \"state tax\" } ], \"unitOfMeasure\": \"EACH\", \"unitPrice\": 300 }, { \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"discount\": 0, \"fee\": 0, \"id\": 4, \"name\": \"BROWNIES\", \"productId\": \"db79b9ee-21c8-4489-9e76-3aea24e3644d\", \"quantity\": 1.0, \"sku\": \"082011500000\", \"status\": \"FULFILLED\", \"tax\": 118, \"taxExempted\": false, \"taxes\": [ { \"amount\": 18, \"id\": \"24da929c-8f89-4bb9-b16d-46dd4fb6627b\", \"type\": \"VAT\" }, { \"amount\": 100, \"id\": \"f87a8ed9-ecd1-4ff6-aa5d-00c86e0b2b59\", \"type\": \"state tax\" } ], \"unitOfMeasure\": \"EACH\", \"unitPrice\": 350 }, { \"createdAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }, \"discount\": 0, \"fee\": 0, \"id\": 5, \"name\": \"Cappuccino\", \"productId\": \"719d2bb0-5c62-478c-97c5-177de7b0fbd1\", \"quantity\": 1.0, \"sku\": \"082011500008\", \"status\": \"FULFILLED\", \"tax\": 118, \"taxExempted\": false, \"taxes\": [ { \"amount\": 18, \"id\": \"24da929c-8f89-4bb9-b16d-46dd4fb6627b\", \"type\": \"VAT\" }, { \"amount\": 100, \"id\": \"f87a8ed9-ecd1-4ff6-aa5d-00c86e0b2b59\", \"type\": \"state tax\" } ], \"unitOfMeasure\": \"EACH\", \"unitPrice\": 350 } ], \"statuses\": { \"fulfillmentStatus\": \"FULFILLED\", \"status\": \"COMPLETED\", \"transactionStatusSummary\": \"EXTERNALLY_PROCESSED\" }, \"taxExempted\": false, \"updatedAt\": { \"year\": 2017, \"month\": 4, \"dayOfMonth\": 23, \"hourOfDay\": 15, \"minute\": 12, \"second\": 20 }} ";
        Gson gson = new Gson();
        Type orderType = new TypeToken<Order>(){}.getType();
        final Order order = gson.fromJson(orderJson, orderType);

        createOrderBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                try {
//                    orderService.createOrder(generateOrder(), UUID.randomUUID().toString(), createOrderServiceListener);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
                //completeOrder(order);
                updateOrderTest();
            }
        });
        ButterKnife.bind(this);
    }


    public void updateOrderTest(){
        String orderJson = "{\"discounts\":[{\"amount\":-400,\"customName\":\"Order level discount\"}],\"items\":[{\"quantity\":10.0,\"id\":1,\"discounts\":[{\"amount\":50,\"customName\":\"custom discount\"}],\"unitPrice\":100,\"discount\":500,\"fee\":0,\"status\":\"FULFILLED\",\"name\":\"Small coffee\",\"sku\":\"sku12348\",\"unitOfMeasure\":\"EACH\"}],\"amounts\":{\"subTotal\":1000,\"discountTotal\":-900,\"netTotal\":100,\"currency\":\"USD\"},\"statuses\":{\"fulfillmentStatus\":\"FULFILLED\",\"status\":\"OPENED\",\"transactionStatusSummary\":\"NONE\"},\"notes\":\"will pick up at 5pm\"}";
        Type orderType = new TypeToken<Order>(){}.getType();
        Gson gson = new Gson();
        Order order = gson.fromJson(orderJson, orderType);

        final List<Order> orderList = new ArrayList();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            orderService.createOrder(order, UUID.randomUUID().toString(), new IPoyntOrderServiceListener.Stub(){
                @Override
                public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
                    orderList.add(order);
                    latch.countDown();
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        order = orderList.get(0);
//        order.getStatuses().setTransactionStatusSummary(TransactionStatusSummary.EXTERNALLY_PROCESSED);
//        order.getStatuses().setStatus(OrderStatus.COMPLETED);

        try {
            orderService.updateOrder(order.getId().toString(), order, UUID.randomUUID().toString(),
                    new IPoyntOrderServiceListener.Stub() {
                        @Override
                        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
                            System.out.println(order);
                        }


                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void completeOrder(Order order) {
        Log.i("completeOrder", order.getId().toString());

        long subtotal = 0L;
        long taxTotal = 0L;
        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);
            item.setStatus(OrderItemStatus.FULFILLED);
            subtotal += item.getUnitPrice() * item.getQuantity();
            taxTotal += item.getTax();
        }
        OrderStatuses orderStatuses = new OrderStatuses();
        orderStatuses.setStatus(OrderStatus.COMPLETED);
        orderStatuses.setFulfillmentStatus(FulfillmentStatus.FULFILLED);
        orderStatuses.setTransactionStatusSummary(TransactionStatusSummary.EXTERNALLY_PROCESSED);
        order.setStatuses(orderStatuses);
        // start
        if (order.getContext() != null) {
            order.setContext(new ClientContext());
        }
        order.getContext().setSource(TransactionSource.WEB);
        order.getContext().setTransactionInstruction(TransactionInstruction.EXTERNALLY_PROCESSED);
        // end


        TransactionAmounts capturedAmount = new TransactionAmounts();
        capturedAmount.setCashbackAmount(0L);
        capturedAmount.setOrderAmount(order.getAmounts().getNetTotal());
        capturedAmount.setTipAmount(0L);
        capturedAmount.setTransactionAmount(order.getAmounts().getNetTotal());
        //order.getAmounts().setCapturedTotals(capturedAmount);
        OrderAmounts orderAmounts = new OrderAmounts();
       // orderAmounts.setCapturedTotals(capturedAmount);
        orderAmounts.setNetTotal(order.getAmounts().getNetTotal());
        orderAmounts.setTaxTotal(taxTotal);
        orderAmounts.setFeeTotal(order.getAmounts().getFeeTotal());
        orderAmounts.setSubTotal(subtotal);
        orderAmounts.setCurrency(order.getAmounts().getCurrency());
        order.setAmounts(orderAmounts);

        Gson gson = new Gson();
        Type orderType = new TypeToken<Order>(){}.getType();
        System.out.println(gson.toJson(order, orderType));

        order.setId(UUID.randomUUID());
        try {
            orderService.completeOrder(order.getId().toString(), order, UUID.randomUUID().toString(), orderServiceListener);

            //orderService.createOrder(order, UUID.randomUUID().toString(), orderServiceListener);
//            DrawerActivity.orderService.captureOrder(order.getId().toString(), order, UUID.randomUUID().toString(), completeOrderListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    protected void onResume() {
        super.onResume();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ORDER_SERVICE),
                orderServiceConnection, BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        unbindService(orderServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
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

    private Order generateOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        List<OrderItem> items = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        // these are the only required fields for second screen display
        item1.setName("Item1");
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        item1.setUnitOfMeasure(UnitOfMeasure.EACH);
        item1.setStatus(OrderItemStatus.ORDERED);
        item1.setTax(0l);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);
        item2.setTax(0l);
        item2.setUnitOfMeasure(UnitOfMeasure.EACH);
        item2.setStatus(OrderItemStatus.ORDERED);
        items.add(item2);


        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);
        item3.setStatus(OrderItemStatus.ORDERED);
        item3.setUnitOfMeasure(UnitOfMeasure.EACH);
        item3.setTax(0l);
        items.add(item3);
        order.setItems(items);

        BigDecimal subTotal = new BigDecimal(0);
        for (OrderItem item : items) {
            BigDecimal price = new BigDecimal(item.getUnitPrice());
            price.setScale(2, RoundingMode.HALF_UP);
            price = price.multiply(new BigDecimal(item.getQuantity()));
            subTotal = subTotal.add(price);
        }

        OrderAmounts amounts = new OrderAmounts();
        amounts.setCurrency("USD");
        amounts.setSubTotal(subTotal.longValue());
        order.setAmounts(amounts);

        OrderStatuses orderStatuses = new OrderStatuses();
        orderStatuses.setStatus(OrderStatus.OPENED);
        order.setStatuses(orderStatuses);
        return order;
    }

    /**
     * @param view this method will use the local content provider to query for open orders
     *             and will pull the last order using OrderService
     */
    @OnClick(R.id.pullOpenOrders)
    public void pullOpenOrdersClicked(View view) {

        String lastOrderId = null;

        String[] mProjection = {OrderstatusesColumns.ORDERID};
        String mSelectionClause = OrderstatusesColumns.FULFILLMENTSTATUS + "= ?";
        String[] mSelectionArgs = {OrderStatus.OPENED.status()};
        String mSortOrder = null;

        Cursor cursor = getContentResolver().query(OrdersColumns.CONTENT_URI_WITH_NETTOTAL_TRXN_STATUS_OPEN,
                mProjection, mSelectionClause, mSelectionArgs, mSortOrder);
        OrdersCursor orderCursor = new OrdersCursor(cursor);
        if (orderCursor != null) {
            while (orderCursor.moveToNext()) {
//                    Log.d(TAG, "order id: " + cursor.getString(0));
//                    Log.d(TAG, "customer id: " + cursor.getString(1));
//                    Log.d(TAG, "created at: " + cursor.getString(2));
                lastOrderId = orderCursor.getOrderid();
                Log.d(TAG, "-------------------------------");
                Log.d(TAG, "order id: " + lastOrderId);
                Log.d(TAG, "customer user id: " + orderCursor.getCustomeruserid());
                Log.d(TAG, "order Number: " + orderCursor.getOrdernumber());
            }

        }
        orderCursor.close();
        cursor.close();
        try {
            if (lastOrderId != null) {
                orderService.getOrder(lastOrderId, UUID.randomUUID().toString(), orderServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

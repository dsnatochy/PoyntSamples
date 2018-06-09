package com.sampleloyaltyapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import co.poynt.api.model.Discount;
import co.poynt.api.model.Order;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.services.v1.IPoyntLoyaltyService;
import co.poynt.os.services.v1.IPoyntLoyaltyServiceListener;

public class SampleLoyaltyService extends Service {
    private static final String TAG = SampleLoyaltyService.class.getSimpleName();

    public SampleLoyaltyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntLoyaltyService.Stub mBinder = new IPoyntLoyaltyService.Stub() {

        @Override
        public void process(Payment payment,
                            String requestId,
                            IPoyntLoyaltyServiceListener iPoyntLoyaltyServiceListener)
                throws RemoteException {
            Log.d(TAG, "process(): " + payment);
            // add a discount to the order
            Order order = payment.getOrder();
            if (order != null) {

                // uncomment this block if you want to apply discount without launching an activity
                // to collect additional input

                List<Discount> discounts = order.getDiscounts();
                if (discounts == null) {
                    discounts = new ArrayList<>();
                }

                long discountAmount = 100L;
                Discount discount = new Discount();
                // add one dollar discount at order level
                // NOTE: discount amount is always negative
                discount.setAmount(-discountAmount);
                discount.setCustomName("Loyalty Discount");
                discount.setProcessor(getPackageName());
                discounts.add(discount);
                order.setDiscounts(discounts);
                // discount total should be updated
                long discountTotal = order.getAmounts().getDiscountTotal() == null ? -discountAmount :
                        order.getAmounts().getDiscountTotal() - discountAmount;
                order.getAmounts().setDiscountTotal(discountTotal);
                // update the over all total
                long orderTotal = order.getAmounts().getNetTotal();
                order.getAmounts().setNetTotal(orderTotal + discountTotal);
                payment.setAmount(orderTotal + discountTotal);
                Log.d(TAG, "Discount added to order: " + payment);
                iPoyntLoyaltyServiceListener.loyaltyApplied(payment, requestId);



                // if you want to collect additional info before processing
//                Intent intent = new Intent(Intents.ACTION_PROCESS_LOYALTY);
//                intent.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
//                intent.putExtra("payment", payment);
//                iPoyntLoyaltyServiceListener.onLaunchActivity(intent, requestId);
            } else {
                Log.d(TAG, "No Discount added - order is required to add discounts");
                iPoyntLoyaltyServiceListener.noLoyaltyApplied(requestId);
            }


        }
    };
}

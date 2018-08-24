package co.poynt.samplegiftcardprocessor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v2.IPoyntSecondScreenService;
import co.poynt.os.services.v2.IPoyntTipListener;
import co.poynt.samplegiftcardprocessor.core.TransactionManager;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomPaymentFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomPaymentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomPaymentFragment extends DialogFragment {
    private static final String ARG_TRANSACTION = "transaction";
    private Transaction transaction;
    private OnFragmentInteractionListener mListener;
    
    private static final String TAG = CustomPaymentFragment.class.getSimpleName();
    

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param transaction Transaction.
     * @return A new instance of fragment ZipCodeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomPaymentFragment newInstance(Transaction transaction) {
        CustomPaymentFragment fragment = new CustomPaymentFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRANSACTION, transaction);
        fragment.setArguments(args);
        return fragment;
    }
    
    IPoyntSecondScreenService secondScreenService;
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            secondScreenService = IPoyntSecondScreenService.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected: ");
            Bundle options = new Bundle();
            options.putString(Intents.EXTRA_MODE, Intents.EXTRA_TIP_MODE_PERCENTS);
            options.putDouble(Intents.EXTRA_TIP_PERCENT1, 10.0);
            options.putDouble(Intents.EXTRA_TIP_PERCENT2, 20.0);
            options.putDouble(Intents.EXTRA_TIP_PERCENT3, 30.0);
            try {
                secondScreenService.captureTip(transaction.getAmounts().getTransactionAmount(),
                        transaction.getAmounts().getCurrency(), options, new IPoyntTipListener.Stub(){
                    @Override
                    public void onTipAdded(long l, double v) throws RemoteException {
                        long orderAmount = transaction.getAmounts().getOrderAmount();
                        long tipAmount = orderAmount * (long)v /100;
                        transaction.getAmounts().setTipAmount(tipAmount);
                        transaction.getAmounts().setTransactionAmount(orderAmount+tipAmount);
                        proceeed();
                    }

                    @Override
                    public void onNoTip() throws RemoteException {
                        proceeed();
                    }

                    private void proceeed(){
                        TransactionManager transactionManager =
                                SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
                        Transaction processedTransaction =
                                transactionManager.processTransaction(transaction,
                                        "");
                        mListener.onFragmentInteraction(processedTransaction, null);
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

    public CustomPaymentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transaction = getArguments().getParcelable(ARG_TRANSACTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_zip_code, container, false);
       // final EditText customPaymentCode = (EditText) view.findViewById(R.id.customPaymentCode);
//
//        Button enterButton = (Button) view.findViewById(R.id.submitButton);
//        enterButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TransactionManager transactionManager =
//                        SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
//                Transaction processedTransaction =
//                        transactionManager.processTransaction(transaction,
//                                customPaymentCode.getText().toString());
//                mListener.onFragmentInteraction(processedTransaction, null);
//            }
//        });

        Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PoyntError error = new PoyntError();
                error.setCode(PoyntError.CARD_DECLINE);
                transaction.setStatus(TransactionStatus.DECLINED);
                mListener.onFragmentInteraction(transaction, error);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        getActivity().bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_SECOND_SCREEN_SERVICE_V2), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        getActivity().unbindService(connection);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Transaction transaction, PoyntError error);
    }

}

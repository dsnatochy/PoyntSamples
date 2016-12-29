package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.TlsVersion;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.SSLSocketFactory;

import co.poynt.api.model.Business;
import co.poynt.api.model.Customer;
import co.poynt.api.model.Email;
import co.poynt.api.model.EmailType;
import co.poynt.api.model.Phone;
import co.poynt.api.model.PhoneType;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.os.services.v1.IPoyntCustomerReadListener;
import co.poynt.os.services.v1.IPoyntCustomerService;
import co.poynt.samples.codesamples.api.CustomerApiInterface;
import co.poynt.samples.codesamples.api.JsonPatchElement;
import co.poynt.samples.codesamples.utils.TLSv1_2_SSLSocketFactory;
import retrofit.RestAdapter;
import retrofit.client.OkClient;


public class CustomerServiceActivity extends Activity {
    private static final String TAG = CustomerServiceActivity.class.getSimpleName();
    EditText firstName;
    EditText lastName;
    EditText phone;
    EditText email;

    TextView customerId;

    EditText attributeName;
    EditText attributeValue;

    TextView customerAttributes;

    Customer currentCustomer;
    
    IPoyntCustomerService mCustomerService;

    String businessId;
    IPoyntBusinessService mBusinessService;
    String apiEndpoint = "https://services.poynt.net";

    ServiceConnection businessServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBusinessService = IPoyntBusinessService.Stub.asInterface(service);
            Log.d(TAG, "onServiceConnected: business service connected");

            try {
                mBusinessService.getBusiness(new IPoyntBusinessReadListener.Stub() {
                    @Override
                    public void onResponse(Business business, PoyntError poyntError) throws RemoteException {
                        if (business != null){
                            businessId = business.getId().toString();
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBusinessService = null;
            Log.d(TAG, "businessService onServiceDisconnected: ");
        }
    };

    ServiceConnection customerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            mCustomerService = IPoyntCustomerService.Stub.asInterface(iBinder);
            Log.d(TAG, "onServiceConnected: ");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCustomerService = null;
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_service);

        phone = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);
        firstName = (EditText) findViewById(R.id.firstName);
        lastName  = (EditText) findViewById(R.id.lastName);

        Button createCustomerBtn = (Button) findViewById(R.id.createCustomerBtn);
        createCustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createCustomer();
            }
        });

        customerId = (TextView) findViewById(R.id.customerId);
        attributeName = (EditText) findViewById(R.id.attributeName);
        attributeValue = (EditText) findViewById(R.id.attributeValue);
        Button updateCustomerBtn = (Button) findViewById(R.id.updateAttributesBtn);
        updateCustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCustomerAttributes();
            }
        });

        customerAttributes = (TextView) findViewById(R.id.customerAttributes);
    }


    private void createCustomer() {
        Customer customer = new Customer();
        customer.setFirstName(firstName.getText().toString());
        customer.setLastName(lastName.getText().toString());
        Phone phone = new Phone();
        phone.setLocalPhoneNumber(this.phone.getText().toString());
        Map<PhoneType, Phone> phones = new HashMap<>();
        phones.put(PhoneType.HOME, phone);
        Email email = new Email();
        email.setEmailAddress(this.email.getText().toString());
        Map<EmailType, Email> emails = new HashMap<>();
        emails.put(EmailType.PERSONAL, email);
        customer.setEmails(emails);

        String requestId = UUID.randomUUID().toString();

        try {
            mCustomerService.createCustomer(customer, requestId, new IPoyntCustomerReadListener.Stub() {
                @Override
                public void onResponse(final Customer customer, final PoyntError poyntError) throws RemoteException {
                    if (poyntError != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CustomerServiceActivity.this, "Failed to create customer. Error: "
                                        + poyntError.getReason(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.d(TAG, "onResponse: error: " + poyntError);

                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CustomerServiceActivity.this, "created customer: " + customer.getId(), Toast.LENGTH_SHORT).show();
                                customerId.setText(customer.getId().toString());
                                clearCustomerForm();
                                hideKeyboard();
                                currentCustomer = customer;

                            }
                        });
                        Type customerType  = new TypeToken<Customer>(){}.getType();
                        Log.d(TAG, "created Customer: " + new Gson().toJson(customer, customerType));
                    }
                }
            });
        }catch (RemoteException e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        bindServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(customerServiceConnection);
        unbindService(businessServiceConnection);
    }

    private void bindServices(){
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CUSTOMER_SERVICE),
                customerServiceConnection, BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE),
                businessServiceConnection, BIND_AUTO_CREATE);

    }

    private void clearCustomerForm(){
        firstName.setText("");
        lastName.setText("");
        email.setText("");
        phone.setText("");
    }
    private void hideKeyboard(){
        InputMethodManager inputManager =
                (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void displayCustomerAttributes(Customer c){
        if (c != null) {
            Map<String, String> attributes = c.getAttributes();
            StringBuffer content = new StringBuffer();
            if (attributes != null) {
                Set keys = attributes.keySet();
                for (Object key : keys) {
                    Object value = attributes.get(key);
                    content.append(key + ": " + value + "\n");
                }
            }

            customerAttributes.setText(content.toString());
        }
    }

    private void updateCustomerAttributes(){
        new AsyncTask<String, Void, Customer>(){

            @Override
            protected Customer doInBackground(String... params) {
                //TODO set accessToken (use IPoyntTokenService to get one)
                String accessToken = null;
                if (accessToken == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CustomerServiceActivity.this, "Set access token first", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return null;
                }

                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Calendar.class, new JsonDeserializer<Calendar>(){
                            @Override
                            // by default gson does not know how to parse ISO8601 date into Calendar 
                            public Calendar deserialize(JsonElement json, Type typeOfT,
                                                        JsonDeserializationContext context) throws JsonParseException {

                                String iso8601date = json.getAsString();
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                                try {
                                    Date date = df.parse(iso8601date);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(date);
                                    return cal;
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                        })
                        .create();


                try {
                    Customer updatedCustomer;
                    Log.d(TAG, "from updateCustomerAttributes: businessId: " + businessId);
                    Log.d(TAG, "from updateCustomerAttributes: currentCustomer: " + currentCustomer);
                    URL url = new URL(apiEndpoint + "/businesses/" + businessId + "/customers/" + currentCustomer.getId());


                    Map <String, String> attributes = currentCustomer.getAttributes();
                    if (attributes == null){
                        attributes = new HashMap<>();
                    }

                    attributes.put(params[0], params[1]);


                    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions
                            (TlsVersion.TLS_1_2).build();
                    ConnectionSpec insecureHttpSpec = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT).build();
                    //criticalConnectionHttpClient.setConnectionSpecs(Collections.singletonList(spec));
                    List<ConnectionSpec> specList = new ArrayList<>();
                    specList.add(spec);
                    specList.add(insecureHttpSpec);
                    SSLSocketFactory sslSocketFactory = new TLSv1_2_SSLSocketFactory();

                    OkHttpClient okHttpClient = new OkHttpClient();
                    okHttpClient.setConnectionSpecs(specList);
                    okHttpClient.setSslSocketFactory(sslSocketFactory);



                    /*RestAdapter adapter = new RestAdapter.Builder()
                            .setEndpoint(apiEndpoint)
                            .setClient(new OkClient(okHttpClient))
                            .build();

                    List<JsonPatchElement> changes = new ArrayList<>();
                    JsonPatchElement change = new JsonPatchElement();
                    change.setOp("replace");
                    change.setPath("/attributes");
                    change.setValue(attributes);
                    changes.add(change);
                    CustomerApiInterface customerApi = adapter.create(CustomerApiInterface.class);
                    updatedCustomer = customerApi.updateCustomerAttributes(
                            UUID.fromString(businessId), currentCustomer.getId(),
                            "Bearer " + accessToken, UUID.randomUUID().toString(), changes
                    );

                    System.out.println(updatedCustomer);
                    return updatedCustomer;*/

                    String requestId = UUID.randomUUID().toString();

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    String json = "[{\"op\": \"replace\",\"path\": \"/attributes\",\"value\":"+ gson.toJson(attributes) + "}]";
                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .patch(body)
                            .addHeader("api-version", "1.2")
                            .addHeader("Poynt-Request-Id", requestId)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .build();

                    Response response = okHttpClient.newCall(request).execute();
                    String responseString = response.body().string();
                    System.out.println(responseString);

                    /*// have to use jackson instead of gson due to a parsing issue
                    ObjectMapper om = new ObjectMapper();
                    updatedCustomer =  om.readValue(responseString, Customer.class);*/

                    Type customerType = new TypeToken<Customer>(){}.getType();
                    updatedCustomer = gson.fromJson(responseString, customerType);
                    return updatedCustomer;
                } catch (Exception e){
                    e.printStackTrace();
                    return currentCustomer;
                }


            }

            @Override
            protected void onPostExecute(Customer customer) {
                super.onPostExecute(customer);
                currentCustomer = customer;
                displayCustomerAttributes(customer);
            }
        }.execute(attributeName.getText().toString(), attributeValue.getText().toString());

    }

}

package com.riconets.bluedrop;

import static android.content.ContentValues.TAG;

import static com.riconets.bluedrop.api.Constants.BUSINESS_SHORT_CODE;
import static com.riconets.bluedrop.api.Constants.CALLBACKURL;
import static com.riconets.bluedrop.api.Constants.PARTYB;
import static com.riconets.bluedrop.api.Constants.PASSKEY;
import static com.riconets.bluedrop.api.Constants.TRANSACTION_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidstudy.daraja.Daraja;
import com.androidstudy.daraja.DarajaListener;
//import com.androidstudy.daraja.model.AccessToken;
import com.androidstudy.daraja.model.LNMExpress;
import com.androidstudy.daraja.model.LNMResult;
import com.androidstudy.daraja.util.TransactionType;
import com.essam.simpleplacepicker.MapActivity;
import com.essam.simpleplacepicker.utils.SimplePlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.riconets.bluedrop.Adapters.CartAdapter;
import com.riconets.bluedrop.api.ApiClient;
import com.riconets.bluedrop.api.Model.AccessToken;
import com.riconets.bluedrop.api.Model.STKPush;
import com.riconets.bluedrop.api.Utils;
import com.riconets.bluedrop.model.CartModel;
import com.riconets.bluedrop.model.OrderModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class Cart extends AppCompatActivity {
    private ApiClient apiClient;
    private RecyclerView cartRecyclerView;
    private TextView priceTxt,AmountTv,CoordinatesTxt,AddressTxt;
    private Button PlaceOrderBtn,ShopNowBtn;
    String addressLatitude,addressLongitude,address;
    String PhoneNumber;
    String Country;
    String Language,Customer_Key,Customer_Secret;
    String []mSupportedAreas={""};
    List<CartModel> cartList;
    ImageView ForwardImageView;
    public Boolean locationPermission=false;
    public static final int Location_Request_Code=1234;
    CartAdapter cartAdapter;
    DatabaseReference databaseReference,mRef,reference;
//    Daraja daraja;
    RelativeLayout noItemLayout;
    ProgressDialog progressDialog,mprogressDialog,mapProgressDialog;
    FirebaseAuth mAuth;
    int totalPrice;
    LinearLayout paymentLayout;
    String VendorID,UID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        //Binding Views
        PlaceOrderBtn=findViewById(R.id.payBtn);
        priceTxt=findViewById(R.id.TotalPriceTxt);
        AmountTv=findViewById(R.id.amountTV);
        cartRecyclerView=findViewById(R.id.cartRecyclerView);
        cartList=new ArrayList<>();
        progressDialog=new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        cartRecyclerView.setHasFixedSize(true);
        paymentLayout=findViewById(R.id.paymentLayout);
        AddressTxt=findViewById(R.id.AddressTxt);
        ForwardImageView=findViewById(R.id.forwardBtn);
        mprogressDialog=new ProgressDialog(this);
        mprogressDialog.setCanceledOnTouchOutside(false);
        mprogressDialog.setIndeterminate(true);
        mprogressDialog.setTitle("Please Wait");
        mprogressDialog.setMessage("Opening Sim Tool Kit");
        mapProgressDialog=new ProgressDialog(this);
        mapProgressDialog.setMessage("Opening Map");
        mapProgressDialog.setTitle("Please Wait");
        ShopNowBtn=findViewById(R.id.shopNowBtn);
        mapProgressDialog.setCanceledOnTouchOutside(false);
        mapProgressDialog.setIndeterminate(true);
        noItemLayout=findViewById(R.id.EmptyCartLayout);
        Customer_Key=getString(R.string.customer_Key);
        Customer_Secret=getString(R.string.customer_secret);
        mAuth=FirebaseAuth.getInstance();
        apiClient=new ApiClient();
        apiClient.setIsDebug(true);
        UID=mAuth.getUid();
        ShopNowBtn.setOnClickListener(v12 -> {
            Home home=new Home();
            FragmentTransaction fragmentTransaction1= getSupportFragmentManager().beginTransaction();
            fragmentTransaction1.replace(R.id.content,home,"");
            fragmentTransaction1.addToBackStack(null);
            fragmentTransaction1.commit();
        });
        getVendorId();
        //            Daraja Intialization
//        daraja= Daraja.with(Customer_Key, Customer_Secret, new DarajaListener<AccessToken>() {
//            @Override
//            public void onResult(@NonNull AccessToken accessToken) {
//                Log.i(getActivity().getClass().getSimpleName(), accessToken.getAccess_token());
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(getActivity().getClass().getSimpleName(), error);
//            }
//        });
//            getAccessToken();
        databaseReference= FirebaseDatabase.getInstance().getReference("Cart");
        mRef=FirebaseDatabase.getInstance().getReference("Order");
        cartAdapter=new CartAdapter(Cart.this,cartList,priceTxt);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        getCartItems();
        getCustomerPhoneNumber();
        PlaceOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cartList.size() < 0) {
                    Toast.makeText(Cart.this, "0 items can not be added to the cart", Toast.LENGTH_SHORT).show();
                } else {
                    if (VendorID == null){
                        Toast.makeText(Cart.this,"Please Try Again",Toast.LENGTH_SHORT).show();
                    }else if(addressLatitude == null && addressLongitude == null && address == null){
                        Toast.makeText(Cart.this,"Please Choose Shipping Address",Toast.LENGTH_SHORT).show();
                    }
                    else{
//                        MakeOrderPayment();
                    }
                }
            }
        });
        ForwardImageView.setOnClickListener(v1 -> {
            if(locationEnabled()) {
                if (checkMapPermission(Cart.this, 1, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    selectlocation();
                }
            }else{
                AlertDialog.Builder locationEnable=new AlertDialog.Builder(Cart.this);
                locationEnable.setTitle("GPS Enable");
                locationEnable.setIcon(R.drawable.ic_location);
                locationEnable.setMessage( "Please Turn on Locations" );
                locationEnable.setCancelable(false);
                locationEnable.setPositiveButton( "Settings" , new
                        DialogInterface.OnClickListener() {
                            @Override
                            public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                            }
                        });
                locationEnable.setNegativeButton( "Cancel" , null );
                AlertDialog alertDialog=locationEnable.create();
                alertDialog.show() ;
            }
        });
    }
    private boolean locationEnabled() {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (gps_enabled && network_enabled) {
            return true;
        }
        return false;
    }
    private void getAccessToken() {
        apiClient.setGetAccessToken(true);
        apiClient.mpesaService().getAccessToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(@NonNull Call<AccessToken> call, @NonNull Response<AccessToken> response) {

                if (response.isSuccessful()) {
                    AccessToken accessToken=response.body();
                    apiClient.setAuthToken(String.valueOf(accessToken));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccessToken> call, @NonNull Throwable t) {

            }
        });
    }
    private void saveOrder(){
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy @HH:mm", Locale.US);
        String date = dateFormat.format(new Date());
        String OrderId = "BD0" + System.currentTimeMillis();
        String OrderTimeStamp = String.valueOf(System.currentTimeMillis());
        String cost = priceTxt.getText().toString();
        OrderModel order = new OrderModel(OrderId, "In Progress", address, addressLatitude, addressLongitude, cost, date, VendorID, UID);
        mRef.child(VendorID).child(OrderId).setValue(order).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    for (int n = 0; n < cartList.size(); n++) {
                        String CardID = cartList.get(n).getCartID();
                        String ProductId = cartList.get(n).getProductID();
                        HashMap<String, String> map = new HashMap<>();
                        map.put("ProductId", ProductId);
                        map.put("ProductName", cartList.get(n).getName());
                        map.put("ProductQuantity", cartList.get(n).getProductQuantity());
                        map.put("ProductPrice", cartList.get(n).getProductPrice());
                        map.put("TotalPrice", cartList.get(n).getTotalPrice());
                        mRef.child(VendorID).child(OrderId).child("Items").child(ProductId)
                                .setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    DatabaseReference database = FirebaseDatabase.getInstance().getReference("Cart").child(UID).child(CardID);
                                    database.removeValue();
                                }
                            }
                        });
                    }
                    Log.d(TAG, "onComplete: Place Order");
                    Intent intent = new Intent(getApplicationContext(), Order.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("VendorID", VendorID);
                    bundle.putString("TimeStamp", OrderTimeStamp);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
    }
    private void MakeOrderPayment() {
//        //to implement Customer to Business
//        progressDialog.show();
//        if(PhoneNumber!=null) {
//            LNMExpress lnmExpress = new LNMExpress(
//                    "174379",
//                    "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919",  //https://developer.safaricom.co.ke/test_credentials
//                    TransactionType.CustomerPayBillOnline, // TransactionType.CustomerPayBillOnline  <- Apply any of these two
//                    "1",
//                    "254708374149",
//                    "174379",
//                    "0114486824",
//                    "https://mydomain.com/path",
//                    "001ABC",
//                    "Goods Payment"
//            );
//            daraja.requestMPESAExpress(lnmExpress,
//                    new DarajaListener<LNMResult>() {
//                        @Override
//                        public void onResult(@NonNull LNMResult lnmResult) {
//                            Log.i(getActivity().getClass().getSimpleName(), lnmResult.ResponseDescription);
//                            Toast.makeText(getActivity(), lnmResult.ResponseDescription, Toast.LENGTH_LONG).show();
//                            mprogressDialog.dismiss();
//                            saveOrder();
//                        }
//
//                        @Override
//                        public void onError(String error) {
//                            Log.i(getActivity().getClass().getSimpleName(), error);
//                        }
//                    });
//        }
//        else{
//            Log.d(TAG, "MakeOrderPayMent: Phone Number is null");
//            mprogressDialog.dismiss();
//        }
    }
    private void sendSTKPUSH(String phoneNumber, String totalPrice) {
        mprogressDialog.setMessage("Processing Your Message");
        mprogressDialog.setTitle("Please Wait");
        mprogressDialog.setIndeterminate(true);
        mprogressDialog.setCanceledOnTouchOutside(false);
        mprogressDialog.show();
        String TimeStamp = Utils.getTimestamp();
        STKPush stkPush = new STKPush(
                BUSINESS_SHORT_CODE,
                Utils.getPassword(BUSINESS_SHORT_CODE, PASSKEY, TimeStamp),
                TimeStamp,
                TRANSACTION_TYPE,
                totalPrice,
                Utils.sanitizePhoneNumber(phoneNumber),
                PARTYB,
                Utils.sanitizePhoneNumber(phoneNumber),
                CALLBACKURL,
                "MPESA Android Test", //Account reference
                "Testing"  //Transaction description
        );
        apiClient.setGetAccessToken(false);

        apiClient.mpesaService().sendPush(stkPush).enqueue(new Callback<STKPush>() {
            @Override
            public void onResponse(@NonNull Call<STKPush> call, @NonNull Response<STKPush> response) {
                mprogressDialog.dismiss();
                try {
                    if (response.isSuccessful()) {
                        Timber.d("post submitted to API. %s", response.body());
                    } else {
                        Timber.e("Response %s", response.errorBody().string());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(@NonNull Call<STKPush> call, @NonNull Throwable t) {
                mprogressDialog.dismiss();
                Timber.e(t);
            }

        });
    }
    private void getCustomerPhoneNumber() {
        DatabaseReference dataRef=FirebaseDatabase.getInstance().getReference("Customers");
        dataRef.child(UID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PhoneNumber=snapshot.child("phoneNumber").getValue().toString();
                Log.d(TAG, "onDataChange: "+PhoneNumber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: "+error.getMessage())  ;
            }
        });
    }
    public static boolean checkMapPermission(Activity activity, int requestCode, String permissionName) {
        if (ContextCompat.checkSelfPermission(activity,
                permissionName)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{permissionName},
                    requestCode);
        } else {
            return true;
        }
        return false;
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                selectlocation();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SimplePlacePicker.SELECT_LOCATION_REQUEST_CODE && resultCode == RESULT_OK){
            if (data != null) ShowSelectedAddress(data);
        }

    }
    private void getCartItems() {
        databaseReference.child(UID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartList.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    CartModel curCart=dataSnapshot.getValue(CartModel.class);
                    cartList.add(curCart);
                    cartRecyclerView.setAdapter(cartAdapter);
                    cartAdapter.notifyDataSetChanged();

                }
//                for(int n=0;n<cartList.size();n++){
//                    totalPrice+=Integer.parseInt(cartList.get(n).getTotalPrice());
//                }
                if(cartList.size()>0){
                    noItemLayout.setVisibility(View.GONE);
                    paymentLayout.setVisibility(View.VISIBLE);
//                    priceTxt.setText(String.valueOf(totalPrice));
                    cartRecyclerView.setVisibility(View.VISIBLE);
                }else{
                    noItemLayout.setVisibility(View.VISIBLE);
                    paymentLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void getVendorId() {
        reference= FirebaseDatabase.getInstance().getReference("Customers");
        String UserID=FirebaseAuth.getInstance().getUid();
        if(UserID!=null) {
            reference.child(UserID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    VendorID = snapshot.child("vendorID").getValue().toString();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
    private void selectlocation() {
        Country="Kenya";
        Language="English";
        String ApiKey="AIzaSyDbiqrB6Ar5n1EMhqdvk_cH0Uq2_u51EmA";
        showMap(ApiKey,Country,Language,mSupportedAreas);
    }
    private void ShowSelectedAddress(Intent data) {
        address=data.getStringExtra(SimplePlacePicker.SELECTED_ADDRESS);
        AddressTxt.setText(address);
        addressLatitude=String.valueOf(data.getDoubleExtra(SimplePlacePicker.LOCATION_LAT_EXTRA,-1));
        addressLongitude=String.valueOf(data.getDoubleExtra(SimplePlacePicker.LOCATION_LNG_EXTRA,-1));
        mapProgressDialog.dismiss();
    }
    private void showMap(String apiKey, String country, String language, String[] mSupportedAreas) {
        Intent intent=new Intent(getApplicationContext(), MapActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(SimplePlacePicker.API_KEY,apiKey);
        bundle.putString(SimplePlacePicker.COUNTRY,country);
        bundle.putString(SimplePlacePicker.LANGUAGE,language);
        bundle.putStringArray(SimplePlacePicker.SUPPORTED_AREAS,mSupportedAreas);
        intent.putExtras(bundle);
        startActivityForResult(intent, SimplePlacePicker.SELECT_LOCATION_REQUEST_CODE);
        mapProgressDialog.show();
    }
}
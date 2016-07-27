package fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.oltranz.airtime.airtime.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import client.ClientData;
import client.ClientServices;
import client.ServerClient;
import config.DeviceIdentity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import simplebeans.loginbeans.LoginRequest;
import simplebeans.loginbeans.LoginResponse;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LoginInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Login#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Login extends Fragment {
    private String tag="AirTime: "+getClass().getSimpleName();
    private String msisdn="";

    private LoginInteractionListener loginListener;
    private Typeface font;

    public Login() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginBean.
     */
    public static Login newInstance(String param1, String param2) {
        Log.d("AirTime: Login", "New Instance is creating");
        Login fragment = new Login();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Log.d(tag, "The fragment is created");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(tag, "The fragment view are being created");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.loginlayout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(tag, "View are finally inflated");
        final EditText tel=(EditText) view.findViewById(R.id.msisdn);
        tel.setTypeface(font);
        final EditText pin=(EditText) view.findViewById(R.id.pin);
        pin.setTypeface(font);
        Button btnLogin=(Button) view.findViewById(R.id.submit);
        btnLogin.setTypeface(font);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(tag, "Login Button touched");
                if (!TextUtils.isEmpty(pin.getText().toString()) && !TextUtils.isEmpty(tel.getText().toString())) {

                    //get current Time
                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    String currentTime = df.format(Calendar.getInstance().getTime());

                    //get device Identity
                    DeviceIdentity deviceIdentity = new DeviceIdentity(getContext());
                    //get user Identity
                    LoginRequest loginRequest = new LoginRequest(pin.getText().toString(),
                            tel.getText().toString(),
                            currentTime,
                            deviceIdentity.getSerialNumber(),
                            deviceIdentity.getImei(),
                            deviceIdentity.getVersion());

                    //keep the msisdn for future use
                    msisdn=loginRequest.getMsisdn();

                    //Client data to send to the Sever
                    Log.d(tag, "Data to push to the server:\n" + new ClientData().mapping(loginRequest));

                    //Dummy data
                   // loginListener.onLoginInteraction(200, "Success", null);

                    //making a Login request
                    try {
                        ClientServices clientServices = ServerClient.getClient().create(ClientServices.class);
                        Call<LoginResponse> callService = clientServices.loginUser(loginRequest);
                        callService.enqueue(new Callback<LoginResponse>() {
                            @Override
                            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                                //HTTP status code
                                int statusCode = response.code();
                                try{
                                    LoginResponse loginResponse = response.body();

                                    //handle the response from the server
                                    Log.d(tag, "Server Result:\n" + new ClientData().mapping(loginResponse));
                                    loginListener.onLoginInteraction(loginResponse.getResponseStatusSimpleBean().getStatusCode(),
                                            loginResponse.getResponseStatusSimpleBean().getMessage(),
                                            msisdn,
                                            loginResponse);
                                }catch (Exception e){
                                    loginListener.onLoginInteraction(500, e.getMessage(), msisdn, null);
                                }
                            }

                            @Override
                            public void onFailure(Call<LoginResponse> call, Throwable t) {
                                // Log error here since request failed
                                Log.e(tag, t.toString());
                                loginListener.onLoginInteraction(500, t.getMessage(), msisdn, null);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        loginListener.onLoginInteraction(500, e.getMessage(), msisdn, null);
                    }
                } else {
                    loginListener.onLoginInteraction(500, "Invalid Credential.", msisdn, null);
                }
            }
        });
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginInteractionListener) {
            loginListener = (LoginInteractionListener) context;
            font = Typeface.createFromAsset(context.getAssets(), "font/ubuntu.ttf");
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement LoginInteractionListener");
        }
        Log.d(tag,"Fragment Attached");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        loginListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface LoginInteractionListener {
        // passing the Message and Staus code to mother activity
        void onLoginInteraction(int statusCode, String message, String msisdn, LoginResponse loginResponse);
    }
}

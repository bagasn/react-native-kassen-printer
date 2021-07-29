// ReactNativeKassenPrinterModule.java

package com.kassenprinter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.service.PosprinterService;
import net.posprinter.utils.DataForSendToPrinterTSC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.facebook.react.bridge.Callback;

public class ReactNativeKassenPrinterModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    public static boolean ISCONNECT=false;
    public static IMyBinder myBinder;

    ServiceConnection mSerconnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder= (IMyBinder) service;
            show("Connected", Toast.LENGTH_LONG);
            Log.e("myBinder","connect");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("myBinder","disconnect");
        }
    };

    public ReactNativeKassenPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReactNativeKassenPrinter";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    private List<String> btList = new ArrayList<>();
    private ArrayList<String> btFoundList = new ArrayList<>();
    private ArrayAdapter<String> BtBoudAdapter ,BtfoundAdapter;
    private View BtDialogView;
    private ListView BtBoundLv,BtFoundLv;
    private LinearLayout ll_BtFound;
    private AlertDialog btdialog;
    private Button btScan;
    //private DeviceReceiver BtReciever;
    private BluetoothAdapter bluetoothAdapter;

    @ReactMethod
    public void checkBluetooth(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            reactContext.startActivityForResult(intent, 1, null);

        }else{
            //show("bluetooth enabled", Toast.LENGTH_SHORT);
//      BtReciever=new DeviceReceiver(btFoundList,BtfoundAdapter,BtFoundLv);
            Intent intent =new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
        }
    }

    @ReactMethod
    public void findAvailableDevice(Callback callback){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        btList.clear();
        if(!bluetoothAdapter.isEnabled() && bluetoothAdapter!=null){
            show("Bluetooth disabled", Toast.LENGTH_SHORT);
            //checkBluetooth();
        }else{
            Set<BluetoothDevice> device = bluetoothAdapter.getBondedDevices();
            if(((Set) device).size()>0){
                //存在已经配对过的蓝牙设备
                for(Iterator<BluetoothDevice> it = device.iterator(); it.hasNext();){
                    BluetoothDevice btd=it.next();
                    //btList.add(btd.getName()+'\n'+btd.getAddress());
                    btList.add(btd.getAddress());
                    //BtBoudAdapter.notifyDataSetChanged();
                }
                //connectBT(btList.get(1));
                callback.invoke(btList);
//      printBarcode();
                //show(btList.get(1), Toast.LENGTH_LONG);
            }else{  //不存在已经配对过的蓝牙设备
                btList.add("No can be matched to use bluetooth");
                BtBoudAdapter.notifyDataSetChanged();
            }
        }
    }

    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }
}

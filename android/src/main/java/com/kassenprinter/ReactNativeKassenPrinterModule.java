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

import com.facebook.react.bridge.Promise;
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
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

public class ReactNativeKassenPrinterModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    public static boolean ISCONNECT=false;
    public static IMyBinder myBinder;

    ServiceConnection mSerconnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder= (IMyBinder) service;
            show("Connected to printer", Toast.LENGTH_LONG);
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
    public void findAvailableDevice(Promise promise){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        btList.clear();
        if(!bluetoothAdapter.isEnabled() && bluetoothAdapter!=null){
            show("Bluetooth disabled", Toast.LENGTH_SHORT);
            //checkBluetooth();
        }else{
            Set<BluetoothDevice> device = bluetoothAdapter.getBondedDevices();
            WritableArray dataBt = new WritableNativeArray();
            Integer key = 0;
            if(((Set) device).size()>0){
                //存在已经配对过的蓝牙设备
                for(Iterator<BluetoothDevice> it = device.iterator(); it.hasNext();){
                    BluetoothDevice btd=it.next();
                    //btList.add(btd.getName()+'\n'+btd.getAddress());
                    btList.add(btd.getAddress());
                    //dataBt.putString(String.valueOf("bt-"+key), btd.getAddress());
                    dataBt.pushString(String.valueOf(btd.getName() + "-" + btd.getAddress()));
                    //BtBoudAdapter.notifyDataSetChanged();
                    key++;
                }
                //connectBT(btList.get(1));
                promise.resolve(dataBt);
//      printBarcode();
                //show(btList.get(1), Toast.LENGTH_LONG);
            }else{  //不存在已经配对过的蓝牙设备
                btList.add("No can be matched to use bluetooth");
                BtBoudAdapter.notifyDataSetChanged();
            }
        }
    }

    @ReactMethod
    private void connectPrinter(String address){
        String a=address.trim();
        Intent intent =new Intent(reactContext, PosprinterService.class);
        reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
        //show(a, Toast.LENGTH_LONG);
        if (a.equals(null)||a.equals("")){
            show("Failed null", Toast.LENGTH_SHORT);
        }else {
            //show("onn here success"+a, Toast.LENGTH_SHORT);
            myBinder.ConnectBtPort(a, new TaskCallback() {
                @Override
                public void OnSucceed() {
                    ISCONNECT=true;
                    show("Sucess", Toast.LENGTH_SHORT);
                    //promise.resolve("Success");
                    printBarcode();
                }

                @Override
                public void OnFailed() {
                    ISCONNECT=false;
                    //promise.resolve("Error");
                    show("Failed error", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }

    private void printBarcode(){
        if (ISCONNECT){

            myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    show("Berhasil anjim", Toast.LENGTH_LONG);
                }

                @Override
                public void OnFailed() {
                    show("Gagal", Toast.LENGTH_LONG);

                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    // Label size
                    list.add(DataForSendToPrinterTSC.sizeBymm(50,30));
                    // gap
                    list.add(DataForSendToPrinterTSC.gapBymm(10,0));

                    // clear buffer
                    list.add(DataForSendToPrinterTSC.cls());
                    // set direction
                    list.add(DataForSendToPrinterTSC.direction(0));
                    // barcode
                    //list.add(DataForSendToPrinterTSC.barCode(40,15,"128",100,1,0,2,2,"abcdef12345"));
                    // text
                    list.add(DataForSendToPrinterTSC.text(40,10,"TSS24.BF2",0,1,1,"29072021"));
                    list.add(DataForSendToPrinterTSC.text(320,10,"TSS24.BF2",0,1,1,"1/1"));
                   //list.add(DataForSendToPrinterTSC.block(40,10,10,10,"TSS24.BF2",0,1, 1, 0, 3, "1/1"));
//                    //list.add(DataForSendToPrinterTSC.bar(40,10, 5, 80));
                    list.add(DataForSendToPrinterTSC.text(40,40,"TSS24.BF2",0,1,1,"* kopi susu"));
                    list.add(DataForSendToPrinterTSC.text(40,70,"TSS24.BF2",0,1,1,"这是测试文本 Testing testing"));
                    // print
                    list.add(DataForSendToPrinterTSC.print(1));

                    return list;
                }
            });

        }else {
            show("Ini apa anjing", Toast.LENGTH_LONG);
        }

    }
}

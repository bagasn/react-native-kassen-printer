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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReactNativeKassenPrinterModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    public static boolean ISCONNECT = false;
    public static IMyBinder myBinder;

    ServiceConnection mSerconnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (IMyBinder) service;
            show("Connected to printer", Toast.LENGTH_LONG);
            Log.e("myBinder", "connect");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("myBinder", "disconnect");
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
    private ArrayAdapter<String> BtBoudAdapter, BtfoundAdapter;
    private View BtDialogView;
    private ListView BtBoundLv, BtFoundLv;
    private LinearLayout ll_BtFound;
    private AlertDialog btdialog;
    private Button btScan;
    //private DeviceReceiver BtReciever;
    private BluetoothAdapter bluetoothAdapter;


    @ReactMethod
    public void checkBluetooth() {
        //This method will check and connect to printer or nor.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            reactContext.startActivityForResult(intent, 1, null);

        } else {
            //show("bluetooth enabled", Toast.LENGTH_SHORT);
//      BtReciever=new DeviceReceiver(btFoundList,BtfoundAdapter,BtFoundLv);
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);

        }
    }

    @ReactMethod
    public void findAvailableDevice(Promise promise) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btList.clear();
        if (!bluetoothAdapter.isEnabled() && bluetoothAdapter != null) {
            show("Bluetooth disabled", Toast.LENGTH_SHORT);
            //checkBluetooth();
        } else {
            Set<BluetoothDevice> device = bluetoothAdapter.getBondedDevices();
            WritableArray dataBt = new WritableNativeArray();
            Integer key = 0;
            if (((Set) device).size() > 0) {
                //存在已经配对过的蓝牙设备
                for (Iterator<BluetoothDevice> it = device.iterator(); it.hasNext(); ) {
                    BluetoothDevice btd = it.next();
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
            } else {  //不存在已经配对过的蓝牙设备
                btList.add("No can be matched to use bluetooth");
                BtBoudAdapter.notifyDataSetChanged();
            }
        }
    }

    @ReactMethod
    private void connectPrinter(String address, final Promise promise) {
        String a = address.trim();
        Intent intent = new Intent(reactContext, PosprinterService.class);
        reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
        //show(a, Toast.LENGTH_LONG);
        if (a.equals(null) || a.equals("")) {
            show("Failed null", Toast.LENGTH_SHORT);
        } else {
            //show("onn here success"+a, Toast.LENGTH_SHORT);
            myBinder.ConnectBtPort(a, new TaskCallback() {
                @Override
                public void OnSucceed() {
                    ISCONNECT = true;
                    //show("Sucess", Toast.LENGTH_SHORT);
                    promise.resolve(true);
                    //printBarcode();
                }

                @Override
                public void OnFailed() {
                    ISCONNECT = false;
                    //promise.resolve("Error");
                    promise.resolve(false);
                    //show("Failed error", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }

    @ReactMethod
    private void print(final ReadableArray printBuffer) {
        if (ISCONNECT) {

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
                    list.add(DataForSendToPrinterTSC.sizeBymm(50, 30));
                    // gap
                    list.add(DataForSendToPrinterTSC.gapBymm(10, 0));

                    // clear buffer
                    list.add(DataForSendToPrinterTSC.cls());
                    // set direction
                    list.add(DataForSendToPrinterTSC.direction(0));
                    // barcode
                    //list.add(DataForSendToPrinterTSC.offSetBymm(40));
                    int bufferLength = printBuffer.size() + 1;
                    int paper = 0;


                    try {
                        JSONArray aaa = convertArrayToJson(printBuffer);

                        for (int i = 0; i < aaa.length(); i++) {
                            if (i == 1) {
                                list.add(DataForSendToPrinterTSC.text(340, 10, "TSS24.BF2", 0, 1, 1, "" + aaa.getString(i)));
                            } else {
                                if(aaa.getString(i).length() > 28) {

                                    int paragraphLength = 28;
                                    ArrayList<String> array = new ArrayList<String>();
                                    for (int index = 0; index < aaa.getString(i).length(); index += 28) {
                                        int endIndex = index + 28;
                                        if (endIndex > aaa.getString(i).length()) {
                                            endIndex = aaa.getString(i).length();
                                        }
                                        String paragraph = aaa.getString(i).substring(index, endIndex);
                                        array.add(paragraph);
                                    }

                                    for (String p: array) {
                                        list.add(DataForSendToPrinterTSC.text(40, 10 + paper, "TSS24.BF2", 0, 1, 1, "" + p));
                                        //System.out.println("printing 2 : "+array.get(k));
                                        paper += 25;
                                    }
                                }else{
                                    list.add(DataForSendToPrinterTSC.text(40, 10 + paper, "TSS24.BF2", 0, 1, 1, "" + aaa.getString(i)));
                                    paper += 25;
                                }

                            }


                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //list.add(DataForSendToPrinterTSC.text(40,10+paper,"TSS24.BF2",0,1,1, ""+));

                    //list.add(DataForSendToPrinterTSC.barCode(40,15,"128",100,1,0,2,2,"abcdef12345"));
                    // text
//                    list.add(DataForSendToPrinterTSC.text(40,10,"TSS24.BF2",0,1,1,"29072021"));
//                    list.add(DataForSendToPrinterTSC.text(320,10,"TSS24.BF2",0,1,1,"1/1"));
//                   //list.add(DataForSendToPrinterTSC.block(40,10,10,10,"TSS24.BF2",0,1, 1, 0, 3, "1/1"));
////                    //list.add(DataForSendToPrinterTSC.bar(40,10, 5, 80));
//                    list.add(DataForSendToPrinterTSC.text(40,40,"TSS24.BF2",0,1,1,"* kopi susu"));
//                    list.add(DataForSendToPrinterTSC.text(40,70,"TSS24.BF2",0,1,1,"这是测试文本 Testing testing"));


                    // print
                    list.add(DataForSendToPrinterTSC.print(1));

                    return list;
                }
            });

        } else {
            show("Ini apa anjing", Toast.LENGTH_LONG);
        }

    }

    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    private static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
}

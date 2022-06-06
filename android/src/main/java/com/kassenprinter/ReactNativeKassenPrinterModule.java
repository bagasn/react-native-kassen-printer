// ReactNativeKassenPrinterModule.java

package com.kassenprinter;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.kassenprinter.config.Constant;
import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.UiExecute;
import net.posprinter.service.PosprinterService;
import net.posprinter.utils.DataForSendToPrinterTSC;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    private final String TAG = "KassenPrinterModule";

    private final ReactApplicationContext reactContext;

    private BluetoothAdapter bluetoothAdapter;

    /**
     * Printer Label Session
     */

    private Boolean isConnectToPrinterLabel = false;

    private static IMyBinder mLabelBinder;

    private final ServiceConnection mLabelServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            printLog("Service to printer label is running");

            isConnectToPrinterLabel = false;
            mLabelBinder = ((IMyBinder) iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            printLog("Service to printer label is shutdown");

            isConnectToPrinterLabel = false;
            mLabelBinder = null;
        }
    };

    /**
     * Printer Label End Session
     */

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
        callback.invoke("Received numberArgument: " + numberArgument +
                " stringArgument: " + stringArgument);
    }

    @Deprecated
    @ReactMethod
    public void checkBluetooth() {
        //This method will check and connect to printer or nor.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            reactContext.startActivityForResult(intent, 1, null);
        } else {
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mLabelServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @ReactMethod
    public void findAvailableDevice(Promise promise) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled() && bluetoothAdapter != null) {
            show("Bluetooth tidak aktif");
            //checkBluetooth();
        } else {
            Set<BluetoothDevice> device = bluetoothAdapter.getBondedDevices();
            WritableArray dataBt = new WritableNativeArray();
            Integer key = 0;
            if (((Set) device).size() > 0) {
                //存在已经配对过的蓝牙设备
                for (Iterator<BluetoothDevice> it = device.iterator(); it.hasNext(); ) {
                    BluetoothDevice btd = it.next();

                    dataBt.pushString(String.valueOf(btd.getName() + "=" + btd.getAddress()));
                    key++;
                }

                promise.resolve(dataBt);
            }
        }
    }

    @ReactMethod
    private void connectPrinter(String address, final Promise promise) {
        String btAddress = address.trim();

        if (mLabelBinder == null) {
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mLabelServiceConnection, Context.BIND_AUTO_CREATE);
        }

        if (btAddress.isEmpty()) {
            show("Alamat bluetooth tidak valid", Toast.LENGTH_SHORT);
            promise.resolve(false);
        } else {
            mLabelBinder.connectBtPort(btAddress, new UiExecute() {
                @Override
                public void onsucess() {
                    isConnectToPrinterLabel = true;

                    promise.resolve(true);
                }

                @Override
                public void onfailed() {
                    isConnectToPrinterLabel = false;

                    promise.resolve(false);
                }
            });
        }
    }

    @Deprecated
    @ReactMethod
    private void print(final Integer paperSize, final ReadableArray printBuffer, final Promise promise) {
        if (isConnectToPrinterLabel) {
//            Intent intent = new Intent(reactContext, PosprinterService.class);
//            reactContext.bindService(intent, mLabelServiceConnection, Context.BIND_AUTO_CREATE);
            mLabelBinder.writeDataByYouself(new UiExecute() {
                @Override
                public void onsucess() {
                    promise.resolve(true);
                }

                @Override
                public void onfailed() {
                    promise.resolve(false);
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    // Label size
                    list.add(DataForSendToPrinterTSC.sizeBymm(paperSize, 30));
                    // gap
                    list.add(DataForSendToPrinterTSC.gapBymm(10, 0));

                    // clear buffer
                    list.add(DataForSendToPrinterTSC.cls());
                    // set direction
                    list.add(DataForSendToPrinterTSC.direction(0));

                    int bufferLength = printBuffer.size() + 1;

                    int paper = 0;

                    try {
                        JSONArray jsonArray = convertArrayToJson(printBuffer);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            list.add(DataForSendToPrinterTSC.text(
                                    10,
                                    10 + paper,
                                    "TSS24.BF2",
                                    0,
                                    1,
                                    1,
                                    "" + jsonArray.getString(i))
                            );
                            paper += 25;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // print
                    list.add(DataForSendToPrinterTSC.print(1));
                    Log.i(TAG, "Print Data: " + list);

                    return list;
                }
            });

        } else {
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void setPrinterToReady(final String address, final Promise promise) {
        printLog("NATIVE FUN - set printer to ready");
        if (!isBluetoothEnable()) {
            printLog("Bluetooth disable");
            promise.resolve(false);
//            promise.reject(Constant.ERROR_BLUETOOTH_DISABLE, "KASSEN: Printer disabled");
            return;
        }

        if (mLabelBinder == null) {
            Intent request = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(request, mLabelServiceConnection, Context.BIND_AUTO_CREATE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int timer = 0;

                    printLog("Try to bind service on " + timer + "ms");
                    while (mLabelBinder == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            printLog("While waiting the binder to bound\n" + e);
                        } finally {
                            timer += 200;
                        }
                    }

//                    promise.resolve(true);

                    printLog("Waiting for binder ready - Connecting to printer label");
                    connectToPrinterLabel(address, promise);
                }
            }).start();
        } else {
//            promise.resolve(true);

            if (isConnectToPrinterLabel) {
                promise.resolve(true);
            } else {
                printLog("Binder ready - Connecting to printer label");
                connectToPrinterLabel(address, promise);
            }
        }
    }

    @ReactMethod
    public void connectToPrinterLabel(String address, final Promise promise) {
        if (isConnectToPrinterLabel) {
            promise.resolve(true);
            return;
        }

        mLabelBinder.connectBtPort(address, new UiExecute() {
            @Override
            public void onsucess() {
                Log.i(TAG, "Connected to printer label");

                isConnectToPrinterLabel = true;
                promise.resolve(true);
            }

            @Override
            public void onfailed() {
                Log.e(TAG, "Connecting printer label failed");

                isConnectToPrinterLabel = false;
                promise.resolve(false);
//                promise.reject(Constant.ERROR_CANNOT_CONNECT_TO_PRINTER, "Cannot connect to printer");
            }
        });
    }

    @ReactMethod
    public void printLabel(
            final Integer paperWidth,
            final Integer paperHeight,
            final ReadableArray readableArray,
            final Promise promise) {
        mLabelBinder.writeDataByYouself(new UiExecute() {
            @Override
            public void onsucess() {
                promise.resolve(true);
            }

            @Override
            public void onfailed() {
                promise.resolve(false);
            }
        }, new ProcessData() {
            @Override
            public List<byte[]> processDataBeforeSend() {
                List<byte[]> list = new ArrayList<>();

                list.add(DataForSendToPrinterTSC.sizeBymm(paperWidth, paperHeight));
//                list.add(DataForSendToPrinterTSC.gapBymm(20, 0));
                list.add(DataForSendToPrinterTSC.cls());

                int lineColumnY = 0;

                try {
                    JSONArray jsonArray = convertArrayToJson(readableArray);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        list.add(DataForSendToPrinterTSC.text(
                                10,
                                10 + lineColumnY,
                                "TSS24.BF2",
                                0,
                                1,
                                1,
                                "" + jsonArray.getString(i))
                        );
                        lineColumnY += 25;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // print
                list.add(DataForSendToPrinterTSC.print(1));

                return list;
            }
        });
    }

    private Boolean isBluetoothEnable() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter.isEnabled();
    }


    public void show(String message, int duration) {
        Toast.makeText(reactContext, message, duration).show();
    }

    public void show(String message) {
        Toast.makeText(reactContext, message, Toast.LENGTH_SHORT).show();
    }

    private void printLog(Object message) {
//        System.out.println(message);
        Log.d(TAG, message.toString());
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

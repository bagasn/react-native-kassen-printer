# react-native-kassen-printer

Tested in Kassen Printer Bluetooth DT-640
Android only

## Getting started

`$ npm install react-native-kassen-printer --save`

### Mostly automatic installation

`$ react-native link react-native-kassen-printer`

## Usage
```javascript
import ReactNativeKassenPrinter from 'react-native-kassen-printer';

//Check bluetooth module if enable or disable
await ReactNativeKassenPrinter.checkBluetooth();

//Find available device bluetooth
await ReactNativeKassenPrinter.findAvailableDevice();

//Connect to printer and print the array string
await ReactNativeKassenPrinter.connectPrinter(btAddress).
  .then(async (check) => {
    if(check == false){
      //Run this if connect bluetooth printer error
    }else{
      await ReactNativeKassenPrinter.print(paperSize, [string])
    }
  }
```


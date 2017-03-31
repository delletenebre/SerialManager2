## Serial Manager 2

[Обсуждение на pccar.ru](http://pccar.ru/showthread.php?t=24120)

<img src="https://cloud.githubusercontent.com/assets/3936845/14065232/ca2985c6-f443-11e5-8cf0-37bf12f44809.png" width="240"> <img src="https://cloud.githubusercontent.com/assets/3936845/14065231/ca2776f0-f443-11e5-94c0-b82fc1c76b84.png" width="240"> <img src="https://cloud.githubusercontent.com/assets/3936845/14435783/51e91190-003b-11e6-9e5f-827bb1ac9264.png" width="240">

## Возможности
* Подключение:
  * USB
  * Bluetooth
  * WebSocket
  
* Действия при получении команды:
  * Показ уведомлений в виде "плавающего" окна
  * Запуск приложения
  * Эмуляция нажатия клавиш клавиатуры
  * Выполнение консольных (shell) команд
  * Отправка данных на контроллер
  

## Arduino → Serial Manager
Формат отправляемой команды: `<key:value>`

Пример простого скетча для ардуино:
```cpp
int counter = 0;

void setup() {
  Serial.begin(9600);
}

void loop() {
  Serial.println("<test:" + String(counter) + ">");
  counter++;
  delay(3000);
}
```

## Serial Manager → Arduino

### Запуск и завершение соединения
При включенной опции `Отправлять состояния соединения`, на контроллер, при каждом успешном
соединении, будет отправлено сообщение
`kg.serial.manager.connection_established`.

При завершении работы сервиса, на все подключенные контроллеры будет отправлено сообщение
`kg.serial.manager.connection_lost`.


### Датчик света
При включенной опции `Отправлять данные датчика освещённости`, на контроллер будут отправлены:
* `light_sensor_value:{value}`, где `{value}` значение датчика в люксах;
* `light_sensor_mode:{mode}`, где `{mode}` число от 0 до 7:
  * 0 - [LIGHT_NO_MOON](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_NO_MOON)
  * 1 - [LIGHT_FULLMOON](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_FULLMOON)
  * 2 - [LIGHT_CLOUDY](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_CLOUDY)
  * 3 - [LIGHT_SUNRISE](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_SUNRISE)
  * 4 - [LIGHT_OVERCAST](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_OVERCAST)
  * 5 - [LIGHT_SHADE](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_SHADE)
  * 6 - [LIGHT_SUNLIGHT](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_SUNLIGHT)
  * 7 - [LIGHT_SUNLIGHT_MAX](https://developer.android.com/reference/android/hardware/SensorManager.html#LIGHT_SUNLIGHT_MAX)

Сообщения датчика освещённости отправляются не чаще одного раза в 3 секунды и только при значительном изменении освещения, т.е. при
смене значения `{mode}`.



## Serial Manager → Android
Broadcast Intent'ы:
* При получении команды:
  * Action: `kg.serial.manager.command_received`
  * Extras: `key`, `value`
* При запуске программы:
  * `kg.serial.manager.app_started`
* При запуске сервиса:
  * `kg.serial.manager.started`
* При остановке сервиса:
  * `kg.serial.manager.stopped`


## Библиотеки
* [UsbSerial](https://github.com/felHR85/UsbSerial)

## Альтернативы
* [Serial Manager](https://github.com/delletenebre/SerialManager)
* [Remote Inputs Manager / Remote steering wheel control](http://forum.xda-developers.com/showthread.php?t=2635159)

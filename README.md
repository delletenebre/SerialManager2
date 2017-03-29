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
  

## Arduino → Android
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
* [Remote Inputs Manager / Remote steering wheel control](http://forum.xda-developers.com/showthread.php?t=2635159)

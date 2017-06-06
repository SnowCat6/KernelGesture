# KernelGesture
Look at https://play.google.com/store/apps/details?id=ru.vpro.kernelgesture

Реакция на жесты пробуждения телефона и активизация заданных вами действий.

Определяет нажатия от устройств ввода и в зависимости от возможности ядра обрабатывает действия.
Для большенства телефонов можно сделать кастомное ядро и в него внедрить распознование жестов для разблокировки телефона, если драйвер тачскрина это поддерживает.
Так же есть разные кастомные ядра со своей технологией жестов, их тоже можно внедрить.

## Вы можете определить как именно ваше ядро реагирует на жесты
1. Выключите телефон
2. Введите adb shell getevent -l
3. Выполните жест пробуждения
4. Вышлите начала лога мне - с перечнем устройств

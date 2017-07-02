package gesture.action

import android.graphics.drawable.Drawable
import gesture.GestureAction

/**
 * Базовый класс для всех собственных действий
 */

abstract class ActionItem(val action: GestureAction)
{
    val context = action.context
    /**
     * Начало процесса ожидание жеста
     */
    open fun onStart() {}

    /**
     * Окончание процесса ожидания жеста
     */
    open fun onStop() {}

    /**
     * Закрытие сервиса ожидания жестов и выход из программы
     */
    open fun close() {}

    /**
     * Получить строку идентификатор действия, к примеру screen.on
     */
    abstract fun action(): String

    /**
     * Определить по идентификатору действия, что действие имеет отношение к действию
     */
    open fun isAction(action: String): Boolean = action == action()

    /**
     * Вернуть название действия
     */
    open fun name(): String = action()

    /**
     * Вернуть иконку действия
     */
    open fun icon(): Drawable = action.context.getDrawable(android.R.color.transparent)

    /**
     * Выполнитть действие
     */
    abstract fun run(): Boolean
}
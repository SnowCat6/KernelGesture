package gestureDetect.action

import android.content.Context
import android.graphics.drawable.Drawable
import gestureDetect.GestureAction
import ru.vpro.kernelgesture.tools.getDrawableEx

/**
 * Базовый класс для всех собственных действий
 */

abstract class ActionItem(val action: GestureAction)
{
    /**
     * Определить доступность действий
     */
    open fun onCreate(context: Context):Boolean = true
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
    abstract fun action(context: Context): String?

    open fun isEnable(context: Context) : Boolean
            = action(context)?.isNotEmpty() ?: false
    /**
     * Определить по идентификатору действия, что действие имеет отношение к действию
     */
    open fun isAction(context: Context, action: String)
            : Boolean = action == action(context)

    /**
     * Вернуть название действия
     */
    open fun name(context: Context)
            : String? = action(context)

    /**
     * Вернуть иконку действия
     */
    open fun icon(context: Context)
            : Drawable? = context.getDrawableEx(android.R.color.transparent)

    /**
     * Выполнитть действие
     */
    abstract fun run(context: Context): Boolean
}
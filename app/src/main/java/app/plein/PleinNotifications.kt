package app.plein

import android.service.notification.NotificationListenerService

/**
 * Служба-ключ.
 *
 * Сама ничего не делает: она нужна, чтобы система пустила лаунчер к чужим
 * сессиям проигрывания. Без включённого доступа к уведомлениям
 * `MediaSessionManager.getActiveSessions` бросает исключение, и плитка
 * «сейчас играет» работать не может.
 *
 * Уведомления не читаем и никуда не отправляем.
 */
class PleinNotifications : NotificationListenerService()

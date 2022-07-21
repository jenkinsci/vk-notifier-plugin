# VkNotifier

Плагин позволяет отправлять сообщения из Jenkins в социальную сеть vk.com

## Использование

Этот плагин использует послесборочные операции для отправки уведомления

После установки нужно зайти в настройки Jenkins и установить API ключ сообщества, а так же написать куда слать
уведомления. Эти параметры можно переопределить для конкретных билдов

### Глобальная конфигурация

![Глобальная конфигурация](https://raw.githubusercontent.com/Spliterash/jenkins-vk-notifier/master/.github/images/global_configuration.png)

### Конфигурация для отдельного билда

![Конфигурация билда](https://raw.githubusercontent.com/Spliterash/jenkins-vk-notifier/master/.github/images/job_configuration.png)

## Pipeline

Так же можно отправить сообщение посредством pipeline

```groovy
stage("Notify") {
    steps {
        // Отправить сообщение о начале сборки по стандартному шаблону (peer необязательно)
        vkSendStart peer: "Optional"
        // Отправить сообщение об окончании сборки
        vkSendEnd peer: "Optional"
        // Отправить кастомное сообщение
        vkSend message: "required", peer: "Optional"
    }
}
```
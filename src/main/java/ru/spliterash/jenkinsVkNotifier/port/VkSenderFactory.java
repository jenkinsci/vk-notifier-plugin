package ru.spliterash.jenkinsVkNotifier.port;

public interface VkSenderFactory {
    VkSender create(String token);
}

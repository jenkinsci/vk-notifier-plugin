package ru.spliterash.jenkinsVkNotifier.port.simple;

import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.VkSenderFactory;

public class SimpleVkSenderFactory implements VkSenderFactory {
    @Override
    public VkSender create(String token) {
        return new SimpleVkSenderClient(token);
    }
}

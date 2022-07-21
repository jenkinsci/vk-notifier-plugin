package ru.spliterash.jenkinsVkNotifier.port;

public interface VkSender {
    void sendMessage(String peerId, String txt);
}

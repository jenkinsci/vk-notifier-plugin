package ru.spliterash.jenkinsVkNotifier;

import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import lombok.SneakyThrows;
import ru.spliterash.jenkinsVkNotifier.jenkins.defaultJob.VkNotifierPostAction;
import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.simple.SimpleVkSenderFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VkNotifierSender {
    private static final Pattern VAR_PATTERN = Pattern.compile("%(?<name>[a-zA-Z_-]*?)%");
    private final VkSender vkSender;
    private final VkNotifierPostAction.VkNotifierDescriptor descriptor;
    private final EnvVars env;
    private final Run<?, ?> build;

    @SneakyThrows
    public VkNotifierSender(VkNotifierPostAction.VkNotifierDescriptor descriptor, Run<?, ?> build, BuildListener listener) {
        this.vkSender = new SimpleVkSenderFactory().create(descriptor.getApiKey());
        this.descriptor = descriptor;
        this.build = build;
        this.env = build.getEnvironment(listener);
    }

    private String getPeer(String peer) {
        return peer == null || peer.isEmpty() ? descriptor.getDefaultPeerId() : peer;
    }

    private String prepareMessage(String message) {
        Matcher matcher = VAR_PATTERN.matcher(message);

        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            String replacement = getEnvVar(matcher.group("name"));
            builder.append(message, i, matcher.start());
            if (replacement == null)
                builder.append(matcher.group());
            else
                builder.append(replacement);
            i = matcher.end();
        }

        builder.append(message.substring(i));
        return builder.toString();
    }

    private String getEnvVar(String key) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (key) {
            case "JOB_STATUS":
                Result result = build.getResult();
                return result != null ? result.toString() : null;
            default:
                return env.get(key);
        }
    }

    public void send(String peer, String message) {
        vkSender.sendMessage(getPeer(peer), prepareMessage(message));
    }

    public void sendEnd(String peer, String message) {
        String finalMessage = message == null || message.isEmpty() ? descriptor.getDefaultEndMessage() : message;
        send(peer, finalMessage);
    }

    public void sendStart(String peerId, String message) {
        String finalMessage = message == null || message.isEmpty() ? descriptor.getDefaultStartMessage() : message;
        send(peerId, finalMessage);
    }
}

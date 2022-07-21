package ru.spliterash.jenkinsVkNotifier;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import lombok.SneakyThrows;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import ru.spliterash.jenkinsVkNotifier.jenkins.defaultJob.VkNotifierPostAction;
import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.simple.SimpleVkSenderFactory;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
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
        List<StringCredentials> list = CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());

        CredentialsMatcher matcher = CredentialsMatchers.withId(descriptor.getApiKeyCredentialId());
        StringCredentials cred = CredentialsMatchers.firstOrNull(list, matcher);

        if (cred == null)
            throw new NoSuchElementException("Cant find credential with id " + descriptor.getApiKeyCredentialId());

        this.vkSender = new SimpleVkSenderFactory().create(cred.getSecret().getPlainText());
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

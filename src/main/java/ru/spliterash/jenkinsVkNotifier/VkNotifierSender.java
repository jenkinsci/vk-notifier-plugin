package ru.spliterash.jenkinsVkNotifier;

import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import jenkins.scm.RunWithSCM;
import lombok.SneakyThrows;
import ru.spliterash.jenkinsVkNotifier.jenkins.defaultJob.VkNotifierPostAction;
import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.simple.SimpleVkSenderFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VkNotifierSender {
    private static final Pattern VAR_PATTERN = Pattern.compile("%(?<name>[a-zA-Z_-]*?)%");
    private final VkSender vkSender;
    private final VkNotifierPostAction.VkNotifierDescriptor descriptor;
    private final EnvVars env;
    private final Run<?, ?> build;

    @SneakyThrows
    public VkNotifierSender(VkNotifierPostAction.VkNotifierDescriptor descriptor, Run<?, ?> build, BuildListener listener) {
        this.vkSender = new SimpleVkSenderFactory().create(descriptor.getApiKey().getPlainText());
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
            case "JOB_CHANGES":
                return createChangesList();
            default:
                return env.get(key);
        }
    }

    private String createChangesList() {
        if (build instanceof RunWithSCM) {
            List<ChangeLogSet.Entry> changes = ((RunWithSCM<?, ?>) build).getChangeSets()
                    .stream()
                    .flatMap(c -> Arrays.stream(c.getItems()))
                    .filter(c -> c instanceof ChangeLogSet.Entry)
                    .map(c -> (ChangeLogSet.Entry) c)
                    .collect(Collectors.toList());

            if (!changes.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0, changesSize = changes.size(); i < changesSize; i++) {
                    ChangeLogSet.Entry change = changes.get(i);

                    String msg = change.getMsg().trim();
                    int nl = msg.indexOf("\n");
                    if (nl >= 0)
                        msg = msg.substring(0, nl).trim();
                    if (msg.length() > 50)
                        msg = msg.substring(0, 50);

                    String author = getAuthorName(change);

                    stringBuilder.append(msg).append(" [").append(author).append("]");

                    // Если не предпоследний элемент
                    if (i < changesSize - 1)
                        stringBuilder.append("\n");
                }

                return stringBuilder.toString();
            }
        }

        return "*** No changes. ***";
    }

    private String getAuthorName(ChangeLogSet.Entry change) {
        try {
            Class.forName("hudson.plugins.git.GitChangeSet");

            if (change instanceof GitChangeSet) {
                GitChangeSet gitChange = (GitChangeSet) change;

                return gitChange.getAuthorName();
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        return change.getAuthor().getFullName();

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

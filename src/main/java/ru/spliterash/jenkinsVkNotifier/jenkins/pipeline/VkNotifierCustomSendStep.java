package ru.spliterash.jenkinsVkNotifier.jenkins.pipeline;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import ru.spliterash.jenkinsVkNotifier.VkNotifierSender;
import ru.spliterash.jenkinsVkNotifier.jenkins.defaultJob.VkNotifierPostAction;

import java.util.Set;
@Getter
public class VkNotifierCustomSendStep extends Step {
    private final String message;
    @Setter(onMethod_ = @DataBoundSetter)
    private String peer;

    @DataBoundConstructor
    public VkNotifierCustomSendStep(String message) {
        this.message = message;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new StepExecution(context) {
            @Override
            public boolean start() {
                Jenkins jenkins = Jenkins.get();

                VkNotifierPostAction.VkNotifierDescriptor descriptor = jenkins.getDescriptorByType(VkNotifierPostAction.VkNotifierDescriptor.class);

                try {
                    new VkNotifierSender(descriptor, getContext().get(Run.class), getContext().get(BuildListener.class)).send(peer, message);
                    getContext().onSuccess(null);
                } catch (Exception exception) {
                    getContext().onFailure(exception);
                }

                return true;
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);

        }

        @Override
        public String getFunctionName() {
            return "vkSend";
        }
    }
}

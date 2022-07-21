package ru.spliterash.jenkinsVkNotifier.jenkins.defaultJob;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import ru.spliterash.jenkinsVkNotifier.VkNotifierSender;

import java.io.IOException;

@Getter
@AllArgsConstructor(onConstructor_ = {@DataBoundConstructor})
public class VkNotifierPostAction extends Notifier {
    private final String startMessage;
    private final String endMessage;
    private final String peerId;


    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        VkNotifierDescriptor descriptor = getDescriptor();

        new VkNotifierSender(descriptor, build, listener).sendStart(peerId, startMessage);

        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        VkNotifierDescriptor descriptor = getDescriptor();

        new VkNotifierSender(descriptor, build, listener).sendEnd(peerId, endMessage);

        return true;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public VkNotifierDescriptor getDescriptor() {
        return Jenkins.get().getDescriptorByType(VkNotifierDescriptor.class);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Getter
    @Setter(onMethod_ = {@DataBoundSetter})
    @Extension
    public static class VkNotifierDescriptor extends BuildStepDescriptor<Publisher> {
        private String apiKeyCredentialId;

        private String defaultPeerId;
        private String defaultStartMessage;
        private String defaultEndMessage;

        public VkNotifierDescriptor() {
            load();

            if (defaultStartMessage == null || defaultStartMessage.isEmpty())
                defaultStartMessage = "Build %JOB_BASE_NAME% with number %BUILD_NUMBER% started";

            if (defaultEndMessage == null || defaultEndMessage.isEmpty())
                defaultEndMessage = "Build %JOB_BASE_NAME% with number %BUILD_NUMBER% complete, status: %JOB_STATUS%";
        }

        @Override
        public String getDisplayName() {
            return "VK Parameters";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();

            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Restricted(NoExternalUse.class)
        public static ListBoxModel findApiKeyCredentialIdItems(@AncestorInPath Item context) {
            Jenkins jenkins = Jenkins.get();

            if (context == null && !jenkins.hasPermission(Jenkins.ADMINISTER) ||
                    context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel();
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM, context, StringCredentials.class);
        }

        public ListBoxModel doFillApiKeyCredentialIdItems(@AncestorInPath Item context) {
            return findApiKeyCredentialIdItems(context);
        }
    }
}

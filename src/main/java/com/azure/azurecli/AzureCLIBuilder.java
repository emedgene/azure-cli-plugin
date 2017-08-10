package com.azure.azurecli;

import com.azure.azurecli.exceptions.AzureCloudException;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class AzureCLIBuilder extends Builder implements SimpleBuildStep {

    private List<Command> commands;
    private String principalCredentialId;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public AzureCLIBuilder(String principalCredentialId, List<Command> commands) {
        this.commands = commands;
        this.principalCredentialId = principalCredentialId;
    }

    public AzureCLIBuilder(String principalCredentialId, List<String> strCommands, boolean dsl) {

        List<Command> commands = new ArrayList<>();
        for (String command
                :
                strCommands) {
            commands.add(new Command(command, ""));
        }
        this.commands = commands;
        this.principalCredentialId = principalCredentialId;
    }


    public List<Command> getCommands() {
        return commands;
    }

    public String getPrincipalCredentialId() {
        return principalCredentialId;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        AzureCredentials.ServicePrincipal servicePrincipal = AzureCredentials.getServicePrincipal(principalCredentialId);
        com.azure.azurecli.CredentialsCache credentialsCache = new com.azure.azurecli.CredentialsCache(servicePrincipal);
        ShellExecuter shellExecuter = new ShellExecuter(listener.getLogger());

        try {
            shellExecuter.login(credentialsCache);
            for (Command command
                    :
                    commands) {

                List<String> tokens = com.azure.azurecli.Utils.extractTokens(command.getScript());
                HashMap<String, String> replacements = new HashMap<>();
                for (String token
                        :
                        tokens) {

                    String varValue = com.azure.azurecli.Utils.getEnvVar(build.getEnvironment(listener), token);
                    if (varValue == null || varValue == "") {
                        throw AzureCloudException.create("Variable " + token + " is empty or null");
                    }
                    replacements.put(token, varValue);
                }
                String commandText = com.azure.azurecli.Utils.tokenizeText(command.getScript(), replacements);
                String output = shellExecuter.executeAZ(commandText);
                command.parseExportedVariables(listener.getLogger(), build, output);

            }
        } catch (Exception e) {
            listener.getLogger().println("Failure: " + e.getMessage());
            build.setResult(Result.FAILURE);
        }
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        public String getDisplayName() {
            ShellExecuter executer = new ShellExecuter();
            String output = null;
            try {
                output = executer.getVersion();
            } catch (AzureCloudException e) {
                return e.getMessage();
            }
            String[] result = output.split(System.lineSeparator(), 2);
            return result[0];
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillPrincipalCredentialIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            List<AzureCredentials> creds = CredentialsProvider.lookupCredentials(AzureCredentials.class, item, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
            for (AzureCredentials cred
                    :
                    creds) {
                result.add(cred.getId());
            }
            return result.includeEmptyValue()
                    .includeCurrentValue(credentialsId);
        }

    }
}


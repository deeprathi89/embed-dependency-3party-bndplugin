package hcl.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;


import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.Base64;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.commons.Externalizer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


@Component(immediate = true, service = WorkflowProcess.class, name = "Create Page From Assets", property = {
        "process.label=Create Page From Assets", "Constants.SERVICE_VENDOR=Adobe",
        "Constants.SERVICE_DESCRIPTION=Create Page From Assets"})
public class PageCreatorWorkflow implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(PageCreatorWorkflow.class);


    @Reference
    private ResourceResolverFactory resolverFactory;

    String authorLink = "";
    String dummyPage= "/content/hcl/us/en/brand1-template";

    protected enum Arguments {
        PROCESS_ARGS("PROCESS_ARGS"),
        /**
         * emailTemplate - process argument
         */
        TEMPLATE("emailTemplate"),
        /**
         * sendTo - process argument
         */
        SEND_TO("sendTo"),

        /**
         * dateFormat - process argument
         */
        DATE_FORMAT("dateFormat"),
        /**
         * password
         */
        PWD("pwd");
        private String argumentName;

        Arguments(String argumentName) {
            this.argumentName = argumentName;
        }

        public String getArgumentName() {
            return this.argumentName;
        }

    }

    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaData)
            throws WorkflowException {
        log.info("INSIDE EMAIL PROCESS STEP");
        try {

            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);


            String[] args = buildArguments(metaData);


            String payloadPath = workItem.getWorkflowData().getPayload().toString();

            //Get Assets from the folder payload path
            Resource folderResource = resolver.getResource(payloadPath);
            final Iterator<Resource> allAssetChildren = resolver.findResources(
                    String.format("SELECT * FROM [dam:Asset] AS node WHERE ISDESCENDANTNODE(node,'%s')", payloadPath),
                            javax.jcr.query.Query.JCR_SQL2
                    );

            //	copy(Page page, String destination, String beforeName, boolean shallow, boolean resolveConflict, boolean autoSave)
            //Create Pages
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            Page currentPage = (resolver.getResource(dummyPage)).adaptTo(Page.class);
            Page newPage = pageManager.copy(currentPage,"/content/hcl/us/en/test",null,false,true,true);

            //populateImages()
            //log.info("Sent is :"+sent);

        } catch (Exception e) {
            log.error("Exception in execute of sendmailwfprocess", e);
        }
    }


    protected String getValueFromArgs(String key, String[] arguments) {
        for (String str : arguments) {
            String trimmedStr = str.trim();
            if (trimmedStr.startsWith(key + ":")) {
                return trimmedStr.substring((key + ":").length());
            }
        }
        return null;
    }

    private String[] buildArguments(MetaDataMap metaData) {
        // the 'old' way, ensures backward compatibility
        String processArgs = metaData.get(Arguments.PROCESS_ARGS.getArgumentName(), String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return new String[0];
        }
    }




}

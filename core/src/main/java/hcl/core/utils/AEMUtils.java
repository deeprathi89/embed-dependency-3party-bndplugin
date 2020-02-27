package hcl.core.utils;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import javax.jcr.*;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.nio.file.InvalidPathException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AEMUtils {

    public static ResourceResolver getAdminResolver(ResourceResolverFactory resolverFactory) {
        try {
            return resolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            return null;
        }
    }

    public static <T extends Object> T getServiceReference(Class<T> clazz) {
        T t = null;
        Bundle bundle = FrameworkUtil.getBundle(clazz);
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext != null) {
                ServiceReference reference = bundleContext.getServiceReference(clazz.getCanonicalName());
                t = (T) bundleContext.getService(reference);
            }
        }
        return t;
    }

    public static Page createPageHierarchy(PageManager pageManager, String path, String pageTemplate) throws Exception {
        Page page = null;
        try {
            if (!StringUtils.startsWith(path, "/content")) {
                throw new InvalidPathException(path, "Invalid Path entered for page creation");
            }
            String[] tokens = StringUtils.split(path, "/");
            StringBuilder pathBuilder = new StringBuilder("/content");
            for (int i = 1; i < tokens.length; i++) {
                String parentPath = pathBuilder.toString();
                pathBuilder.append("/").append(tokens[i]);
                page = pageManager.getPage(pathBuilder.toString());
                if (null == page) {
                    page = pageManager.create(parentPath, tokens[i], pageTemplate, tokens[i], false);
                }
            }
        } catch (Exception e) {
            throw new Exception();
        }
        return page;
    }

    public static Resource createNodeHierarchy(ResourceResolver resolver, String path, String type, String resourceType) {
        String[] tokens = StringUtils.split(path, "/");
        StringBuilder pathBuilder = new StringBuilder();
        Resource parentResource = null;
        try {
            for (int i = 0; i < tokens.length; i++) {
                pathBuilder.append("/").append(tokens[i]);
                Resource resource = resolver.getResource(pathBuilder.toString());
                if (i == 0 && resource == null) {
                    throw new Exception("Invalid content hierarchy");
                }
                if (null == resource && i > 0) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("jcr:primaryType", type);
                    properties.put("jcr:title", tokens[i]);
                    if(StringUtils.isNotBlank(resourceType)) {
                        properties.put("sling:resourceType", resourceType);
                    }
                    resource = resolver.create(parentResource, tokens[i], properties);
                }
                parentResource = resource;
            }
            return parentResource;
        } catch (Exception e) {
            return null;
        }
    }

    public static Resource createNodeHierarchy(ResourceResolver resolver, String path, Resource parentResource,
                                               String type, boolean toBeReverseReplicated) {
        String[] tokens = StringUtils.split(path, "/");
        Resource resource = null;
        try {
            for (int i = 0; i < tokens.length; i++) {
                resource = resolver.getResource(parentResource, tokens[i]);
                if (null == resource) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("jcr:primaryType", type);
                    properties.put("cq:lastModified", Calendar.getInstance());
                    properties.put("cq:lastModifiedBy", resolver.getUserID());
                    if (toBeReverseReplicated) {
                        // properties.put("cq:distribute", true);
                    }
                    resource = resolver.create(parentResource, tokens[i], properties);
                }
                parentResource = resource;
            }
            return parentResource;
        } catch (Exception e) {
            return null;
        }
    }


    public static void setACLOnResource(ResourceResolver resolver, String principal, String path,
                                        Privilege[] privileges) throws RepositoryException {
        UserManager userMgr = resolver.adaptTo(UserManager.class);
        Session session = resolver.adaptTo(Session.class);
        Authorizable authorizable = userMgr.getAuthorizable(principal);

        JackrabbitAccessControlManager acMgr = (JackrabbitAccessControlManager) session.getAccessControlManager();

        JackrabbitAccessControlPolicy[] ps = acMgr.getPolicies(authorizable.getPrincipal());
        JackrabbitAccessControlList list = (JackrabbitAccessControlList) ps[0];

        Map<String, Value> restrictions = new HashMap<String, Value>();
        ValueFactory vf = session.getValueFactory();
        restrictions.put("rep:nodePath", vf.createValue(path, PropertyType.PATH));
        restrictions.put("rep:glob", vf.createValue("*"));
        list.addEntry(authorizable.getPrincipal(), privileges, false, restrictions);

        acMgr.setPolicy(list.getPath(), list);
        session.save();

    }

    public static void setACL(String path, Session session, boolean isAllow, String privilege, String principal)
            throws RepositoryException {
        AccessControlManager acMgr = session.getAccessControlManager();
        JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(acMgr, path);
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        if (acl != null) {
            Privilege[] privs = { acMgr.privilegeFromName(privilege) };
            Authorizable authorizable = userManager.getAuthorizable(principal);
            acl.addEntry(authorizable.getPrincipal(), privs, isAllow, null);
            acMgr.setPolicy(path, acl);
        }
        session.save();
    }
}
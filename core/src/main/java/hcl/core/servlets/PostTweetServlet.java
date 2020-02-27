/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hcl.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import hcl.core.utils.AEMUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service=Servlet.class,
           property={
                   "service.description=Create Project Servlet",
                   "sling.servlet.methods=POST",
                   "sling.servlet.paths="+ "/bin/postTweet",

           })
public class PostTweetServlet extends SlingAllMethodsServlet {

    public static final Logger LOGGER = LoggerFactory.getLogger(PostTweetServlet.class);
    static String consumerKeyStr = "UsdoPosen4Ls2qNdJTsl73DXW";
    static String consumerSecretStr = "0p5owSbzsdb1N206x6vcsZvvuPQDRMOMrKVlbaxq0joS2VTAFc";
    static String accessTokenStr = "1232895727992393728-UOBIDsv4J1gAjFQiQUMsxK6WrNKsfO";
    static String accessTokenSecretStr = "UxPrifKy43eAR6Y8T5Fgh8Rp1QXbwtUB4DZKzeqALwOT2";
    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private transient CryptoSupport cryptoSupport;
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws ServletException, IOException {
        ResourceResolver resolver = null;
        LOGGER.info("Hereeeee *****************************");
        try{  Twitter twitter = new TwitterFactory().getInstance();
        resolver = AEMUtils.getAdminResolver(this.resolverFactory);
        if(null==resolver) {
            LOGGER.info("Resolver is null");
        }
        String request="";
        if(null!=req.getParameter("assetPath")) {
             request = req.getParameter("assetPath");
        }
        String parentPath = "/var/dam/share";
        String token = getAccessToken();
        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "nt:unstructured");
        properties.put("shareJobName", "Link Share");
        properties.put("allowOriginal", true);
        properties.put("path", new String[] { request });
        Resource resource = resolver.create(resolver.getResource(parentPath),
                StringUtils.substringBefore(token, "."), properties);
            LOGGER.info("Resource created at parent path :"+parentPath);
        resolver.commit();
            String sharedLink="";
        Externalizer externalizer = resolver.adaptTo(Externalizer.class);
            if(null==externalizer || StringUtils.isEmpty(externalizer.publishLink(resolver, request))){
                 sharedLink = externalizer.publishLink(resolver, request);
            }
      //  String sharedLink = externalizer.publishLink(resolver, "/linkshare.html?sh=" + token);

            LOGGER.info("Shared Link :"+sharedLink);


        twitter.setOAuthConsumer(consumerKeyStr, consumerSecretStr);
        AccessToken accessToken = new AccessToken(accessTokenStr,
                accessTokenSecretStr);

        twitter.setOAuthAccessToken(accessToken);
          LOGGER.info("Request is :"+request);

          Resource res = resolver.getResource(request);
          Asset asset = res.adaptTo(Asset.class);
          InputStream is = asset.getRendition("original").getStream();
          LOGGER.info("Extension :"+ FilenameUtils.getExtension(request));
            LOGGER.info("Asset Name :"+asset.getName());
          File file = File.createTempFile(asset.getName(), FilenameUtils.getExtension(request));
            FileUtils.copyInputStreamToFile(is,file);
            StatusUpdate status2 = new StatusUpdate("Tweeted :"+sharedLink);

            status2.setMedia(file);

            twitter.updateStatus(status2);
      //  twitter.updateStatus(sharedLink);
          LOGGER.info("Successfully updated the status in Twitter." + sharedLink);
        System.out.println("Successfully updated the status in Twitter.");
        resp.getWriter().write("Successfuly Tweeted yessssss");
    }catch (Exception te) {
            resp.setStatus(500);
            resp.getWriter().write("Could not tweet");
          LOGGER.error("Error Occured." + te);
        te.printStackTrace();
    }

    }

    private String getAccessToken() {
        String token = UUID.randomUUID().toString().replace('-', '_');
        try {
            return token + "." + Base64.encodeBase64URLSafeString(this.cryptoSupport.hmac_sha256(token.getBytes()))
                    .replace("=", "");
        } catch (CryptoException e) {
            return null;
        }
    }


}

/**
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   This file is part of the LDP4j Project:
 *     http://www.ldp4j.org/
 *
 *   Center for Open Middleware
 *     http://www.centeropenmiddleware.com/
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Copyright (C) 2014 Center for Open Middleware.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Artifact    : org.ldp4j.framework:ldp4j-server-command:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-server-command-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.server.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.ldp4j.server.api.impl.RDFXMLMediaTypeProvider;
import org.ldp4j.server.api.impl.RuntimeInstanceImpl;
import org.ldp4j.server.api.impl.TurtleMediaTypeProvider;
import org.ldp4j.server.api.spi.IMediaTypeProvider;
import org.ldp4j.server.api.spi.RuntimeInstance;
import org.ldp4j.server.commands.Command;
import org.ldp4j.server.commands.CommandDescription;
import org.ldp4j.server.commands.CommandDescriptionUtil;
import org.slf4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;

public final class IntegrationTestHelper {

	private final Logger logger;

	private final CloseableHttpClient httpclient;
	private final CommandDescriptionUtil commandUtil;

	private URL url;

	public IntegrationTestHelper(Logger logger) {
		this.logger = logger;
		this.httpclient = HttpClients.createDefault();
		this.commandUtil = CommandDescriptionUtil.newInstance();
	}

	public void base(URL url) {
		this.url = url;
	}
	
	private URI resolve(String... path) throws URISyntaxException {
		return url.toURI().resolve(Joiner.on("/").join(Arrays.asList(path)));
	}

	private URI resourceLocation(String path) throws URISyntaxException {
		return resolve("ldp4j/api",path);
	}


	public  void executeCommand(Object command) throws Exception {
		HttpPost post = new HttpPost(resolve("ldp4j/action/"));
		post.setHeader(HttpHeaders.ACCEPT,MediaType.TEXT_PLAIN);
		post.setEntity(
			new StringEntity(
				commandUtil.
					toString(
						CommandDescription.newInstance(command)
					),
					ContentType.create(Command.MIME, "UTF-8")
				)
			);
		httpRequest(post);
	}

	public <T extends HttpUriRequest> T newRequest(String path, Class<? extends T> clazz) {
		try {
			return 
				clazz.
					getConstructor(URI.class).
						newInstance(resourceLocation(path));
		} catch (Exception e) {
			throw new IllegalStateException("Could not create request",e);
		}
	}
	
	public void httpRequest(final HttpUriRequest request) throws Exception {
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				String responseBody = logResponse(response);
				// TODO: Add validation mechanism here
				return responseBody;
			}
			
			private final String NL=System.getProperty("line.separator");
			
			private String logResponse(final HttpResponse response) throws IOException {
				HttpEntity entity = response.getEntity();
				String responseBody = entity != null ? EntityUtils.toString(entity) : null;
				if(logger.isDebugEnabled()) {
					StringBuilder builder=new StringBuilder();
					builder.append("-- REQUEST COMPLETED -------------------------").append(NL);
					builder.append("-- RESPONSE INIT -----------------------------").append(NL);
					builder.append(response.getStatusLine().toString()).append(NL);
					builder.append("-- RESPONSE HEADERS---------------------------").append(NL);
					for(org.apache.http.Header h:response.getAllHeaders()) {
						builder.append(h.getName()+" : "+h.getValue()).append(NL);
					}
					if(responseBody!=null && responseBody.length()>0) {
						builder.append("-- RESPONSE BODY -----------------------------").append(NL);
						builder.append(responseBody).append(NL);
					}
					builder.append("-- RESPONSE END ------------------------------");
					logger.debug(builder.toString());
				}
				return responseBody;
			}
		};
		logger.debug("-- REQUEST INIT -------------------------------");
		logger.debug(request.getRequestLine().toString());
		httpclient.execute(request, responseHandler);
	}

	public void shutdown() throws Exception {
		if(httpclient!=null) {
			httpclient.close();
		}
	}

	public static JavaArchive getCommandArchive() {
		JavaArchive coreArchive= 
			ShrinkWrap.
				create(JavaArchive.class,"ldp4j-server-command.jar").
				addPackages(true, "org.ldp4j.model.vocabulary").
				addPackages(true, "org.ldp4j.sdk").
				addPackages(true, "org.ldp4j.server.api").
				addPackages(true, "org.ldp4j.server.blueprints").
				addPackages(true, "org.ldp4j.server.commands").
				addPackages(true, "org.ldp4j.server.deployment").
				addPackages(true, "org.ldp4j.server.resources").
				addPackages(true, "org.ldp4j.server.templates").
				addPackages(true, "org.ldp4j.server.xml").
				addAsServiceProvider(RuntimeInstance.class, RuntimeInstanceImpl.class).
				addAsServiceProvider(IMediaTypeProvider.class,TurtleMediaTypeProvider.class,RDFXMLMediaTypeProvider.class);
		return coreArchive;
	}

}

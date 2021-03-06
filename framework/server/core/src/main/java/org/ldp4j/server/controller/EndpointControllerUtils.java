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
 *   Artifact    : org.ldp4j.framework:ldp4j-server-core:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-server-core-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.server.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.ldp4j.application.domain.LDP;
import org.ldp4j.application.engine.context.Capabilities;
import org.ldp4j.application.engine.context.PublicBasicContainer;
import org.ldp4j.application.engine.context.PublicDirectContainer;
import org.ldp4j.application.engine.context.PublicIndirectContainer;
import org.ldp4j.application.engine.context.PublicRDFSource;
import org.ldp4j.application.engine.context.PublicResource;
import org.ldp4j.application.engine.context.PublicResourceVisitor;
import org.ldp4j.application.vocabulary.Term;
import org.ldp4j.server.spi.RuntimeInstance;

public final class EndpointControllerUtils {

	private static final String ENTITY_TAG_HEADER     = "ETag";
	private static final String LAST_MODIFIED_HEADER  = "Last-Modified";
	private static final String LINK_HEADER           = "Link";
	private static final String ACCEPT_POST_HEADER    = "Accept-Post";
	private static final String ALLOW_HEADER          = "Allow";

	private EndpointControllerUtils() {
	}

	private static void addAllowedMethodHeader(ResponseBuilder builder, String method, boolean flag) {
		if(flag) {
			builder.header(EndpointControllerUtils.ALLOW_HEADER,method);
		}
	}

	/**
	 * Get a text/plain representation that indicates the acceptable media types
	 * for the specified resource. The current implementation <b>only</b> takes
	 * care of the media type, nor language, nor encodings are taken into
	 * consideration for generating the acceptable content.
	 *
	 * @param variants
	 *            The acceptable variants
	 * @param resourceLocation
	 *            The location of the resource
	 * @return A content instance that outlines the acceptable media types and
	 *         the locations from where the resource contents for those media
	 *         types can be retrieved.
	 */
	public static String getAcceptableContent(
			List<Variant> variants,
			URI resourceLocation) {
		StringBuilder builder = new StringBuilder();
		for (Variant variant : variants) {
			builder.append(variant.getMediaType()).append(" : %1$s%n");
		}
		return
			String.format(
				builder.toString(),
				resourceLocation
			);
	}

	public static void populateAllowedHeaders(ResponseBuilder builder, Capabilities capabilities) {
		// LDP 1.0 - 4.2.8.2 : "LDP servers must indicate their support for HTTP
		// Methods by responding to a HTTP OPTIONS request on the
		// LDPR’s URL with the HTTP Method tokens in the HTTP
		// response header Allow."
		addAllowedMethodHeader(builder,"OPTIONS",true);
		addAllowedMethodHeader(builder,"HEAD",true);
		addAllowedMethodHeader(builder,"GET",true);
		addAllowedMethodHeader(builder,"PUT",capabilities.isModifiable());
		addAllowedMethodHeader(builder,"PATCH",capabilities.isPatchable());
		addAllowedMethodHeader(builder,"DELETE",capabilities.isDeletable());
		addAllowedMethodHeader(builder,"POST",capabilities.isFactory());
		if(capabilities.isFactory()) {
			// LDP 1.0 - 7.1.2 : "The Accept-Post HTTP header should appear
			// in the OPTIONS response for any resource that supports the
			// use of the POST method."
			for(MediaType mediaType:RuntimeInstance.getInstance().getSupportedMediaTypes()) {
				builder.header(EndpointControllerUtils.ACCEPT_POST_HEADER,mediaType.toString());
			}
		}
	}

	public static void populateProtocolEndorsedHeaders(ResponseBuilder builder, PublicResource resource) {
		builder.header(EndpointControllerUtils.LAST_MODIFIED_HEADER,resource.lastModified());
		builder.header(EndpointControllerUtils.ENTITY_TAG_HEADER,resource.entityTag());
	}

	public static void populateProtocolSpecificHeaders(ResponseBuilder builder, PublicResource resource) {
		// LDP 1.0 - 5.2.1.4 : "LDP servers exposing LDPCs must advertise
		// their LDP support by exposing a HTTP Link header with a target
		// URI matching the type of container (see below) the server
		// supports, and a link relation type of type (that is, rel='type')
		// in all responses to requests made to the LDPC's HTTP Request-URI"
		// LDP 1.0 - 5.2.1.4 : "LDP servers may provide additional HTTP
		// Link: rel='type' headers"
		List<Term> types = resource.accept(
			new PublicResourceVisitor<List<Term>>() {
				@Override
				public List<Term> visitRDFSource(PublicRDFSource resource) {
					List<Term> base=new ArrayList<Term>();
					base.add(LDP.RESOURCE);
					return base;
				}
				@Override
				public List<Term> visitBasicContainer(PublicBasicContainer resource) {
					List<Term> terms=visitRDFSource(resource);
					terms.add(LDP.BASIC_CONTAINER);
					return terms;
				}
				@Override
				public List<Term> visitDirectContainer(PublicDirectContainer resource) {
					List<Term> terms=visitRDFSource(resource);
					terms.add(LDP.DIRECT_CONTAINER);
					return terms;
				}
				@Override
				public List<Term> visitIndirectContainer(PublicIndirectContainer resource) {
					List<Term> terms=visitRDFSource(resource);
					terms.add(LDP.INDIRECT_CONTAINER);
					return terms;
				}
			}
		);

		for(Term type:types) {
			builder.header(EndpointControllerUtils.LINK_HEADER,createLink(type, "type"));
		}
	}

	public static String createLink(Object uriRef, Object rel) {
		return String.format("<%s>; rel=\"%s\"",uriRef,rel);
	}

}

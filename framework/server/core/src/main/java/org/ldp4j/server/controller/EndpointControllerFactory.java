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

import org.ldp4j.application.engine.context.ApplicationContext;
import org.ldp4j.application.engine.context.PublicResource;

public class EndpointControllerFactory {

	private EndpointControllerFactory() {
	}

	private String normalizePath(String path) {
		String tPath=path;
		if(tPath==null) {
			tPath="";
		} else {
			tPath = tPath.trim();
		}
		return tPath;
	}

	public EndpointController createController(ApplicationContext applicationContext, String path) {
		EndpointController result=null;
		if(applicationContext==null) {
			result=new InternalFailureEndpointController();
		} else {
			PublicResource resource=applicationContext.findResource(normalizePath(path));
			if(resource!=null) {
				switch(resource.status()) {
					case GONE:
						result=new GoneEndpointController(applicationContext,resource);
						break;
					case PUBLISHED:
						result=new ExistingEndpointController(applicationContext,resource);
						break;
					default:
						throw new IllegalStateException("Unsupported status "+resource.status());
				}
			} else {
				result=new NotFoundEndpointController(applicationContext);
			}
		}
		return result;
	}

	public static EndpointControllerFactory create() {
		return new EndpointControllerFactory();
	}

}

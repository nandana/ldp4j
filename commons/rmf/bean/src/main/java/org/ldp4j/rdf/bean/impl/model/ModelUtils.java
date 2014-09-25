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
 *   Artifact    : org.ldp4j.commons.rmf:rmf-bean:1.0.0-SNAPSHOT
 *   Bundle      : rmf-bean-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.rdf.bean.impl.model;

import java.net.URI;

import org.ldp4j.rdf.Resource;
import org.ldp4j.rdf.URIRef;

final class ModelUtils {

	private ModelUtils() {
	}
	
	static enum Namespace {
		RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#",1),
		RDFS("http://www.w3.org/2000/01/rdf-schema#",2),
		OWL("http://www.w3.org/2002/07/owl#",3),
		XML_SCHEMA("http://www.w3.org/2001/XMLSchema#",4),
		UNKNOWN("UNKNOWN",5),
		;
		private final String id;
		private final int priority;
	
		Namespace(String id, int priority) {
			this.id = id;
			this.priority = priority;
		}
	
		static Namespace fromURI(URIRef uri) {
			for(Namespace namespace:values()) {
				if(uri.getIdentity().toString().startsWith(namespace.getId())) {
					return namespace;
				}
			}
			return UNKNOWN;
		}
	
		int compare(Namespace n) {
			return priority-n.priority;
		}
	
		String getId() {
			return id;
		}
	
	}

	static int compare(URIRef u1, URIRef u2) {
		int res = Namespace.fromURI(u1).compare(Namespace.fromURI(u2));
		if(res!=0) {
			return res;
		}
		return u1.toString().compareTo(u2.toString());
	}

	/**
	 * Finds the index of the first local name character in an (non-relative)
	 * URI. This index is determined by the following the following steps:
	 * <ul>
	 * <li>Find the <em>first</em> occurrence of the '#' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the '/' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the ':' character.
	 * <li>Add <tt>1<tt> to the found index and return this value.
	 * </ul>
	 * Note that the third step should never fail as every legal (non-relative)
	 * URI contains at least one ':' character to separate the scheme from the
	 * rest of the URI. If this fails anyway, the method will throw an
	 * {@link IllegalArgumentException}.
	 * 
	 * @param uri
	 *        A URI string.
	 * @return The index of the first local name character in the URI string.
	 *         Note that this index does not reference an actual character if the
	 *         algorithm determines that there is not local name. In that case,
	 *         the return index is equal to the length of the URI string.
	 * @throws IllegalArgumentException
	 *         If the supplied URI string doesn't contain any of the separator
	 *         characters. Every legal (non-relative) URI contains at least one
	 *         ':' character to separate the scheme from the rest of the URI.
	 */
	static int getLocalNameIndex(String uri) {
		int separatorIdx = uri.indexOf('#');

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf('/');
		}

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf(':');
		}

		if (separatorIdx < 0) {
			throw new IllegalArgumentException("No separator character founds in URI: " + uri);
		}

		return separatorIdx + 1;
	}

	/**
	 * Checks whether the URI consisting of the specified namespace and local
	 * name has been split correctly according to the URI splitting rules
	 * specified in {@link URI}.
	 * 
	 * @param namespace
	 *        The URI's namespace, must not be <tt>null</tt>.
	 * @param localName
	 *        The URI's local name, must not be <tt>null</tt>.
	 * @return <tt>true</tt> if the specified URI has been correctly split into a
	 *         namespace and local name, <tt>false</tt> otherwise.
	 * @see URI
	 * @see #getLocalNameIndex(String)
	 */
	static boolean isCorrectURISplit(String namespace, String localName) {
		assert namespace != null : "namespace must not be null";
		assert localName != null : "localName must not be null";

		if (namespace.length() == 0) {
			return false;
		}

		int nsLength = namespace.length();
		char lastNsChar = namespace.charAt(nsLength - 1);

		if (lastNsChar == '#') {
			// correct split if namespace has no other '#'
			return namespace.lastIndexOf('#', nsLength - 2) == -1;
		} else if (lastNsChar == '/') {
			// correct split if local name has no '/' and URI contains no '#'
			return 
				localName.indexOf('/') == -1 && 
				localName.indexOf('#') == -1 && 
				namespace.indexOf('#') == -1;
		} else if (lastNsChar == ':') {
			// correct split if local name has no ':' and URI contains no '#' or
			// '/'
			return 
				localName.indexOf(':') == -1 && 
				localName.indexOf('#') == -1 && 
				localName.indexOf('/') == -1 && 
				namespace.indexOf('#') == -1 && 
				namespace.indexOf('/') == -1;
		}

		return false;
	}

	static String getNamespace(URIRef uri) {
		String uriString = uri.getIdentity().toString();
		int localNameIdx = getLocalNameIndex(uriString);
		return uriString.substring(0, localNameIdx);
	}

	static String getLocalName(URIRef uri) {
		String uriString = uri.getIdentity().toString();
		int localNameIdx = getLocalNameIndex(uriString);
		return uriString.substring(localNameIdx);
	}
	
	static String getIdentity(Resource<?> subject) {
		return subject.getIdentity().toString();
	}
	
}
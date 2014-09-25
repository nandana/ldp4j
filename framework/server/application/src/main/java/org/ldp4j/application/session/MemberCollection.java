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
 *   Artifact    : org.ldp4j.framework:ldp4j-server-application:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-server-application-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.application.session;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.ldp4j.application.resource.Container;
import org.ldp4j.application.resource.Resource;
import org.ldp4j.application.resource.ResourceId;
import org.ldp4j.application.resource.ResourceVisitor;

import com.google.common.base.Objects;

final class MemberCollection {
	
	private final Map<ResourceId,DelegatedResourceSnapshot> members;

	private MemberCollection() {
		this.members=new LinkedHashMap<ResourceId, DelegatedResourceSnapshot>();
	}
	
	Set<DelegatedResourceSnapshot> members() {
		return Collections.unmodifiableSet(new LinkedHashSet<DelegatedResourceSnapshot>(this.members.values()));
	}

	boolean hasMember(ResourceSnapshot resource) {
		return members.containsValue(resource);
	}

	void addMember(DelegatedResourceSnapshot snapshot) {
		checkNotNull(snapshot,"Member cannot be null");
		checkState(!members.containsKey(snapshot.resourceId()),"A resource with id '%s' is already a member of the container",snapshot.resourceId());
		this.members.put(snapshot.resourceId(),snapshot);
	}

	boolean removeMember(ResourceSnapshot member) {
		if(member==null) return false;
		boolean result = this.members.containsValue(member);
		if(result) {
			this.members.remove(member.name());
		}
		return result;
	}
	
	@Override
	public String toString() {
		return 
			Objects.
				toStringHelper(getClass()).
					add("members",this.members.keySet()).
					toString();
	}

	static MemberCollection newInstance() {
		return new MemberCollection();
	}

	static MemberCollection createFromResource(Resource resource, final DelegatedWriteSession session) {
		final MemberCollection memberRepository = new MemberCollection();
		resource.accept(
			new ResourceVisitor() {
				@Override
				public void visitResource(Resource resource) {
					// Nothing to do
				}
				@Override
				public void visitContainer(Container resource) {
					for(ResourceId memberId:resource.memberIds()) {
						memberRepository.addMember(session.resolveResource(memberId));
					}
				}
			}
		);
		return memberRepository;
	}
	
}
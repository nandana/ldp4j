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
 *   Artifact    : org.ldp4j.framework:ldp4j-application-core:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-application-core-1.0.0-SNAPSHOT.jar
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

import org.ldp4j.application.resource.Attachment;
import org.ldp4j.application.resource.Resource;
import org.ldp4j.application.data.Name;

import com.google.common.base.Objects;

final class AttachmentSnapshotCollection {
	
	static final class DelegatedAttachmentSnapshot implements AttachmentSnapshot {
		
		private final String id;
		private final DelegatedResourceSnapshot resource;
		
		private DelegatedAttachmentSnapshot(String attachmentId, DelegatedResourceSnapshot resource) {
			this.id = attachmentId;
			this.resource = resource;
		}
		
		@Override
		public final String id() {
			return id;
		}
	
		@Override
		public final DelegatedResourceSnapshot resource() {
			return this.resource;
		}
	
		@Override
		public int hashCode() {
			return 
				System.identityHashCode(this.resource) +
				Objects.hashCode(this.id);
		}
	
		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}
	
		@Override
		public String toString() {
			return 
				Objects.
					toStringHelper(AttachmentSnapshot.class).
						omitNullValues().
						add("id", this.id).
						add("resource.name()", this.resource.name()).
						toString();
		}
	
	}

	public static enum Attachability {
		ATTACHABLE("Not attached") {
			@Override
			public boolean canAttach() {
				return true;
			}
		},
		ID_ALREADY_USED("AttachmentSnapshot identifier %1$s already used"),
		RESOURCE_NAME_ALREADY_USED("Resource name %2$s already used"),
		ALREADY_ATTACHED("Resource named %2$s already attached as %1$s"),
		ATTACHMENT_COLLISION("A resource named %2$s is already attached and another resource is attached as %1$s");
		;
		
		private final String message;
	
		Attachability(String message) {
			this.message = message;
		}
		
		public boolean canAttach() {
			return false;
		}
		
		String description(AttachmentSnapshot attachment) {
			return String.format(message,attachment.id(),attachment.resource().name());
		}
		
	}

	private final Set<DelegatedAttachmentSnapshot> attachments;
	private final Map<String, DelegatedAttachmentSnapshot> attachmentsById;
	private final Map<Name<?>, DelegatedAttachmentSnapshot> attachmentsByName;

	private AttachmentSnapshotCollection() {
		this.attachments=new LinkedHashSet<DelegatedAttachmentSnapshot>();
		this.attachmentsByName=new LinkedHashMap<Name<?>, DelegatedAttachmentSnapshot>();
		this.attachmentsById=new LinkedHashMap<String, DelegatedAttachmentSnapshot>();
	}
	
	private void safeAttach(DelegatedAttachmentSnapshot attachment) {
		attachments.add(attachment);
		attachmentsByName.put(attachment.resource().name(),attachment);
		attachmentsById.put(attachment.id(),attachment);
	}
	
	void add(DelegatedAttachmentSnapshot attachment) {
		checkNotNull(attachment,"Attachment snapshot cannot be null");
		AttachmentSnapshotCollection.Attachability attachability = attachability(attachment.id(),attachment.resource().name());
		checkState(attachability.canAttach(),attachability.description(attachment));
		safeAttach((DelegatedAttachmentSnapshot)attachment);
	}

	boolean remove(DelegatedAttachmentSnapshot attachment) {
		checkNotNull(attachment,"Attachment snapshot cannot be null");
		boolean result = attachments.remove(attachment);;
		if(result) {
			attachmentsByName.remove(attachment.resource().name());
			attachmentsById.remove(attachment.id());
		}
		return result;
	}
	
	Set<DelegatedAttachmentSnapshot> attachments() {
		return Collections.unmodifiableSet(new LinkedHashSet<DelegatedAttachmentSnapshot>(attachments));
	}

	DelegatedAttachmentSnapshot findByResource(ResourceSnapshot resource) {
		checkNotNull(resource,"Attached delegate cannot be null");
		DelegatedAttachmentSnapshot attachment = attachmentsByName.get(resource.name());
		if(attachment!=null && attachment.resource()!=resource) {
			attachment=null;
		}
		return attachment;
	}

	DelegatedAttachmentSnapshot findById(String attachmentId) {
		checkNotNull(attachmentId,"Attachment snapshot identifier cannot be null");
		return attachmentsById.get(attachmentId);
	}

	Attachability attachability(String attachmentId, Name<?> name) {
		Attachability result=null;
		AttachmentSnapshot aBN=attachmentsByName.get(name);
		AttachmentSnapshot aBI=attachmentsById.get(attachmentId);
		if(aBN==aBI) {
			if(aBN==null) {
				result=Attachability.ATTACHABLE;
			} else {
				result=Attachability.ALREADY_ATTACHED;
			}
		} else {
			if(aBN!=null) {
				result=Attachability.RESOURCE_NAME_ALREADY_USED;
			} else if(aBI!=null) {
				result=Attachability.ID_ALREADY_USED;
			} else {
				result=Attachability.ATTACHMENT_COLLISION;
			}
		}
		return result;
	}
			
	@Override
	public String toString() {
		return attachments.toString();
	}

	static DelegatedAttachmentSnapshot newAttachment(String attachmentId, DelegatedResourceSnapshot resource) {
		checkNotNull(attachmentId,"Attachment snapshot identifier cannot be null");
		checkNotNull(resource,"Attached resource cannot be null");
		checkNotNull(resource.name(),"Attached resource name cannot be null");
		return new DelegatedAttachmentSnapshot(attachmentId, resource);
	}

	static AttachmentSnapshotCollection createFromResource(Resource resource, DelegatedWriteSession session) {
		AttachmentSnapshotCollection repository=new AttachmentSnapshotCollection();
		for(Attachment attachment:resource.attachments()) {
			DelegatedResourceSnapshot snapshot = session.resolveResource(attachment.resourceId());
			DelegatedAttachmentSnapshot newAttachment=new DelegatedAttachmentSnapshot(attachment.id(),snapshot);
			repository.safeAttach(newAttachment);
		}
		return repository;
	}

	static AttachmentSnapshotCollection newInstance() {
		return new AttachmentSnapshotCollection();
	}

}
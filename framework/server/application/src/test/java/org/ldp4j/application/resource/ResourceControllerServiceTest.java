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
package org.ldp4j.application.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.ldp4j.application.data.IndividualReferenceBuilder.newReference;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ldp4j.application.data.DataDSL;
import org.ldp4j.application.data.DataSet;
import org.ldp4j.application.data.Name;
import org.ldp4j.application.data.NamingScheme;
import org.ldp4j.application.endpoint.Endpoint;
import org.ldp4j.application.endpoint.EndpointFactoryService;
import org.ldp4j.application.endpoint.EntityTag;
import org.ldp4j.application.example.BookContainerHandler;
import org.ldp4j.application.example.BookHandler;
import org.ldp4j.application.example.InMemoryContainerHandler.NameProvider;
import org.ldp4j.application.example.PersonHandler;
import org.ldp4j.application.ext.ResourceHandler;
import org.ldp4j.application.impl.InMemoryApplication;
import org.ldp4j.application.spi.RuntimeInstance;
import org.ldp4j.application.template.TemplateManagementService;

public class ResourceControllerServiceTest {

	private ResourceControllerService sut;
	private TemplateManagementService tms;

	private <T extends Resource> T publishResource(Class<? extends T> clazz, String templateId, Name<?> resourceName, String path) {
		T resource=ResourceFactoryService.defaultFactory().createResource(templateId,resourceName,null,clazz);
		RuntimeInstance.
		getInstance().
			getRepositoryRegistry().
				getResourceRepository().
					add(resource);
	
		Endpoint endpoint=EndpointFactoryService.defaultFactory().createEndpoint(resource,path,new EntityTag(path),new Date());
		RuntimeInstance.
			getInstance().
				getRepositoryRegistry().
					getEndpointRepository().
						add(endpoint);
		return resource;
	}

	private DataSet getInitialData() {
		DataSet initial=
			DataDSL.
				dataSet().
					individual(newReference().toLocalIndividual().named("Miguel")). // Initial context
						hasProperty("age").
							withValue(34).
						hasLink("hasFather").
							toIndividual(newReference().toLocalIndividual().named("Michel")). // New individual and context
								hasLink("hasWife").
									referringTo(newReference().toLocalIndividual().named("Consuelo")). // New individual, same context
						build(); // Create data set
		return initial;
	}

	private Name<?> name(String id) {
		return NamingScheme.getDefault().name(id);
	}

	private DataSet getUpdatedData() {
		DataSet initial=
			DataDSL.
				dataSet().
					individual(newReference().toLocalIndividual().named("Miguel")). // Initial context
						hasProperty("age").
							withValue(34).
						hasLink("hasWife").
							referringTo(newReference().toLocalIndividual().named("Ana María")).
						hasLink("hasFather").
							toIndividual(newReference().toLocalIndividual().named("Michel")). // New individual and context
								hasLink("hasWife").
									referringTo(newReference().toLocalIndividual().named("Consuelo")). // New individual, same context
						build(); // Create data set
		return initial;
	}

	private <T extends ResourceHandler> T getHandler(Class<? extends T> handlerClass) {
		return tms.getHandler(handlerClass);
	}

	@BeforeClass
	public static void setUpBefore() throws Exception {
		RuntimeInstance.setInstance(new InMemoryApplication());
		RuntimeInstance.
			getInstance().
				getServiceRegistry().
					getService(TemplateManagementService.class).
						register(PersonHandler.class);
	}

	@AfterClass
	public static void tearDownAfter() throws Exception {
		RuntimeInstance.setInstance(null);
	}

	@Before
	public void setUp() throws Exception {
		sut = 
			RuntimeInstance.
				getInstance().
					getServiceRegistry().
						getService(ResourceControllerService.class);
		tms = RuntimeInstance.getInstance().getServiceRegistry().getService(TemplateManagementService.class);
	}

	@Test
	public void testGetResource() throws Exception {
		String resourcePath = "get";
		Name<?> resourceName = name(resourcePath);
		DataSet initial = getInitialData();

		// BEGIN initialization
		Resource resource = publishResource(Resource.class,"personTemplate", resourceName, resourcePath);
		PersonHandler handler = getHandler(PersonHandler.class);
		handler.add(resourceName, initial);
		// END Initialization

		DataSet data = sut.getResource(resource);
		assertThat(data,notNullValue());
		assertThat(data,sameInstance(initial));
	}

	@Test
	public void testUpdateResource() throws Exception {
		String resourcePath = "update";
		Name<?> resourceName = name(resourcePath);
		DataSet initial = getInitialData();
		DataSet updatedDate = getUpdatedData();

		// BEGIN initialization
		Resource resource = publishResource(Resource.class,"personTemplate", resourceName, resourcePath);
		PersonHandler handler = getHandler(PersonHandler.class);
		handler.add(resourceName, initial);
		// END Initialization

		DataSet data = sut.getResource(resource);
		assertThat(data,sameInstance(initial));
		sut.updateResource(resource,updatedDate);
		data = sut.getResource(resource);
		assertThat(data,sameInstance(updatedDate));
	}

	@Test
	public void testDeleteResource() throws Exception {
		String resourcePath = "delete";
		Name<?> resourceName = name(resourcePath);
		DataSet initial = getInitialData();

		// BEGIN initialization
		Resource resource = publishResource(Resource.class,"personTemplate", resourceName, resourcePath);
		PersonHandler handler = getHandler(PersonHandler.class);
		handler.add(resourceName, initial);
		// END Initialization

		DataSet data = sut.getResource(resource);
		assertThat(data,sameInstance(initial));
		sut.deleteResource(resource);
		assertThat(handler.hasResource(resourceName),equalTo(false));
	}

	@Test
	public void testCreateResource() throws Exception {
		String resourcePath = "post";
		Name<?> resourceName = name(resourcePath);
		DataSet initialData = getInitialData();

		// BEGIN initialization
		Container resource = publishResource(Container.class,BookContainerHandler.ID, resourceName, resourcePath);
		
		NameProvider nameProvider = NameProvider.create(resourceName);
		Name<String> id = NamingScheme.getDefault().name("book1");
		nameProvider.addMemberName(id);

		BookHandler resourceHandler = getHandler(BookHandler.class);
		BookContainerHandler containerHandler = getHandler(BookContainerHandler.class);
		containerHandler.add(resourceName, null);
		containerHandler.setBookHandler(resourceHandler);
		containerHandler.addNameProvider(resourceName, nameProvider);
		// END Initialization

		Resource newResource = sut.createResource(resource,initialData);
		assertThat(newResource,notNullValue());
		assertThat((Object)newResource.id().name(),equalTo((Object)id));
		assertThat(newResource.id().templateId(),equalTo(BookHandler.ID));
		assertThat(resourceHandler.hasResource(resourceName),equalTo(false));
		DataSet data = sut.getResource(newResource);
		assertThat(data,sameInstance(initialData));
	}

}
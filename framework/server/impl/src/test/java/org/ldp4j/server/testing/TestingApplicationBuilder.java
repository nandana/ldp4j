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
 *   Artifact    : org.ldp4j.framework:ldp4j-server-impl:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-server-impl-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.server.testing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestingApplicationBuilder {

	public static enum Parameter {
		
		ResourceParameter(null, null) {
			@Override
			protected String formatValue(String value) {
				return PACKAGE_PATH.concat(value);
			}
			
		},
		SETTINGS(ResourceParameter,"settings.xml"),
		POM(ResourceParameter,"pom.xml"),
		WEB_XML(ResourceParameter,"web.xml"),
		LiteralParameter(null, null) {
		},
		DEPLOYABLE_NAME(LiteralParameter,"ldp-application.war"),
		CONTROL_PHRASE(LiteralParameter,"LDP Application"),
		BooleanParameter(null, null) {
		},
		EXCLUDE_MIDDLEWARE(BooleanParameter,"false"),
		WITH_STUBS(BooleanParameter,"false"),
		WITH_EXTENDED_STUBS(BooleanParameter,"false"),
		;

		private final String value;
		private final Parameter parameter;

		private Parameter(Parameter parameter, String resourceName) {
			this.parameter = parameter;
			this.value = resourceName;
		}
		
		public boolean isLiteral() {
			return LiteralParameter.equals(parameter);
		}

		public boolean isResource() {
			return ResourceParameter.equals(parameter);
		}
		
		public String getValue() {
			String result=value;
			if(parameter!=null) {
				result=parameter.formatValue(value);
			}
			return result;
		}

		protected String formatValue(String value) {
			return value;
		}
	}
	
	private static final Logger LOGGER=LoggerFactory.getLogger(TestingApplicationBuilder.class);

	private final Map<Parameter,String> parameters=new HashMap<Parameter, String>();

	private static final String PACKAGE_PATH;

	private JavaArchive ldpServerArchive;

	private JavaArchive stubs;

//	private JavaArchive extendedStubs;

	static {
		PACKAGE_PATH = TestingApplicationBuilder.class.getPackage().getName().replace(".", "/").concat("/");
	}
	
	private void updateParameter(Parameter parameter, String value) {
		if(value!=null) {
			String tValue = value.trim();
			if(!tValue.isEmpty()) {
				parameters.put(parameter, tValue);
			}
		}
	}

	private String retrieveParameter(Parameter parameter) {
		String result=parameters.get(parameter);
		if(result==null) {
			result=parameter.getValue();
		}
		return result;
	}

	public TestingApplicationBuilder() {
		ldpServerArchive = TestingUtil.getServerRuntimeArchive();
		stubs=TestingUtil.getServerTestingArchive();
//		extendedStubs=TestingUtil.getServerExtendedTestingArchive();
	}
	
	/**
	 * @return the settings
	 */
	public String getSettings() {
		return retrieveParameter(Parameter.SETTINGS);
	}

	/**
	 * @return the pom
	 */
	public String getPom() {
		return retrieveParameter(Parameter.POM);
	}

	/**
	 * @return the webXml
	 */
	public String getWebXml() {
		return retrieveParameter(Parameter.WEB_XML);
	}

	/**
	 * @return the deployableName
	 */
	public String getDeployableName() {
		return retrieveParameter(Parameter.DEPLOYABLE_NAME);
	}

	/**
	 * @return the controlPhrase
	 */
	public String getControlPhrase() {
		return retrieveParameter(Parameter.CONTROL_PHRASE);
	}

	/**
	 * @param settings the settings to set
	 */
	public TestingApplicationBuilder withSettings(String settings) {
		updateParameter(Parameter.SETTINGS,settings);
		return this;
	}

	/**
	 * @param pom the pom to set
	 */
	public TestingApplicationBuilder withPom(String pom) {
		updateParameter(Parameter.POM,pom);
		return this;
	}


	/**
	 * @param webXml the webXml to set
	 */
	public TestingApplicationBuilder withWebXml(String webXml) {
		updateParameter(Parameter.WEB_XML,webXml);
		return this;
	}


	/**
	 * @param deployableName the deployableName to set
	 */
	public TestingApplicationBuilder withDeployableName(String deployableName) {
		updateParameter(Parameter.DEPLOYABLE_NAME,deployableName);
		return this;
	}


	/**
	 * @param controlPhrase the controlPhrase to set
	 */
	public TestingApplicationBuilder withControlPhrase(String controlPhrase) {
		updateParameter(Parameter.CONTROL_PHRASE,controlPhrase);
		return this;
	}

	public TestingApplicationBuilder excludeMiddleware() {
		updateParameter(Parameter.EXCLUDE_MIDDLEWARE,Boolean.toString(true));
		return this;
	}

	public TestingApplicationBuilder withStubs() {
		updateParameter(Parameter.WITH_STUBS,Boolean.toString(true));
		return this;
	}

	public TestingApplicationBuilder withExtendedStubs() {
		updateParameter(Parameter.WITH_EXTENDED_STUBS,Boolean.toString(true));
		return this;
	}

	public WebArchive build(JavaArchive... archives) {
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Creating testing web application archive:");
			LOGGER.debug("- Maven configuration:");
			LOGGER.debug("  + Setting: "+getSettings());
			LOGGER.debug("  + POM....: "+getPom());
			LOGGER.debug("- Web application configuration:");
			LOGGER.debug("  + Descriptor.....: "+getWebXml());
			LOGGER.debug("  + Deployable name: "+getDeployableName());
			LOGGER.debug("  + Control phrase.: "+getControlPhrase());
			if(!Boolean.parseBoolean(retrieveParameter(Parameter.EXCLUDE_MIDDLEWARE))) {
				LOGGER.debug("- Middleware libraries:");
				LOGGER.debug("  + "+ldpServerArchive);
			}
			if(Boolean.parseBoolean(retrieveParameter(Parameter.WITH_STUBS))) {
				LOGGER.debug("- Testing stubs:");
				LOGGER.debug("  + "+stubs);
			}
/*
			if(Boolean.parseBoolean(retrieveParameter(Parameter.WITH_EXTENDED_STUBS))) {
				LOGGER.debug("- Extended testing stubs:");
				LOGGER.debug("  + "+extendedStubs);
			}
*/
			if(archives.length>0) {
				LOGGER.debug("- Custom libraries:");
				for(JavaArchive archive:archives) {
					LOGGER.debug("  + "+archive);
				}
			}
		}

		MavenResolverSystem resolver = Maven.
			configureResolver().fromClassloaderResource(getSettings());
		PomEquippedResolveStage mavenResolver= 
			resolver.loadPomFromClassLoaderResource(getPom());

		WebArchive war=
			ShrinkWrap.
				create(WebArchive.class, getDeployableName()).
				addAsLibraries(
					mavenResolver.
						importRuntimeDependencies().asFile()).
				addAsLibraries(archives).
				addAsWebResource(
					new StringAsset(getControlPhrase()),"index.html");

		updatePredefinedBundles(war);
		updateWebInf(war);

		if(LOGGER.isTraceEnabled()) {
			LOGGER.debug(String.format("Testing web application archive: \n%s",war.toString(true)));
		}

		return war;
	}

	private void updatePredefinedBundles(WebArchive war) {
		if(!Boolean.parseBoolean(retrieveParameter(Parameter.EXCLUDE_MIDDLEWARE))) {
			war.addAsLibraries(ldpServerArchive);
		}
		if(Boolean.parseBoolean(retrieveParameter(Parameter.WITH_STUBS))) {
			war.addAsLibraries(stubs);
		}
/*
		if(Boolean.parseBoolean(retrieveParameter(Parameter.WITH_EXTENDED_STUBS))) {
			war.addAsLibraries(extendedStubs);
		}
*/
	}

	private void updateWebInf(WebArchive war) {
		File file = new File(getWebXml());
		if(file.canRead() && file.isFile()) {
			war.addAsWebInfResource(file);
		} else {
			war.addAsWebInfResource(getWebXml());
		}
	}

}
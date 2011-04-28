/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.internal;

import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.Messages;
import org.jboss.ide.eclipse.as.core.server.IJBossServerConstants;
import org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime;
import org.jboss.ide.eclipse.as.core.util.IConstants;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;

public class LocalJBossServerRuntime extends AbstractLocalJBossServerRuntime implements IJBossServerRuntime {
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		setAttribute(IJBossServerRuntime.PROPERTY_CONFIGURATION_NAME, IJBossServerConstants.DEFAULT_CONFIGURATION);
	}

	protected String getNextRuntimeName() {
		if( isEAP(getRuntime())) {
			String version = "5.x"; //$NON-NLS-1$
			String base = Messages.jboss + " EAP " + version + " " + Messages.runtime; //$NON-NLS-1$ //$NON-NLS-2$
			return getNextRuntimeName(base);
		}
		return super.getNextRuntimeName();
	}
	
	
	public static boolean isEAP(IRuntime rt) {
		return rt.getRuntimeType().getId().startsWith("org.jboss.ide.eclipse.as.runtime.eap."); //$NON-NLS-1$
	}
	
	public IStatus validate() {
		IStatus s = super.validate();
		if( !s.isOK()) return s;
		
		if( getJBossConfiguration().equals("")) //$NON-NLS-1$
			return new Status(IStatus.ERROR, JBossServerCorePlugin.PLUGIN_ID, 0, 
					NLS.bind(Messages.ServerRuntimeConfigNotFound, getRuntime().getName()), null);
		
		return Status.OK_STATUS;
	}
		
	public String getJBossConfiguration() {
		return getAttribute(PROPERTY_CONFIGURATION_NAME, (String)""); //$NON-NLS-1$
	}
	
	public void setJBossConfiguration(String config) {
		setAttribute(IJBossServerRuntime.PROPERTY_CONFIGURATION_NAME, config);
	}

	public String getDefaultRunArgs() {
		return IConstants.STARTUP_ARG_CONFIG_LONG + "=" + getJBossConfiguration() + IConstants.SPACE;  //$NON-NLS-1$
	}

	public String getDefaultRunVMArgs() {
		IConstants c = new IConstants(){};
		String name = getRuntime().getName();
		String ret = c.QUOTE + c.SYSPROP + c.PROGRAM_NAME_ARG + c.EQ +  
			"JBossTools: " + name + c.QUOTE + c.SPACE; //$NON-NLS-1$
		if( Platform.getOS().equals(Platform.OS_MACOSX))
			ret += c.SERVER_ARG + c.SPACE;
		IRuntimeType type = getRuntime().getRuntimeType();
		if (type != null && 
				(IJBossToolingConstants.AS_50.equals(type.getId()) ||
				 IJBossToolingConstants.AS_51.equals(type.getId()) ||
				 IJBossToolingConstants.AS_60.equals(type.getId()) ||
				 IJBossToolingConstants.EAP_50.equals(type.getId())) ) {
			ret += c.DEFAULT_MEM_ARGS_AS50;
		} else {
			ret += c.DEFAULT_MEM_ARGS;
		}
		if( Platform.getOS().equals(Platform.OS_LINUX))
			ret += c.SYSPROP + c.JAVA_PREFER_IP4_ARG + c.EQ + true + c.SPACE; 
		ret += c.SYSPROP + c.SUN_CLIENT_GC_ARG + c.EQ + 3600000 + c.SPACE;
		ret += c.SYSPROP + c.SUN_SERVER_GC_ARG + c.EQ + 3600000 + c.SPACE;
		ret += c.QUOTE + c.SYSPROP + c.ENDORSED_DIRS + c.EQ + 
			(getRuntime().getLocation().append(c.LIB).append(c.ENDORSED)) + c.QUOTE + c.SPACE;
		if( getRuntime().getLocation().append(c.BIN).append(c.NATIVE).toFile().exists() ) 
			ret += c.SYSPROP + c.JAVA_LIB_PATH + c.EQ + c.QUOTE + 
				getRuntime().getLocation().append(c.BIN).append(c.NATIVE) + c.QUOTE + c.SPACE;
		
		return ret;
	}
	
	public HashMap<String, String> getDefaultRunEnvVars(){
		HashMap<String, String> envVars = new HashMap<String, String>(1);
		envVars.put("Path", IConstants.NATIVE); //$NON-NLS-1$
		return envVars;
	}

	public String getConfigLocation() {
		return getAttribute(PROPERTY_CONFIG_LOCATION, IConstants.SERVER);
	}

	public void setConfigLocation(String configLocation) {
		setAttribute(PROPERTY_CONFIG_LOCATION, configLocation);
	}

	public IPath getConfigurationFullPath() {
		return getConfigLocationFullPath().append(getJBossConfiguration());
	}

	public IPath getConfigLocationFullPath() {
		String cl = getConfigLocation();
		if( new Path(cl).isAbsolute())
			return new Path(cl);
		return getRuntime().getLocation().append(cl);
	}
}

/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.internal.v7;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;

public class JBoss7Server extends JBossServer implements IJBoss7Deployment {
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		setAttribute(DEPLOY_DIRECTORY_TYPE, DEPLOY_CUSTOM);
		setAttribute(IJBossToolingConstants.WEB_PORT_DETECT, false);
		setAttribute(IJBossToolingConstants.WEB_PORT, IJBossToolingConstants.JBOSS_WEB_DEFAULT_PORT);
	}
	public boolean hasJMXProvider() {
		return false;
	}
	public String getDeployFolder(String type) {
		if( type.equals(DEPLOY_SERVER) ) {
			// TODO make sure this is correct?! Upstream APIs have this wrong
		}
		return getDeployFolder(this, type);
	}
	
	
	public static boolean supportsJBoss7MarkerDeployment(IServer server) {
		if( server.loadAdapter(IJBoss7Deployment.class, new NullProgressMonitor()) != null )
			return true;
		return false;
	}
}

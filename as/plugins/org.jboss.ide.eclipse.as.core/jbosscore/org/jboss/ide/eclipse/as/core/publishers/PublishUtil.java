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
package org.jboss.ide.eclipse.as.core.publishers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.jboss.ide.eclipse.as.core.server.IJBossServerConstants;

public class PublishUtil {
	public static int countChanges(IModuleResourceDelta[] deltas) {
		IModuleResource res;
		int count = 0;
		if( deltas == null ) return 0;
		for( int i = 0; i < deltas.length; i++ ) {
			res = deltas[i].getModuleResource();
			if( res != null && res instanceof IModuleFile)
				count++;
			count += countChanges(deltas[i].getAffectedChildren());
		}
		return count;
	}


	public static int countMembers(IModule module) {
		try {
			ModuleDelegate delegate = (ModuleDelegate)module.loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
			return delegate == null ? 0 : countMembers(delegate.members());
		} catch( CoreException ce ) {}
		return 0;
	}
	public static int countMembers(IModuleResource[] resources) {
		int count = 0;
		if( resources == null ) return 0;
		for( int i = 0; i < resources.length; i++ ) {
			if( resources[i] instanceof IModuleFile ) {
				count++;
			} else if( resources[i] instanceof IModuleFolder ) {
				count += countMembers(((IModuleFolder)resources[i]).members());
			}
		}
		return count;
	}

	public static IPath getDeployPath(IModule[] moduleTree, String deployFolder) {
		// TODO This should probably change once 241466 is solved
		IPath root = new Path( deployFolder );
		String type, name;
		for( int i = 0; i < moduleTree.length; i++ ) {
			type = moduleTree[i].getModuleType().getId();
			name = moduleTree[i].getName();
			if( new Path(name).segmentCount() > 1 )
				// we strongly suspect this is a binary object and not a project
				return root.append(new Path(name).lastSegment());
			if( IJBossServerConstants.FACET_EAR.equals(type)) 
				root = root.append(name + IJBossServerConstants.EXT_EAR);
			else if( IJBossServerConstants.FACET_WEB.equals(type)) 
				root = root.append(name + IJBossServerConstants.EXT_WAR);
			else if( IJBossServerConstants.FACET_UTILITY.equals(type) && i >= 1 
					&& IJBossServerConstants.FACET_WEB.equals(moduleTree[i-1].getModuleType().getId())) 
				root = root.append(IJBossServerConstants.WEB_INF)
						.append(IJBossServerConstants.LIB)
						.append(name + IJBossServerConstants.EXT_JAR);			
			else if( IJBossServerConstants.FACET_CONNECTOR.equals(type)) {
				root = root.append(name + IJBossServerConstants.EXT_RAR);
			} else if( IJBossServerConstants.FACET_ESB.equals(type)){
				root = root.append(name + IJBossServerConstants.EXT_ESB);
			}else
				root = root.append(name + IJBossServerConstants.EXT_JAR);
		}
		return root;
	}
	
	// TODO This can also change to find the isBinaryModule method 
	public static boolean isBinaryObject(IModule[] moduleTree) {
		String name;
		for( int i = 0; i < moduleTree.length; i++ ) {
			name = moduleTree[i].getName();
			if( new Path(name).segmentCount() > 1 )
				// we strongly suspect this is a binary object and not a project
				return true;
		}
		return false;
	}
	
	public static IModuleResource[] getResources(IModule module) throws CoreException {
		ModuleDelegate md = (ModuleDelegate)module.loadAdapter(ModuleDelegate.class, new NullProgressMonitor());
		IModuleResource[] members = md.members();
		return members;
	}
	public static IModuleResource[] getResources(IModule[] tree) throws CoreException {
		return getResources(tree[tree.length-1]);
	}
	
	public static java.io.File getFile(IModuleFile mf) {
		return (IFile)mf.getAdapter(IFile.class) != null ? 
					((IFile)mf.getAdapter(IFile.class)).getLocation().toFile() :
						(java.io.File)mf.getAdapter(java.io.File.class);
	}
}

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
package org.jboss.ide.eclipse.as.core.util;

import java.util.List;
import java.util.Properties;

import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.ExtensionManager;
import org.jboss.ide.eclipse.as.core.server.INeedCredentials;
import org.jboss.ide.eclipse.as.core.server.IProvideCredentials;
import org.jboss.ide.eclipse.as.core.server.IServerProvider;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller;
import org.jboss.ide.eclipse.as.core.server.internal.IPollResultListener;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.core.server.internal.PollThread;
import org.jboss.ide.eclipse.as.core.server.internal.ServerAttributeHelper;
import org.jboss.ide.eclipse.as.core.server.internal.ServerStatePollerType;
import org.jboss.ide.eclipse.as.core.server.v7.management.AS7ManagementDetails;

/**
 * @author Rob Stryker
 * @author André Dietisheim
 */
public class PollThreadUtils {

	/**
	 * Returns the poller id of the poller that is currently used in the given server and 
	 * that is waiting for the given state.
	 * 
	 * @param expectedState
	 * @param server
	 * @return
	 */
	public static String getPollerId(boolean expectedState, IServer server) {
		JBossServer s = ServerConverter.getJBossServer(server);
		ServerAttributeHelper helper = s.getAttributeHelper();
		String key = expectedState == IServerStatePoller.SERVER_UP ?
				IJBossToolingConstants.STARTUP_POLLER_KEY
				: IJBossToolingConstants.SHUTDOWN_POLLER_KEY;
		String defaultPoller = expectedState == IServerStatePoller.SERVER_UP ?
				IJBossToolingConstants.DEFAULT_STARTUP_POLLER
				: IJBossToolingConstants.DEFAULT_SHUTDOWN_POLLER;
		String pollerId = helper.getAttribute(key, defaultPoller);
		return pollerId;
	}

	/**
	 * Returns the poller that is current used in the given server for the expected state.
	 *  
	 * @param expectedState the state that the poller waits for
	 * @param server the server that is using the poller
	 * @return
	 */
	public static IServerStatePoller getPoller(boolean expectedState, IServer server) {
		return getPoller(getPollerId(expectedState, server));
	}

	/**
	 * Returns the poller for the given poller id. 
	 * 
	 * @param pollerId the id of the poller to use
	 * @return the poller for the given id
	 */
	public static IServerStatePoller getPoller(String pollerId) {
		ServerStatePollerType type = ExtensionManager.getDefault().getPollerType(pollerId);
		if (type != null) {
			IServerStatePoller tempPoller = type.createPoller();
			tempPoller.setPollerType(type);
			return tempPoller;
		}
		return null;
	}

	/**
	 * Stops the given poll thread.
	 * 
	 * @param pollThread the poll thread to stop
	 */
	public static void stopPolling(PollThread pollThread) {
		cancelPolling(null, pollThread);
	}

	/**
	 * Cancels the given poll thread with the given message (that tells about the reason to cancel polling).
	 * 
	 * @param message the reason to cancel the poll thread
	 * @param pollThread the poll thread to cancel
	 */
	public static void cancelPolling(String message, PollThread pollThread) {
		if (pollThread != null) {
			if (message != null) {
				pollThread.cancel(message);
			} else {
				pollThread.cancel();
			}
		}
	}


	/**
	 * Stops the given poll thread and creates a new poll thread for the given
	 * expected state, poller, result listener and server.
	 * 
	 * @param expectedState the state to wait for 
	 * @param poller the poller to use to wait for the expected state
	 * @param pollThread the poll thread to stop
	 * @param listener the listener to inform about the polling result 
	 * @return the new poll thread
	 */
	public static PollThread pollServer(boolean expectedState, IServerStatePoller poller, PollThread currentPollThread,
			IPollResultListener listener, IServer server) {
		stopPolling(currentPollThread);
		PollThread newPollThread = new PollThread(expectedState, poller, listener, server);
		newPollThread.start();
		return newPollThread;
	}
	
	/**
	 * The credential provider is alerted that credentials are needed. 
	 * The response may come at any time. 
	 * 
	 * @param requester
	 * @param requiredProps
	 */
	public static void requestCredentialsAsynch(INeedCredentials requester, List<String> requiredProps) {
		IProvideCredentials provider = ExtensionManager.getDefault()
				.getFirstCredentialProvider(requester, requiredProps);
		provider.handle(requester, requiredProps);
	}
	
	/**
	 * The credential provider is alerted that credentials are needed. 
	 * The thread is then delayed until some result is provided. 
	 * 
	 * @param requester
	 * @param requiredProps
	 * @return
	 */

	public static Properties requestCredentialsSynchronous(final IServerProvider requester, List<String> requiredProps) {
		IProvideCredentials provider = ExtensionManager.getDefault()
				.getFirstCredentialProvider(requester, requiredProps);
		final Properties[] returnedProps = new Properties[1];
		final Boolean[] gotProps = new Boolean[1];
		returnedProps[0] = null;
		gotProps[0] = false;
		
		/*
		 * This dummy requirer will set variables when the credentials finally arrive. 
		 * Then this synchronous method can finish. 
		 */
		
		INeedCredentials dummyRequirer = new INeedCredentials() {
			public IServer getServer() {
				return requester.getServer();
			}
			public List<String> getRequiredProperties() {
				// ignore
				return null;
			}
			public void provideCredentials(Properties credentials) {
				returnedProps[0] = credentials;
				// necessary to have a gotProps since a cancelation on the UI may 
				// set null as the returned properties
				gotProps[0] = true;
			}
		};
		
		provider.handle(dummyRequirer, requiredProps);

		while( !gotProps[0]) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException ie) {
				// ignore
			}
		}
		return returnedProps[0];
	}
}

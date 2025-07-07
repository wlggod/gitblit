/*
 * Copyright 2015 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.transport.ssh;

import java.util.Locale;

import org.apache.sshd.server.auth.gss.GSSAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.manager.IAuthenticationManager;
import com.gitblit.models.UserModel;

public class SshKrbAuthenticator extends GSSAuthenticator {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final IAuthenticationManager authManager;
	protected final boolean stripDomain;


	public SshKrbAuthenticator(IStoredSettings settings, IAuthenticationManager authManager) {
		this.authManager = authManager;

		String keytabString = settings.getString(Keys.git.sshKrb5Keytab, "");
		if(! keytabString.isEmpty()) {
			setKeytabFile(keytabString);
		}

		String servicePrincipalName = settings.getString(Keys.git.sshKrb5ServicePrincipalName, "");
		if(! servicePrincipalName.isEmpty()) {
			setServicePrincipalName(servicePrincipalName);
		}

		this.stripDomain = settings.getBoolean(Keys.git.sshKrb5StripDomain, false);
	}

	@Override
	public boolean validateIdentity(ServerSession session, String identity) {
		log.info("identify with kerberos {}", identity);
		SshDaemonClient client = session.getAttribute(SshDaemonClient.KEY);

		String username = identity.toLowerCase(Locale.US);
		if (stripDomain) {
			int p = username.indexOf('@');
			if (p > 0) {
				username = username.substring(0, p);
			}
		}
		UserModel user = authManager.authenticate(username);
		if (user != null) {
// TODO: Check if the user was set in the client and if it is the same as this user. Do not allow changing the user during the SSH auth process.
			client.setUser(user);
			return true;
		}
		log.warn("could not authenticate {} for SSH", username);
		return false;
	}
}

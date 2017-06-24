/*******************************************************************************
 * Copyright 2016 Universidad Politécnica de Madrid UPM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.security.authorisator.access_checkers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.owl.TypeExpression;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.ontology.profile.User;
import org.universAAL.ontology.security.AccessRight;
import org.universAAL.ontology.security.AccessType;
import org.universAAL.ontology.security.Role;
import org.universAAL.ontology.security.SecuritySubprofile;
import org.universAAL.security.authorisator.CHeQuerrier;
import org.universAAL.security.authorisator.ProjectActivator;
import org.universAAL.security.authorisator.interfaces.AccessChecker;

/**
 * @author amedrano
 *
 */
public class CheckUserRoles implements AccessChecker {

	private CHeQuerrier querier;

	protected ModuleContext mc;

	private static final String AUX_BAG_OBJECT = ProjectActivator.NAMESPACE + "auxilaryBagObject";
	private static final String AUX_BAG_PROP = ProjectActivator.NAMESPACE + "auxilaryBagProperty";

	/** {@inheritDoc} */
	public Set<AccessType> checkAccess(ModuleContext mc, User usr, Resource asset) {

		init(mc);

		// get the SecuritySubProfile for the user

		LogUtils.logDebug(mc, getClass(), "accessCheck", "Querying for the SecuritySubProfile.");
		SecuritySubprofile ssp = getSecuritySubProfile(mc, usr);
		LogUtils.logDebug(mc, getClass(), "accessCheck", "Got the SecuritySubProfile.");
		if (ssp == null) {
			LogUtils.logInfo(mc, getClass(), "checkAccess", "No SecuritySubprofile found for user:" + usr.getURI());
			return Collections.EMPTY_SET;
		}

		// aggregate all the AccessRights
		LogUtils.logDebug(mc, getClass(), "accessCheck", "Aggregating AccessRights.");
		List<Role> roles = ssp.getRoles();
		Set<AccessRight> finalAccessRights = new HashSet<AccessRight>();
		for (Role role : roles) {
			finalAccessRights.addAll(role.getAllAccessRights());
		}
		LogUtils.logDebug(mc, getClass(), "accessCheck", finalAccessRights.size() + " AccessRights aggregated.");

		// match the asset with all AccessRights

		return matchAccessRightsWAsset(finalAccessRights, asset);
	}

	protected void init(ModuleContext mc) {
		if (querier == null) {
			querier = new CHeQuerrier(mc);
		}
		if (this.mc == null) {
			this.mc = mc;
		}
	}

	protected HashSet<AccessType> matchAccessRightsWAsset(Set<AccessRight> finalAccessRights, Resource asset) {
		HashSet<AccessType> res = new HashSet<AccessType>();
		for (AccessRight ar : finalAccessRights) {
			Object te = ar.getProperty(AccessRight.PROP_ACCESS_TO);
			if (te instanceof TypeExpression && ((TypeExpression) te).hasMember(asset)) {
				LogUtils.logDebug(mc, getClass(), "matchAccessRightsWAsset", asset.getURI() + " Matched!");
				res.addAll(AssetDefaultAccessChecker.resolveFromValue(ar));
			}
		}
		LogUtils.logDebug(mc, getClass(), "matchAccessRightsWAsset", res.size() + " Matching AccessRights.");
		return res;
	}

	protected SecuritySubprofile getSecuritySubProfile(ModuleContext mc, User usr) {
		Object o = querier.query(CHeQuerrier.getQuery(CHeQuerrier.getResource("getSecuritySubProfileForUser.sparql"),
				new String[] { AUX_BAG_OBJECT, AUX_BAG_PROP, usr.getURI() }));
		SecuritySubprofile ssp;
		if (o instanceof Resource) {
			// "o" should be the resource AUX_BAG_OBJECT
			o = ((Resource) o).getProperty(AUX_BAG_PROP);
			if (o instanceof List) {
				LogUtils.logWarn(mc, getClass(), "checkAccess",
						"WTF mode: More than one SecuritySubprofile found for the given user: " + usr.getURI());
				o = ((List) o).get(0);
				ssp = (SecuritySubprofile) querier.getFullResourceGraph(((Resource) o).getURI());
			} else if (o != null) {
				// not a list and not null => must be a Resource representing
				// the SSP
				ssp = (SecuritySubprofile) querier.getFullResourceGraph(((Resource) o).getURI());
			} else {
				LogUtils.logError(mc, getClass(), "checkAccess",
						"No SecuritySubprofile found for the given user: " + usr.getURI());
				ssp = null;
			}
			return ssp;
		} else {
			LogUtils.logError(mc, getClass(), "getAllObjectsOfType",
					"Wrong querry response, should get a Resource and we didn't");
			return null;
		}
	}

}

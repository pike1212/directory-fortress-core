/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core.impl;

import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.DelReviewMgr;
import org.apache.directory.fortress.core.model.AdminRole;
import org.apache.directory.fortress.core.model.OrgUnit;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.UserAdminRole;
import org.apache.directory.fortress.core.util.VUtil;

import java.util.List;

/**
 * This class implements the ARBAC02 DelReviewMgr interface for performing policy interrogation of provisioned Fortress ARBAC entities
 * that reside in LDAP directory.
 * These APIs map directly to similar named APIs specified by ARBAC02 functions.  The ARBAC Functional specification describes delegated administrative
 * operations for the creation and maintenance of ARBAC element sets and relations.  Delegated administrative review functions for performing administrative queries
 * and system functions for creating and managing ARBAC attributes on user sessions and making delegated administrative access control decisions.
 * <h3>Administrative Role Based Access Control (ARBAC)</h3>
 * <img src="../doc-files/ARbac.png">
 * <p/>
 * Fortress fully supports the Oh/Sandhu/Zhang ARBAC02 model for delegated administration.  ARBAC provides large enterprises the capability to delegate administrative authority to users that reside outside of the security admin group.
 * Decentralizing administration helps because it provides security provisioning capability to work groups without sacrificing regulations for accountability or traceability.
 * <p/>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * <p/>
 * This class is NOT thread safe if parent instance variables ({@link #contextId} or {@link #adminSess}) are set.
 */
public class DelReviewMgrImpl extends Manageable implements DelReviewMgr
{
    private static final String CLS_NM = DelReviewMgrImpl.class.getName();
    private static final UserP userP = new UserP();
    private static final OrgUnitP ouP = new OrgUnitP();
    private static final AdminRoleP admRP = new AdminRoleP();

    /**
     * Method reads Admin Role entity from the admin role container in directory.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link org.apache.directory.fortress.core.model.AdminRole#name} - contains the name of the AdminRole being targeted for read</li>
     * </ul>
     *
     * @param role contains role name to be read.
     * @return AdminRole entity that corresponds with role name.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          will be thrown if role not found or system error occurs.
     */
    @Override
    public AdminRole readRole(AdminRole role)
        throws SecurityException
    {
        String methodName = "readRole";
        assertContext(CLS_NM, methodName, role, GlobalErrIds.ARLE_NULL);
        checkAccess(CLS_NM, methodName);
        return admRP.read(role);
    }


    /**
     * Method will return a list of type Admin Role.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link AdminRole#name} - contains all or some chars in the name of AdminRole(s) targeted for search</li>
     * </ul>
     *
     * @param searchVal contains the all or some of the chars corresponding to admin role entities stored in directory.
     * @return List of type AdminRole containing role entities that match the search criteria.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of system error.
     */
    @Override
    public List<AdminRole> findRoles(String searchVal)
        throws SecurityException
    {
        String methodName = "findRoles";
        VUtil.assertNotNull(searchVal, GlobalErrIds.ARLE_NM_NULL, CLS_NM + "." + methodName);
        checkAccess(CLS_NM, methodName);
        AdminRole adminRole = new AdminRole(searchVal);
        adminRole.setContextId(this.contextId);
        return admRP.search(adminRole);
    }

    /**
     * This function returns the set of admin roles assigned to a given user. The function is valid if and
     * only if the user is a member of the USERS data set.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link org.apache.directory.fortress.core.model.User#userId} - contains the userId associated with the User object targeted for search.</li>
     * </ul>
     *
     * @param user contains userId matching user entity stored in the directory.
     * @return List of type UserAdminRole containing the user admin role data.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          If user not found or system error occurs.
     */
    @Override
    public List<UserAdminRole> assignedRoles(User user)
        throws SecurityException
    {
        String methodName = "assignedRoles";
        assertContext(CLS_NM, methodName, user, GlobalErrIds.USER_NULL);
        checkAccess(CLS_NM, methodName);
        User ue = userP.read(user, true);
        return ue.getAdminRoles();
    }


    /**
     * This method returns the data set of all users who are assigned the given admin role.  This searches the User data set for
     * AdminRole relationship.  This method does NOT search for hierarchical Admin Roles relationships.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link AdminRole#name} - contains the name of AdminRole targeted for search</li>
     * </ul>
     *
     * @param role contains the role name used to search the User data set.
     * @return List of type User containing the users assigned data.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          If system error occurs.
     */
    @Override
    public List<User> assignedUsers(AdminRole role)
        throws SecurityException
    {
        String methodName = "assignedUsers";
        assertContext(CLS_NM, methodName, role, GlobalErrIds.ARLE_NULL);
        checkAccess(CLS_NM, methodName);
        return userP.getAssignedUsers(role);
    }


    /**
     * Commands reads existing OrgUnit entity from OrgUnit dataset.  The OrgUnit can be either User or Perm and is
     * set by setting type attribute.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link org.apache.directory.fortress.core.model.OrgUnit#name} - contains the name associated with the OrgUnit object targeted for search.</li>
     * <li>{@link org.apache.directory.fortress.core.model.OrgUnit#type} - contains the type of OU:  {@link org.apache.directory.fortress.core.model.OrgUnit.Type#USER} or {@link org.apache.directory.fortress.core.model.OrgUnit.Type#PERM}</li>
     * </ul>
     *
     * @param entity contains OrgUnit name and type.
     * @return OrgUnit entity that corresponds with ou name and type.
     * @throws org.apache.directory.fortress.core.SecurityException
     *          in the event of data validation or system error.
     */
    @Override
    public OrgUnit read(OrgUnit entity)
        throws SecurityException
    {
        String methodName = "readOU";
        assertContext(CLS_NM, methodName, entity, GlobalErrIds.ORG_NULL);
        checkAccess(CLS_NM, methodName);
        return ouP.read(entity);
    }


    /**
     * Commands searches existing OrgUnit entities from OrgUnit dataset.  The OrgUnit can be either User or Perm and is
     * set by setting type parameter on API.
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link OrgUnit#type} - contains the type of OU:  {@link OrgUnit.Type#USER} or {@link OrgUnit.Type#PERM}</li>
     * <li>searchVal - contains some or all of the chars associated with the OrgUnit objects targeted for search.</li>
     * </ul>
     *
     * @param type      either PERM or USER
     * @param searchVal contains the leading chars that map to {@link OrgUnit#name} on existing OrgUnit(s) targeted for search.
     * @return List of type OrgUnit containing the OrgUnit data.
     * @throws org.apache.directory.fortress.core.SecurityException
     *
     */
    @Override
    public List<OrgUnit> search(OrgUnit.Type type, String searchVal)
        throws SecurityException
    {
        String methodName = "searchOU";
        //VUtil.assertNotNullOrEmpty(searchVal, GlobalErrIds.ORG_NULL, CLS_NM + "." + methodName);
        VUtil.assertNotNull(type, GlobalErrIds.ORG_TYPE_NULL, CLS_NM + "." + methodName);
        checkAccess(CLS_NM, methodName);
        OrgUnit orgUnit = new OrgUnit(searchVal);
        orgUnit.setType(type);
        orgUnit.setContextId(this.contextId);
        return ouP.search(orgUnit);
    }
}


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

import org.apache.commons.collections.CollectionUtils;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.GroupMgr;
import org.apache.directory.fortress.core.ReviewMgr;
import org.apache.directory.fortress.core.ReviewMgrFactory;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.model.Group;
import org.apache.directory.fortress.core.model.User;

import java.util.ArrayList;
import java.util.List;


/**
 * This Manager impl supplies CRUD methods used to manage groups stored within the ldap directory.
 * LDAP group nodes are used for utility and security functions within various systems and apps.
 * <p/>
 * This class is thread safe.
 * <p/>

 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GroupMgrImpl extends Manageable implements GroupMgr
{
    private static final String CLS_NM = GroupMgrImpl.class.getName();
    private static final GroupP GROUP_P = new GroupP();

    /**
     * Create a new group node.  Must have a name and at least one member.
     *
     * @param group contains {@link org.apache.directory.fortress.core.model.Group}.
     * @return {@link org.apache.directory.fortress.core.model.Group} containing entity just added.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    @Override
    public Group add( Group group ) throws org.apache.directory.fortress.core.SecurityException
    {
        String methodName = "add";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        if(!group.isMemberDn())
        {
            loadUserDns( group );
        }

        return GROUP_P.add( group );
    }

    /**
     * Modify existing group node.  The name is required.  Does not update members or properties.
     * Use {@link GroupMgr#add( Group group, String key, String value )}, {@link GroupMgr#delete( Group group, String key, String value )},
     * {@link GroupMgr#assign( Group group, String member) }, or {@link GroupMgr#deassign( Group group, String member) } for multi-occurring attributes.
     *
     * @param group contains {@link Group}.
     * @return {@link Group} containing entity just modified.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    @Override
    public Group update( Group group ) throws SecurityException
    {
        String methodName = "update";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.update( group );
    }

    /**
     * Delete existing group node.  The name is required.
     *
     * @param group contains {@link Group}.
     * @return {@link Group} containing entity just removed.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    @Override
    public Group delete( Group group ) throws SecurityException
    {
        String methodName = "delete";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.delete( group );
    }

    /**
     * Add a property to an existing group node.  The name is required.
     *
     * @param group contains {@link Group}.
     * @param key contains the property key.
     * @param value contains contains the property value.
     * @return {@link Group} containing entity just modified.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    public Group add( Group group, String key, String value ) throws SecurityException
    {
        String methodName = "addProperty";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.add( group, key, value );
    }

    /**
     * Delete existing group node.  The name is required.
     *
     * @param group contains {@link Group}.
     * @param key contains the property key.
     * @param value contains contains the property value.
     * @return {@link Group} containing entity just modified.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    public Group delete( Group group, String key, String value ) throws SecurityException
    {
        String methodName = "deleteProperty";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.delete( group, key, value );
    }

    /**
     * Read an existing group node.  The name is required.
     *
     * @param group contains {@link Group} with name field set with an existing group name.
     * @return {@link Group} containing entity found.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    @Override
    public Group read( Group group ) throws SecurityException
    {
        String methodName = "read";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.read( group );
    }

    /**
     * Search using a full or partial group node.  The name is required.
     *
     * @param group contains {@link Group}.
     * @return List of type {@link Group} containing entities found.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    @Override
    public List<Group> find( Group group ) throws SecurityException
    {
        String methodName = "find";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        
        return GROUP_P.search( group );
    }

    /**
     * Search for groups by userId.  Member (maps to userId) and is required.
     *
     * @param user contains userId that maps to Group member attribute.
     * @return {@link Group} containing entity just added.
     * @throws org.apache.directory.fortress.core.SecurityException in the event system error.
     */
    public List<Group> find( User user ) throws SecurityException
    {
        String methodName = "findWithUsers";
        assertContext(CLS_NM, methodName, user, GlobalErrIds.USER_NULL);
        checkAccess(CLS_NM, methodName);
        loadUserDn( user );
        
        return GROUP_P.search( user );
    }

    /**
     * Assign a user to an existing group node.  The name is required and userDn are required.
     *
     * @param group contains {@link Group}.
     * @param member is the relative distinguished name (rdn) of an existing user in ldap.
     * @return {@link Group} containing entity to assign.
     * @throws org.apache.directory.fortress.core.SecurityException in the event entry already present or other system error.
     */
    @Override
    public Group assign( Group group, String member ) throws SecurityException
    {
        String methodName = "assign";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        ReviewMgr reviewMgr = ReviewMgrFactory.createInstance();
        User user = reviewMgr.readUser( new User( member ) );
        
        return GROUP_P.assign( group, user.getDn() );
    }

    /**
     * Deassign a user from an existing group node.  The name is required and userDn are required.
     *
     * @param group contains {@link Group}.
     * @param member is the relative distinguished name (rdn) of an existing user in ldap.
     * @return {@link Group} containing entity to deassign
     * @throws org.apache.directory.fortress.core.SecurityException in the event entry already present or other system error.
     */
    @Override
    public Group deassign( Group group, String member ) throws SecurityException
    {
        String methodName = "deassign";
        assertContext(CLS_NM, methodName, group, GlobalErrIds.GROUP_NULL);
        checkAccess(CLS_NM, methodName);
        ReviewMgr reviewMgr = ReviewMgrFactory.createInstance();
        User user = reviewMgr.readUser( new User( member ) );
        
        return GROUP_P.deassign( group, user.getDn() );
    }

    private void loadUserDns( Group group ) throws SecurityException
    {
        if( CollectionUtils.isNotEmpty( group.getMembers() ))
        {
            ReviewMgr reviewMgr = ReviewMgrFactory.createInstance();
            List<String> userDns = new ArrayList<String>();
            
            for( String member : group.getMembers() )
            {
                User user = reviewMgr.readUser( new User( member ) );
                userDns.add( user.getDn() );
            }
            
            group.setMembers( userDns );
        }
    }

    private void loadUserDn( User inUser ) throws SecurityException
    {
        ReviewMgr reviewMgr = ReviewMgrFactory.createInstance();
        User outUser = reviewMgr.readUser( inUser );
        inUser.setDn( outUser.getDn() );
    }
}
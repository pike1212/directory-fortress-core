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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.PasswordException;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.ValidationException;
import org.apache.directory.fortress.core.model.AdminRole;
import org.apache.directory.fortress.core.model.Administrator;
import org.apache.directory.fortress.core.model.ConstraintUtil;
import org.apache.directory.fortress.core.model.ObjectFactory;
import org.apache.directory.fortress.core.model.OrgUnit;
import org.apache.directory.fortress.core.model.PwPolicy;
import org.apache.directory.fortress.core.model.Role;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.UserAdminRole;
import org.apache.directory.fortress.core.model.UserRole;
import org.apache.directory.fortress.core.util.VUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Process module for the User entity.  This class performs data validations and error mapping.  It is typically called
 * by internal Fortress manager classes ({@link AdminMgrImpl}, {@link AccessMgrImpl},
 * {@link ReviewMgrImpl}, ...) and not intended for external non-Fortress clients.  This class will accept,
 * {@link org.apache.directory.fortress.core.model.User}, validate its contents and forward on to it's corresponding DAO class {@link org.apache.directory.fortress.core.impl.UserDAO}.
 * <p>
 * Class will throw {@link SecurityException} to caller in the event of security policy, data constraint violation or system
 * error internal to DAO object. This class will forward DAO exceptions ({@link org.apache.directory.fortress.core.FinderException},
 * {@link org.apache.directory.fortress.core.CreateException},{@link org.apache.directory.fortress.core.UpdateException},{@link org.apache.directory.fortress.core.RemoveException}),
 *  or {@link org.apache.directory.fortress.core.ValidationException} as {@link SecurityException}s with appropriate
 * error id from {@link org.apache.directory.fortress.core.GlobalErrIds}.
 * <p>
 * This class is thread safe.
 * </p>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
final class UserP
{
    //private static final boolean IS_SESSION_PROPS_ENABLED = Config.getBoolean( "user.session.props.enabled", false );
    private static final String CLS_NM = UserP.class.getName();
    private static UserDAO uDao = new UserDAO();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private static final PolicyP policyP = new PolicyP();
    private static final AdminRoleP admRoleP = new AdminRoleP();
    private static final OrgUnitP orgUnitP = new OrgUnitP();


    /**
     * Package private constructor.
     */
    UserP()
    {
    }


    /**
     * Takes a User entity that contains full or partial userId OR a full internal userId for search.
     *
     * @param user contains all or partial userId or full internal userId.
     * @return List of type User containing fully populated matching User entities.  If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<User> search( User user ) throws SecurityException
    {
        return uDao.findUsers( user );
    }


    List<User> search( OrgUnit ou, boolean limitSize ) throws SecurityException
    {
        return uDao.findUsers( ou, limitSize );
    }


    /**
     * Search according to full or partial search string that maps to Fortress userid.
     * This search is used by RealmMgr for Websphere.
     *
     * @param user contains full or partial userId.
     * @param limit     specify the max number of records to return in result set.
     * @return List of type String containing userId of all matching User entities. If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<String> search( User user, int limit ) throws SecurityException
    {
        return uDao.findUsers( user, limit );
    }


    /**
     * Return a list of Users that are authorized the given Role.
     *
     * @param role contains the role name targeted for search.
     * @return List of type User containing fully populated matching User entities. If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<User> getAuthorizedUsers( Role role ) throws SecurityException
    {
        return uDao.getAuthorizedUsers( role );
    }


    /**
     * Return a list of Users that are authorized the given Role.
     *
     * @param roles contains the set of role names targeted for search.
     * @param contextId maps to sub-tree in DIT, for example ou=contextId, dc=jts, dc = com.
     * @return Set of type String containing the userId's for matching User entities. If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    Set<String> getAssignedUsers( Set<String> roles, String contextId ) throws SecurityException
    {
        return uDao.getAssignedUsers( roles, contextId );
    }


    /**
     * Return a list of Users that are authorized the given Role.
     * In RBAC the word "authorized" implies the hierarchical role relations graph is considered in result set.
     * This search is used by RealmMgr for Websphere.
     *
     * @param role
     * @param limit specify the max number of records to return in result set.
     * @return list of type String of userIds. If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<String> getAuthorizedUsers( Role role, int limit ) throws SecurityException
    {
        return uDao.getAuthorizedUsers( role, limit );
    }


    /**
     * Return a list of Users assigned the given RBAC role.
     * "Assigned" implies the hierarchical role relation graph will NOT be considered in result set.
     *
     * @param role contains name of RBAC role used for search.
     * @return List of fully populated User entities matching target search. If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<User> getAssignedUsers( Role role ) throws SecurityException
    {
        return uDao.getAssignedUsers( role );
    }


    /**
     * Return a list of Users assigned the given Administrative role.
     * "Assigned" implies the hierarchical role relation graph will NOT be considered in result set.
     *
     * @param role contains name of Admin role used for search.
     * @return List of fully populated User entities matching target search.  If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<User> getAssignedUsers( AdminRole role ) throws SecurityException
    {
        return uDao.getAssignedUsers( role );
    }


    /**
     * Return the list of User's RBAC roles.
     *
     * @param user contains full userId for target operation.
     * @return List of type String containing RBAC role names.  If no records found this will be empty.
     * @throws SecurityException in the event of DAO search error.
     */
    List<String> getAssignedRoles( User user ) throws SecurityException
    {
        return uDao.getRoles( user );
    }


    /**
     * Return a fully populated User entity for a given userId.  If the User entry is not found a SecurityException
     * will be thrown.
     *
     * @param user  contains full userId value.
     * @param isRoles return user's assigned roles if "true".
     * @return User entity containing all attributes associated with User in directory.
     * @throws SecurityException in the event of User not found or DAO search error.
     */
    User read( User user, boolean isRoles ) throws SecurityException
    {
        return uDao.getUser( user, isRoles );
    }


    /**
     * Adds a new User entity to directory.  The User entity input object will be validated to ensure that:
     * userId is present, orgUnitId is valid, roles (optiona) are valid, reasonability checks on all of the
     * other populated values.
     *
     * @param entity User entity contains data targeted for insertion.
     * @return User entity copy of input + additional attributes (internalId) that were added by op.
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    User add( User entity ) throws SecurityException
    {
        return add( entity, true );
    }


    /**
     * Adds a new User entity to directory.
     * The User entity input object will be validated to ensure that: userId is present, orgUnitId is valid,
     * roles (optiona) are valid, reasonability checks on all of the other populated values.
     *
     * @param entity   User entity contains data targeted for insertion.
     * @param validate if false will skip the validations described above.
     * @return User entity copy of input + additional attributes (internalId)
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    User add( User entity, boolean validate ) throws SecurityException
    {
        if ( validate )
        {
            // Ensure the input data is valid.
            validate( entity, false );
        }

        entity = uDao.create( entity );

        return entity;
    }


    /**
     * Update existing user's attributes with the input entity.  Null or empty attributes will be ignored.
     * This method will ignore userId as input as change userId is not allowed.  If password is changed
     * OpenLDAP password policy will not be evaluated on behalf of the user.
     * Other User entity input data can be changed and will also be validated beforehand to ensure that:
     * orgUnitId is valid, roles (optional) are valid, reasonability checks will be performed on all of the populated fields.
     *
     * @param entity User entity contains data targeted for insertion.
     * @return User entity copy of input
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    User update( User entity ) throws SecurityException
    {
        return update( entity, true );
    }


    /**
     * Update existing user's attributes with the input entity.  Null or empty attributes will be ignored.
     * This method will ignore userId or password as input.  The former is not allowed and latter is performed by other
     * methods in this class.
     * Other User entity input data can be changed and will also be validated beforehand to ensure that:
     * orgUnitId is valid, roles (optional) are valid, reasonability checks will be performed on all of the populated fields.
     *
     * @param entity   User entity contains data targeted for insertion.
     * @param validate if false will skip the validations described above.
     * @return User entity copy of input
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    /**
     * Update existing user's attributes with the input entity.  Null or empty attributes will be ignored.
     * This method will ignore userId or password as input.  The former is not allowed and latter is performed by other
     * methods in this class.
     * Other User entity input data can be changed and will also be validated beforehand to ensure that:
     * orgUnitId is valid, roles (optional) are valid, reasonability checks will be performed on all of the populated fields.
     *
     * @param entity   User entity contains data targeted for insertion.
     * @param validate if false will skip the validations described above.
     * @return User entity copy of input
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    User update( User entity, boolean validate ) throws SecurityException
    {
        if ( validate )
        {
            // Ensure the input data is valid.
            validate( entity, true );
        }
        entity = uDao.update( entity );
        return entity;
    }


    /**
     * Method performs a "soft" delete.  It disables User entity and flags as "deleted".  User must exist in directory
     * prior to making this call.
     *
     * @param user Contains the userId of the user targeted for deletion.
     * @return String contains user DN
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    String softDelete( User user ) throws SecurityException
    {
        // Ensure this user isn't listed in Fortress config as a system user that can't be removed via API.
        // Is there a match between this userId and a Fortress system user?
        User checkUser = read( user, true );
        if ( checkUser.isSystem() != null && checkUser.isSystem() )
        {
            String warning = "softDelete userId [" + user.getUserId()
                + "] can't be removed due to policy violation, rc=" + GlobalErrIds.USER_PLCY_VIOLATION;
            throw new SecurityException( GlobalErrIds.USER_PLCY_VIOLATION, warning );
        }
        user.setDescription( "DELETED" );
        User outUser = uDao.update( user );
        return outUser.getDn();
    }


    /**
     * This method performs a "hard" delete.  It completely removes all data associated with this user from the directory.
     * User entity must exist in directory prior to making this call else exception will be thrown.
     *
     * @param user Contains the userid of the user targeted for deletion.
     * @return String contains user DN
     * @throws SecurityException in the event of data validation or DAO system error.
     */
    String delete( User user ) throws SecurityException
    {
        // Ensure this user isn't listed in Fortress config as a system user that can't be removed via API.
        // Is there a match between this userId and a Fortress system user?
        User checkUser = read( user, true );
        if ( checkUser.isSystem() != null && checkUser.isSystem() )
        {
            String warning = "delete userId [" + user.getUserId()
                + "] can't be removed due to policy violation, rc=" + GlobalErrIds.USER_PLCY_VIOLATION;
            throw new SecurityException( GlobalErrIds.USER_PLCY_VIOLATION, warning );
        }
        return uDao.remove( user );
    }


    /**
     * Removes the user's association from OpenLDAP password policy.  Once this association is removed, the User
     * password policy will default to that which is default for ldap server.
     *
     * @param user contains the userId for target user.
     * @throws SecurityException in the event of DAO error.
     */
    void deletePwPolicy( User user ) throws SecurityException
    {
        uDao.deletePwPolicy( user );
    }


    /**
     * This method performs authentication only.  It does not activate RBAC roles in session.  It will evaluate
     * password policies.
     *
     * @param user  Contains the userid of the user signing on along with password.
     * @return Session object will be returned if authentication successful.  This will not contain user's roles.
     * @throws SecurityException in the event of data validation failure, security policy violation or DAO error.
     */
    Session authenticate( User user ) throws SecurityException
    {
        Session session;
        session = uDao.checkPassword( user );

        if ( !session.isAuthenticated() )
        {
            String info = "UserP.authenticate failed  for userId [" + user.getUserId() + "] reason code ["
                + session.getErrorId() + "] msg [" + session.getMsg() + "]";
            throw new PasswordException( session.getErrorId(), info );
        }

        VUtil.validateConstraints( session, VUtil.ConstraintType.USER, false );

        return session;
    }


    /**
     * CreateSession
     * <p>
     * This method is called by AccessMgr and is not intended for use outside Fortress core.  The successful
     * result is Session object that contains target user's RBAC and Admin role activations.  In addition to checking
     * user password validity it will apply configured password policy checks.  Method may also store parms passed in for
     * audit trail..
     * <ul>
     * <li> authenticate user password
     * <li> password policy evaluation with OpenLDAP PwPolicy
     * <li> evaluate temporal constraints on User and UserRole entities.
     * <li> allow selective role activations into User RBAC Session.
     * <li> require valid password if trusted == false.
     * <li> will disallow any user who is locked out due to OpenLDAP pw policy, regardless of trusted flag being set as parm on API.
     * <li> return User's RBAC Session containing User and UserRole attributes.
     * <li> throw a SecurityException for authentication failures, other policy violations, data validation errors or system failure.
     * </ul>
     * </p>
     * <p>
     * The function is valid if and only if:
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * </p>
     * <p>
     * The User parm contains the following (* indicates required)
     * <ul>
     * <li> String userId*
     * <li> char[] password
     * <li> List<UserRole> userRoles contains a list of RBAC role names authorized for user and targeted for activation within this session.
     * <li> List<UserAdminRole> userAdminRoles contains a list of Admin role names authorized for user and targeted for activation.
     * <li> Properties logonProps collection of auditable name/value pairs to store.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * </p>
     * <p>
     * Notes:
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter.
     * </ul>
     * </p>
     *
     * @param user    Contains userId, password (optional if "trusted"), optional User RBAC Roles: List<UserRole> rolesToBeActivated., optional User Admin Roles: List<UserAdminRole> adminRolesToBeActivated.
     * @param trusted if true password is not required.
     * @return Session object will contain authentication result code, RBAC and Admin role activations, OpenLDAP pw policy output and more.
     * @throws SecurityException in the event of data validation failure, security policy violation or DAO error.
     */
    Session createSession( User user, boolean trusted ) throws SecurityException
    {
        Session session;
        if ( trusted )
        {
            // Create the impl session without authentication of password.
            session = createSessionTrusted( user );
            // Check user temporal constraints.  This op usually performed during authentication.
            VUtil.validateConstraints( session, VUtil.ConstraintType.USER, false );
        }
        else
        {
            // Create the impl session if the user authentication succeeds:
            VUtil.assertNotNullOrEmpty( user.getPassword(), GlobalErrIds.USER_PW_NULL, CLS_NM + ".createSession" );
            session = createSession( user );
        }
        // Did the caller pass in a set of roles for selective activation?
        if ( CollectionUtils.isNotEmpty( user.getRoles() ) )
        {
            // Process selective activation of user's RBAC roles into session:
            List<UserRole> rlsActual = session.getRoles();
            List<UserRole> rlsFinal = new ArrayList<>();
            session.setRoles( rlsFinal );
            // Activate only the intersection between assigned and roles passed into this method:
            for ( UserRole role : user.getRoles() )
            {
                int indx = rlsActual.indexOf( role );
                if ( indx != -1 )
                {
                    UserRole candidateRole = rlsActual.get( indx );
                    rlsFinal.add( candidateRole );
                }
            }
        }
        // Check role temporal constraints + activate roles:
        VUtil.validateConstraints( session, VUtil.ConstraintType.ROLE, true );
        return session;
    }


    /**
     * Called internal to this class only.  Will do all of the session activations of the public method
     * in addition to the password validation.
     *
     * @param inUser   Contains userId that represents rDn of node in ldap directory.
     * @return Session object will contain authentication result code, RBAC and Admin role activations, OpenLDAP pw policy output and more.
     * @throws SecurityException in the event of data validation failure, security policy violation or DAO error.
     */
    private Session createSession( User inUser )
        throws SecurityException
    {
        // read user entity:
        User user = read( inUser, true );
        user.setContextId( inUser.getContextId() );

        // authenticate password, check pw policies and validate user temporal constraints:
        Session session = authenticate( inUser );

        // Set the user entity into the session object:
        session.setUser( user );
        return session;
    }


    /**
     * Trusted session creation method called internal to this class only.  Will do all of the session activations of the public method
     *
     * @param inUser Contains userId that represents rDn of node in ldap directory.
     * @return Session object will contain authentication result code, RBAC and Admin role activations, OpenLDAP pw policy output and more.
     * @throws SecurityException in the event of data validation failure, security policy violation or DAO error.
     */
    private Session createSessionTrusted( User inUser )
        throws SecurityException
    {
        User user = read( inUser, true );
        user.setContextId( inUser.getContextId() );

        if ( user.isLocked() )
        {
            String warning = "createSession failed for userId [" + inUser.getUserId()
                + "] reason user is locked";
            LOG.warn( warning );
            throw new SecurityException( GlobalErrIds.USER_LOCKED_BY_CONST, warning );
        }

        Session session = new ObjectFactory().createSession();
        session.setUserId( inUser.getUserId() );
        // Set this flag to false because user's password was not authenticated.
        session.setAuthenticated( false );
        session.setUser( user );
        return session;
    }


    /**
     * Method will set the OpenLDAP pwlocked attribute which will lock user from being able to signon to the system.
     *
     * @param user Contains userId that represents rDn of node in ldap directory.
     * @throws SecurityException in the event of DAO error.
     */
    void lock( User user ) throws SecurityException
    {
        uDao.lock( user );
    }


    /**
     * Method will reset the OpenLDAP pwlocked attribute which will unlock user and allow to signon to the system.
     *
     * @param user Contains userId that represents rDn of node in ldap directory.
     * @throws SecurityException in the event of DAO  error.
     */
    void unlock( User user ) throws SecurityException
    {
        uDao.unlock( user );
    }


    /**
     * Method will change the user's password and validate user's pw policy in OpenLDAP.
     *
     * @param entity      contains userId and old password.
     * @param newPassword contains the new password which must pass the password policy constraints.
     * @throws SecurityException in the event of data validation failure, password policy violation or DAO error.
     */
    void changePassword( User entity, char[] newPassword ) throws SecurityException
    {
        String userId = entity.getUserId();
        boolean result = uDao.changePassword( entity, newPassword );
        if ( !result )
        {
            LOG.warn( "changePassword failed for user [{}]", userId );
        }
    }


    /**
     * Perform password reset on user entity.  This will change the User password and set the reset flag
     * in OpenLDAP will will force the user to change their password at next logon time.
     *
     * @param user contains the userId and the new password.
     * @throws SecurityException in the event of DAO error.
     */
    void resetPassword( User user ) throws SecurityException
    {
        uDao.resetUserPassword( user );
    }


    /**
     * This command assigns a user to a role.
     * <p>
     * <ul>
     * <li> The command is valid if and only if:
     * <li> The user is a member of the USERS data set
     * <li> The role is a member of the ROLES data set
     * <li> The user is not already assigned to the role
     * <li> The SSD constraints are satisfied after assignment.
     * </ul>
     * </p>
     * <p>
     * Successful completion of this op, the following occurs:
     * </p>
     * <ul>
     * <li> User entity (resides in people container) has role assignment added to aux object class attached to actual user record.
     * <li> Role entity (resides in role container) has userId added as role occupant.
     * <li> (optional) Temporal constraints may be associated with <code>ftUserAttrs</code> aux object class based on:
     * <ul>
     * <li> timeout - number in seconds of session inactivity time allowed.
     * <li> beginDate - YYYYMMDD - determines date when role may be activated.
     * <li> endDate - YYMMDD - indicates latest date role may be activated.
     * <li> beginLockDate - YYYYMMDD - determines beginning of enforced inactive status
     * <li> endLockDate - YYMMDD - determines end of enforced inactive status.
     * <li> beginTime - HHMM - determines begin hour role may be activated in user's session.
     * <li> endTime - HHMM - determines end hour role may be activated in user's session.*
     * <li> dayMask - 1234567, 1 = Sunday, 2 = Monday, etc - specifies which day of week role may be activated.
     * </ul>
     * </ul>
     *
     * @param uRole entity contains userId and role name for targeted assignment.
     * @return String containing the user's DN.  This value is used to update the "roleOccupant" attribute on associated role entity.
     * @throws SecurityException in the event data error in user or role objects or system error.
     */
    String assign( UserRole uRole ) throws SecurityException
    {
        validate( uRole );
        // "assign" custom Fortress role data, i.e. temporal constraints, onto the user node:
        return uDao.assign( uRole );
    }


    /**
     * This command deletes the assignment of the User from the Role entities. The command is
     * valid if and only if the user is a member of the USERS data set, the role is a member of
     * the ROLES data set, and the user is assigned to the role.
     * Any sessions that currently have this role activated will not be effected.
     * Successful completion includes:
     * User entity in USER data set has role assignment removed.
     * Role entity in ROLE data set has userId removed as role occupant.
     * (optional) Temporal constraints will be removed from user aux object if set prior to call.
     *
     * @param uRole entity contains userId and RBAC Role name for targeted assignment.
     * @return String containing the user's DN.  This value is used to remove the "roleOccupant" attribute on associated RBAC Role entity.
     * @throws SecurityException - in the event data error in user or role objects or system error.
     */
    String deassign( UserRole uRole ) throws SecurityException
    {
        validate( uRole );
        // "deassign" custom Fortress role data from the user's node:
        return uDao.deassign( uRole );
    }


    /**
     * This command assigns a user to an admin role.
     * Successful completion of this op, the following occurs:
     * </p>
     * <ul>
     * <li> User entity (resides in people container) has role assignment added to aux object class attached to actual user record.
     * <li> AdminRole entity (resides in admin role container) has userId added as role occupant.
     * <li> (optional) Temporal constraints may be associated with <code>ftUserAttrs</code> aux object class based on:
     * <ul>
     * <li> timeout - number in seconds of session inactivity time allowed.
     * <li> beginDate - YYYYMMDD - determines date when role may be activated.
     * <li> endDate - YYMMDD - indicates latest date role may be activated.
     * <li> beginLockDate - YYYYMMDD - determines beginning of enforced inactive status
     * <li> endLockDate - YYMMDD - determines end of enforced inactive status.
     * <li> beginTime - HHMM - determines begin hour role may be activated in user's session.
     * <li> endTime - HHMM - determines end hour role may be activated in user's session.*
     * <li> dayMask - 1234567, 1 = Sunday, 2 = Monday, etc - specifies which day of week role may be activated.
     * </ul>
     * </ul>
     *
     * @param uRole entity contains userId and Admin Role name for targeted assignment.
     * @return String containing the user's DN.  This value is used to update the "roleOccupant" attribute on associated Admin Role entity.
     * @throws SecurityException in the event data error in user or role objects or system error.
     */
    String assign( UserAdminRole uRole ) throws SecurityException
    {
        validate( uRole );
        // Assign custom Fortress role data, i.e. temporal constraints, onto the user node:
        return uDao.assign( uRole );
    }


    /**
     * This method removes assigned admin role from user entity.  Both user and admin role entities must exist and have role relationship
     * before calling this method.
     * Successful completion:
     * del Role to User assignment in User data set
     * AND
     * User to Role assignment in Admin Role data set.
     *
     * @param uRole entity contains userId and Admin Role name for targeted assignment.
     * @return String containing the user's DN.  This value is used to remove the "roleOccupant" attribute on associated Admin Role entity.
     * @throws SecurityException - in the event data error in user or role objects or system error.
     */
    String deassign( UserAdminRole uRole ) throws SecurityException
    {
        validate( uRole );
        // Deassign custom Fortress role data from the user's node:
        return uDao.deassign( uRole );
    }


    /**
     * Ensure that the passed in variable has the correct fields set.
     *
     * @param uRole - name and userId must be checked.
     * @throws ValidationException - if either are null or empty.
     */
    private void validate( UserRole uRole ) throws ValidationException
    {
        if ( StringUtils.isEmpty( uRole.getUserId() ) )
        {
            throw new ValidationException( GlobalErrIds.USER_ID_NULL, CLS_NM + ".validate userId is NULL" );
        }
        if ( StringUtils.isEmpty( uRole.getName() ) )
        {
            throw new ValidationException( GlobalErrIds.ROLE_NM_NULL, CLS_NM + ".validate name is NULL" );
        }
    }


    /**
     * Method will perform various validations to ensure the integrity of the User entity targeted for insertion
     * or updating in directory.  For example the ou attribute will be "read" from the OrgUnit dataset to ensure
     * that it is valid.  Data reasonability checks will be performed on all non-null attributes.
     * This method will also copy the source constraints to target entity iff the target input entity does not have set
     * prior to calling.
     *
     * @param entity   User entity contains data targeted for insertion or update.  The input role constraints will be accepted.
     * @param isUpdate if true update operation is being performed which specifies a different set of targeted attributes.
     * @throws SecurityException in the event of data validation error or DAO error on Org validation.
     */
    private void validate( User entity, boolean isUpdate )
        throws SecurityException
    {
        if ( !isUpdate )
        {
            // the UserId attribute is required on User:
            VUtil.userId( entity.getUserId() );

            // the cn attribute is optional as input.  entity will default to userId if cn not set by caller on add:
            if ( StringUtils.isNotEmpty( entity.getCn() ) )
            {
                VUtil.safeText( entity.getCn(), GlobalIds.CN_LEN );
            }
            // the sn attribute is optional as input.  entity will default to userId if sn not set by caller on add:
            if ( StringUtils.isNotEmpty( entity.getSn() ) )
            {
                VUtil.safeText( entity.getSn(), GlobalIds.SN_LEN );
            }
            // password is not required on user object but user cannot execute AccessMgr or DelAccessMgr methods w/out pw.
            if ( ArrayUtils.isNotEmpty( entity.getPassword() ) )
            {
                VUtil.password( entity.getPassword() );
            }
            // the OU attribute is required:
            if ( StringUtils.isEmpty( entity.getOu() ) )
            {
                String error = "OU validation failed, null or empty value";
                throw new ValidationException( GlobalErrIds.ORG_NULL_USER, error );
            }
            VUtil.orgUnit( entity.getOu() );
            // ensure ou exists in the OS-U pool:
            OrgUnit ou = new OrgUnit( entity.getOu(), OrgUnit.Type.USER );
            ou.setContextId( entity.getContextId() );
            if ( !orgUnitP.isValid( ou ) )
            {
                String error = "validate detected invalid orgUnit name [" + entity.getOu()
                    + "] adding user with userId [" + entity.getUserId() + "]";
                throw new ValidationException( GlobalErrIds.USER_OU_INVALID, error );
            }
            // description attribute is optional:
            if ( StringUtils.isNotEmpty( entity.getDescription() ) )
            {
                VUtil.description( entity.getDescription() );
            }
        }
        else
        {
            // on User update, all attributes are optional:
            if ( StringUtils.isNotEmpty( entity.getCn() ) )
            {
                VUtil.safeText( entity.getCn(), GlobalIds.CN_LEN );
            }
            if ( StringUtils.isNotEmpty( entity.getSn() ) )
            {
                VUtil.safeText( entity.getSn(), GlobalIds.SN_LEN );
            }
            if ( ArrayUtils.isNotEmpty( entity.getPassword() ) )
            {
                VUtil.password( entity.getPassword() );
            }
            if ( StringUtils.isNotEmpty( entity.getOu() ) )
            {
                VUtil.orgUnit( entity.getOu() );
                // ensure ou exists in the OS-U pool:
                OrgUnit ou = new OrgUnit( entity.getOu(), OrgUnit.Type.USER );
                ou.setContextId( entity.getContextId() );
                if ( !orgUnitP.isValid( ou ) )
                {
                    String error = "validate detected invalid orgUnit name [" + entity.getOu()
                        + "] updating user wth userId [" + entity.getUserId() + "]";
                    throw new ValidationException( GlobalErrIds.USER_OU_INVALID, error );
                }
            }
            if ( StringUtils.isNotEmpty( entity.getDescription() ) )
            {
                VUtil.description( entity.getDescription() );
            }
        }

        // 1 OpenLDAP password policy name must be valid if set:
        if ( StringUtils.isNotEmpty( entity.getPwPolicy() ) )
        {
            PwPolicy policy = new PwPolicy( entity.getPwPolicy() );
            policy.setContextId( entity.getContextId() );
            if ( !policyP.isValid( policy ) )
            {
                String error = "validate detected invalid OpenLDAP policy name [" + entity.getPwPolicy()
                    + "] for userId [" + entity.getUserId()
                    + "]. Assignment is optional for User but must be valid if specified.";
                throw new ValidationException( GlobalErrIds.USER_PW_PLCY_INVALID, error );
            }
        }

        // 2 Validate constraints on User object:
        ConstraintUtil.validate( entity );

        // 3 Validate or copy constraints on RBAC roles:
        if ( CollectionUtils.isNotEmpty( entity.getRoles() ) )
        {
            RoleP rp = new RoleP();
            List<UserRole> roles = entity.getRoles();
            for ( UserRole ure : roles )
            {
                Role inRole = new Role( ure.getName() );
                inRole.setContextId( entity.getContextId() );
                Role role = rp.read( inRole );
                ConstraintUtil.validateOrCopy( role, ure );
            }
        }

        // 4 Validate and copy constraints on Administrative roles:
        if ( CollectionUtils.isNotEmpty( entity.getAdminRoles() ) )
        {
            List<UserAdminRole> uRoles = entity.getAdminRoles();
            for ( UserAdminRole uare : uRoles )
            {
                AdminRole inRole = new AdminRole( uare.getName() );
                inRole.setContextId( entity.getContextId() );
                AdminRole outRole = admRoleP.read( inRole );
                ConstraintUtil.validateOrCopy( outRole, uare );

                // copy the ARBAC AdminRole attributes to UserAdminRole:
                copyAdminAttrs( outRole, uare );
            }
        }
    }

    /**
     * Perform copy on ARBAC attributes.  This is used during assignment of {@link org.apache.directory.fortress.core.model.AdminRole} to {@link org.apache.directory.fortress.core.model.User}.
     * This method does not perform input validations.
     *
     * @param srcR contains source attributes to copy.
     * @param trgR contains the target reference.
     */
    void copyAdminAttrs(Administrator srcR, Administrator trgR)
    {
        trgR.setBeginInclusive(srcR.isBeginInclusive());
        trgR.setEndInclusive(srcR.isEndInclusive());
        trgR.setBeginRange(srcR.getBeginRange());
        trgR.setEndRange(srcR.getEndRange());
        // copy the user and perm pools:
        trgR.setOsPs(srcR.getOsP());
        trgR.setOsUs(srcR.getOsU());
    }
}
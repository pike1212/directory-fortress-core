/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress;

import com.jts.fortress.cfg.Config;
import com.jts.fortress.rbac.ClassUtil;
import com.jts.fortress.util.attr.VUtil;

/**
 * Creates an instance of the PwPolicyMgr object.
 * <p/>
 * The default implementation class is specified as {@link GlobalIds#PSWD_POLICY_DEFAULT_CLASS} but can be overridden by
 * adding the {@link GlobalIds#PSWD_POLICY_IMPLEMENTATION} config property.
 * <p/>

 *
 * @author Shawn McKinney
 * @created October 17, 2009
 */
public class PwPolicyMgrFactory
{
    private static String policyClassName = Config.getProperty(GlobalIds.PSWD_POLICY_IMPLEMENTATION);
    private static final String CLS_NM = PwPolicyMgrFactory.class.getName();

    /**
     * Create and return a reference to {@link PwPolicyMgr} object.
     *
     * @return instance of {@link PwPolicyMgr}.
     * @throws com.jts.fortress.SecurityException in the event of failure during instantiation.
     */
    public static PwPolicyMgr createInstance(String contextId)
        throws SecurityException
    {
        VUtil.assertNotNull(contextId, GlobalErrIds.CONTEXT_NULL, CLS_NM + ".createInstance");
        PwPolicyMgr policyMgr;
        if (policyClassName == null || policyClassName.compareTo("") == 0)
        {
            policyClassName = GlobalIds.PSWD_POLICY_DEFAULT_CLASS;
        }

        policyMgr = (PwPolicyMgr) ClassUtil.createInstance(policyClassName);
        policyMgr.setContextId(contextId);
        return policyMgr;
    }
}

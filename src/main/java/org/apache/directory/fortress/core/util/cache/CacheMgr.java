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
package org.apache.directory.fortress.core.util.cache;

import net.sf.ehcache.CacheManager;
import org.apache.directory.fortress.core.CfgException;
import org.apache.directory.fortress.core.CfgRuntimeException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is a facade and shields internal Fortress objects from specifics of the actual
 * cache implementation that is in use.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class CacheMgr
{
    private static final Logger LOG = LoggerFactory.getLogger( CacheMgr.class.getName() );
    private static final String EHCACHE_CONFIG_FILE = "ehcache.config.file";
    private final CacheManager mEhCacheImpl;
    private static CacheMgr mFtCacheImpl;

    static
    {
        String cacheConfig = Config.getProperty( EHCACHE_CONFIG_FILE );
        try
        {
            // This static block performs the following:
            // 1. Construct an instance of Ehcache's CacheManager object.
            // 2. Requires location of ehcache's config file as parameter.
            // 3. The CacheManager reference then gets passed to constructor of self.
            // 4. Store the reference of self as a static member variable of this class.
            mFtCacheImpl = new CacheMgr( new CacheManager( ClassUtil.resourceAsStream( cacheConfig ) ) );
        }
        catch(CfgException ce)
        {
            // The ehcache file cannot be located on this program's classpath.  Ehcache is required, throw runtime exception.
            LOG.error( "CfgException caught in static initializer=" + ce.getMessage());
            throw new CfgRuntimeException( GlobalErrIds.FT_CACHE_NOT_CONFIGURED, cacheConfig, ce );
        }
    }

    /**
     * Private constructor.
     *
     * @param cacheMangerImpl contains a reference to cache implementation manager.
     */
    private CacheMgr( CacheManager cacheMangerImpl )
    {
        mEhCacheImpl = cacheMangerImpl;
    }

    /**
     * Create or return the fortress cache manager reference.
     * @return handle to the cache manager in effect for process.
     */
    public static CacheMgr getInstance()
    {
        return mFtCacheImpl;
    }

    /**
     * Create a new reference to the ehcache cache implementation.
     *
     * @param cacheName contains the name of the cache to retrieve
     * @return reference to cache for specified object.
     */
    public Cache getCache( String cacheName )
    {
        return CacheFactory.createInstance( cacheName, mEhCacheImpl );
    }

    /**
     * Used to clear all elements from all cache objects.
     *
     */
    public void clearAll()
    {
        mEhCacheImpl.clearAll();
    }
}

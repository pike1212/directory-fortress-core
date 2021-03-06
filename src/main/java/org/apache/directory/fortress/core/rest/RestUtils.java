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
package org.apache.directory.fortress.core.rest;


import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.directory.fortress.core.model.FortRequest;
import org.apache.directory.fortress.core.model.FortResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.model.ObjectFactory;
import org.apache.directory.fortress.core.RestException;
import org.apache.directory.fortress.core.util.Config;
import org.apache.directory.fortress.core.model.Props;
import org.apache.directory.fortress.core.util.crypto.EncryptUtil;


/**
 * This utility class provides methods that wrap Apache's HTTP Client APIs.  This class is thread safe.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RestUtils
{
    private static final String CLS_NM = RestUtils.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private static final String HTTP_UID = Config.getProperty( "http.user" );
    private static final String HTTP_PW_PARAM = "http.pw";
    private static final String HTTP_PW = ( ( EncryptUtil.isEnabled() ) ? EncryptUtil.decrypt( Config
        .getProperty( HTTP_PW_PARAM ) ) : Config.getProperty( HTTP_PW_PARAM ) );
    private static final String HTTP_HOST = Config.getProperty( "http.host" );
    private static final String HTTP_PORT = Config.getProperty( "http.port" );
    private static final String HTTP_PROTOCOL = Config.getProperty( "http.protocol", "http" );
    private static final String VERSION = System.getProperty( "version" );
    private static final String SERVICE = "fortress-rest-" + VERSION;
    // TODO: add SSL capability here:
    private static final String URI = HTTP_PROTOCOL + "://" + HTTP_HOST + ":" + HTTP_PORT + "/" + SERVICE + "/";
    private static final int HTTP_OK = 200;
    private static final int HTTP_401_UNAUTHORIZED = 401;
    private static final int HTTP_403_FORBIDDEN = 403;
    private static final int HTTP_404_NOT_FOUND = 404;
    private static CachedJaxbContext cachedJaxbContext = new CachedJaxbContext();

    /**
     * Used to manage trust store properties.  If enabled, create SSL connection.
     *
     */
    private static final String TRUST_STORE = Config.getProperty( "trust.store" );
    private static final String TRUST_STORE_PW = Config.getProperty( "trust.store.password" );
    private static final String SET_TRUST_STORE_PROP = "trust.store.set.prop";
    private static final boolean IS_SET_TRUST_STORE_PROP = (
        Config.getProperty( SET_TRUST_STORE_PROP ) != null &&
        Config.getProperty( SET_TRUST_STORE_PROP ).equalsIgnoreCase( "true" ) );

    static
    {
        if ( IS_SET_TRUST_STORE_PROP )
        {
            LOG.info( "Set JSSE truststore properties:" );
            LOG.info( "javax.net.ssl.trustStore: {}", TRUST_STORE );
            System.setProperty( "javax.net.ssl.trustStore", TRUST_STORE );
            System.setProperty( "javax.net.ssl.trustStorePassword", TRUST_STORE_PW );
        }
    }


    /**
     * Marshall the request into an XML String.
     *
     * @param request
     * @return String containing xml request
     * @throws RestException
     */
    public static String marshal( FortRequest request ) throws RestException
    {
        String szRetValue;
        try
        {
            // Create a JAXB context passing in the class of the object we want to marshal/unmarshal
            final JAXBContext context = cachedJaxbContext.getJaxbContext( FortRequest.class );
            // =============================================================================================================
            // Marshalling OBJECT to XML
            // =============================================================================================================
            // Create the marshaller, that will transform the object into XML
            final Marshaller marshaller = context.createMarshaller();
            // Create a stringWriter to hold the XML
            final StringWriter stringWriter = new StringWriter();
            // Marshal the javaObject and write the XML to the stringWriter
            marshaller.marshal( request, stringWriter );
            szRetValue = stringWriter.toString();
        }
        catch ( JAXBException je )
        {
            String error = "marshal caught JAXBException=" + je;
            throw new RestException( GlobalErrIds.REST_MARSHALL_ERR, error, je );
        }
        return szRetValue;
    }


    /**
     * Unmarshall the XML response into its associated Java objects.
     *
     * @param szResponse
     * @return FortResponse
     * @throws RestException
     */
    public static FortResponse unmarshall( String szResponse ) throws RestException
    {
        FortResponse response;
        try
        {
            // Create a JAXB context passing in the class of the object we want to marshal/unmarshal
            final JAXBContext context = cachedJaxbContext.getJaxbContext( FortResponse.class );

            // Create the unmarshaller, that will transform the XML back into an object
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            response = ( FortResponse ) unmarshaller.unmarshal( new StringReader( szResponse ) );
        }
        catch ( JAXBException je )
        {
            String error = "unmarshall caught JAXBException=" + je;
            throw new RestException( GlobalErrIds.REST_UNMARSHALL_ERR, error, je );
        }
        return response;
    }


    /**
     * Perform HTTP Get REST request.
     *
     * @param userId
     * @param password
     * @param id
     * @param id2
     * @param id3
     * @param function
     * @return String containing response
     * @throws RestException
     */
    public static String get( String userId, String password, String id, String id2, String id3, String function )
        throws RestException
    {
        String url = URI + function + "/" + id;
        if ( id2 != null )
        {
            url += "/" + id2;
        }
        if ( id3 != null )
        {
            url += "/" + id3;
        }
        LOG.debug( "get function1:{}, id1:{}, id2:{}, id3:{}, url:{}", function, id, id2, id3, url );
        HttpGet get = new HttpGet(url);
        setMethodHeaders( get );
        return handleHttpMethod( get ,HttpClientBuilder.create()
                .setDefaultCredentialsProvider(getCredentialProvider(userId, password)).build() );
    }


    /**
     * Perform HTTP Get REST request.
     *
     * @param id
     * @param id2
     * @param id3
     * @param function
     * @return String containing response
     * @throws RestException
     */
    public static String get( String id, String id2, String id3, String function ) throws RestException
    {
        return get( null, null, id, id2, id3, function );
    }


    /**
     * Perform an HTTP Post REST operation.
     *
     * @param userId
     * @param password
     * @param szInput
     * @param function
     * @return String containing response
     * @throws RestException
     */
    public static String post( String userId, String password, String szInput, String function ) throws RestException
    {
        LOG.debug( "post URI=[{}], function=[{}], request=[{}]", URI, function, szInput );
        String szResponse = null;
        HttpPost post = new HttpPost(URI + function);
        post.addHeader( "Accept", "text/xml" );
        setMethodHeaders( post );
        try
        {
            HttpEntity entity = new StringEntity( szInput, ContentType.TEXT_XML );
            post.setEntity( entity );
            org.apache.http.client.HttpClient httpclient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(getCredentialProvider(userId, password)).build();
            HttpResponse response = httpclient.execute( post );
            String error;

            switch ( response.getStatusLine().getStatusCode() )
            {
                case HTTP_OK :
                    szResponse = IOUtils.toString( response.getEntity().getContent(), "UTF-8" );
                    LOG.debug( "post URI=[{}], function=[{}], response=[{}]", URI, function, szResponse );
                    break;
                case HTTP_401_UNAUTHORIZED :
                    error = "post URI=[" + URI + "], function=[" + function
                            + "], 401 function unauthorized on host";
                    LOG.error( error );
                    throw new RestException( GlobalErrIds.REST_UNAUTHORIZED_ERR, error );
                case HTTP_403_FORBIDDEN :
                    error = "post URI=[" + URI + "], function=[" + function
                            + "], 403 function forbidden on host";
                    LOG.error( error );
                    throw new RestException( GlobalErrIds.REST_FORBIDDEN_ERR, error );
                case HTTP_404_NOT_FOUND :
                    error = "post URI=[" + URI + "], function=[" + function + "], 404 not found from host";
                    LOG.error( error );
                    throw new RestException( GlobalErrIds.REST_NOT_FOUND_ERR, error );
                default :
                    error = "post URI=[" + URI + "], function=[" + function
                            + "], error received from host: " + response.getStatusLine().getStatusCode();
                    LOG.error( error );
                    throw new RestException( GlobalErrIds.REST_UNKNOWN_ERR, error );
            }
        }
        catch ( IOException ioe )
        {
            String error = "post URI=[" + URI + "], function=[" + function + "] caught IOException=" + ioe;
            LOG.error( error );
            throw new RestException( GlobalErrIds.REST_IO_ERR, error, ioe );
        }
        catch ( WebApplicationException we )
        {
            String error = "post URI=[" + URI + "], function=[" + function
                    + "] caught WebApplicationException=" + we;
            LOG.error( error );
            throw new RestException( GlobalErrIds.REST_WEB_ERR, error, we );
        }
        catch ( java.lang.NoSuchMethodError e )
        {
            String error = "post URI=[" + URI + "], function=[" + function
                + "] caught Exception=" + e;
            LOG.error( error );
            e.printStackTrace();
            throw new RestException( GlobalErrIds.REST_UNKNOWN_ERR, error );
        }
        finally
        {
            // Release current connection to the connection pool.
            post.releaseConnection();
        }
        return szResponse;
    }


    /**
     * Perform an HTTP Post REST operation.
     *
     * @param szInput
     * @param function
     * @return String containing response
     * @throws RestException
     */
    public static String post( String szInput, String function ) throws RestException
    {
        return post(null,null,szInput, function);
    }

    private static CredentialsProvider getCredentialProvider(String uid, String password) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials( new AuthScope(HTTP_HOST,Integer.valueOf(HTTP_PORT)),
                new UsernamePasswordCredentials(uid==null?HTTP_UID:uid,password==null?HTTP_PW:password) );
        return credentialsProvider;
    }

    /**
     * Set these params into their associated HTTP header vars.
     *
     * @param httpRequest
     */
    private static void setMethodHeaders( HttpRequest httpRequest )
    {
        if ( httpRequest instanceof HttpPost || httpRequest instanceof HttpPut)
        {
            httpRequest.addHeader( "Content-Type", "application/xml" );
            httpRequest.addHeader( "Accept", "application/xml" );
        }
    }


    /**
     * Convert from non-Base64 to Base64 encoded.
     *
     * @param value
     * @return String contains encoded data
     */
    private static String base64Encode( String value )
    {
        return Base64Utility.encode( value.getBytes() );
    }


    /**
     * Process the HTTP method request.
     *
     * @param httpGetRequest
     * @return String containing response
     * @throws Exception
     */
    private static String handleHttpMethod( HttpRequestBase httpGetRequest ,org.apache.http.client.HttpClient client ) throws RestException
    {
        String szResponse = null;
        try
        {
            HttpResponse response = client.execute( httpGetRequest );
            LOG.debug( "handleHttpMethod Response status : {}", response.getStatusLine().getStatusCode() );

            Response.Status status = Response.Status.fromStatusCode( response.getStatusLine().getStatusCode() );

            if ( status == Response.Status.OK )
            {
                szResponse = IOUtils.toString( response.getEntity().getContent() );
                LOG.debug( szResponse );
            }
            else if ( status == Response.Status.FORBIDDEN )
            {
                LOG.debug( "handleHttpMethod Authorization failure" );
            }
            else if ( status == Response.Status.UNAUTHORIZED )
            {
                LOG.debug( "handleHttpMethod Authentication failure" );
            }
            else
            {
                LOG.debug( "handleHttpMethod Unknown error" );
            }
        }
        catch ( IOException ioe )
        {
            String error = "handleHttpMethod caught IOException=" + ioe;
            LOG.error( error );
            throw new RestException( GlobalErrIds.REST_IO_ERR, error, ioe );
        }
        finally
        {
            // Release current connection to the connection pool.
            httpGetRequest.releaseConnection();
        }
        return szResponse;
    }


    /**
     * @param inProps
     * @return Properties
     */
    public static Properties getProperties( Props inProps )
    {
        Properties properties = null;
        List<Props.Entry> props = inProps.getEntry();
        if ( props.size() > 0 )
        {
            properties = new Properties();
            //int size = props.size();
            for ( Props.Entry entry : props )
            {
                String key = entry.getKey();
                String val = entry.getValue();
                properties.setProperty( key, val );
            }
        }
        return properties;
    }


    /**
     *
     * @param properties
     * @return Prop contains name value pairs.
     */
    public static Props getProps( Properties properties )
    {
        Props props = null;
        if ( properties != null )
        {
            props = new ObjectFactory().createProps();
            for ( Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); )
            {
                String key = ( String ) e.nextElement();
                String val = properties.getProperty( key );
                Props.Entry entry = new Props.Entry();
                entry.setKey( key );
                entry.setValue( val );
                props.getEntry().add( entry );
            }
        }
        return props;
    }
}
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
package org.apache.directory.fortress.core.model;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;


@XmlRootElement(name = "fortGroup")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group", propOrder =
    {
        "name",
        "description",
        "protocol",
        "members",
        "props",
})
public class Group extends FortEntity implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private String protocol;
    private List<String> members;
    private Props props = new Props();
    private boolean memberDn;


    /**
     * Default constructor used by {@link org.apache.directory.fortress.core.ant.FortressAntTask}
     */
    public Group()
    {
    }


    /**
     * Generate instance of group to be loaded as ldap object.
     *
     * @param name        maps to 'cn' attribute in group object class.
     */
    public Group( String name )
    {
        this.name = name;
    }


    /**
     * Generate instance of group to be loaded as ldap object.
     *
     * @param name        maps to 'cn' attribute in group object class.
     * @param description maps to 'description' attribute in group object class.
     */
    public Group( String name, String description )
    {
        this.name = name;
        this.description = description;
    }


    /**
     * Get the second level qualifier on the domain component.  This attribute is required.
     *
     * @return name maps to 'dcObject' object class.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Set the second level qualifier on the domain component.  This attribute is required.
     *
     * @param name maps to 'dcObject' object class.
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * Get the description for the domain component.  This value is not required or constrained
     * but is validated on reasonability.
     *
     * @return field maps to 'o' attribute on 'dcObject'.
     */
    public String getDescription()
    {
        return description;
    }


    /**
     * Set the description for the domain component.  This value is not required or constrained
     * but is validated on reasonability.
     *
     * @param description maps to 'o' attribute on 'dcObject'.
     */
    public void setDescription( String description )
    {
        this.description = description;
    }


    /**
     * Get protocol qualifier for this entity.
     *
     * @return protocol.
     */
    public String getProtocol()
    {
        return protocol;
    }


    /**
     * Set the protocol qualifier for this entity.
     *
     * @param protocol contains protocol qualifier for this entity.
     */
    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }


    /**
     * Add a single userId as member of this entity.
     *
     * @param userId
     */
    public void setMember( String userId )
    {
        if ( members == null )
        {
            members = new ArrayList<>();
        }
        members.add( userId );
    }


    /**
     * Return the members
     *
     * @return List of type String containing userIds.
     */
    public List<String> getMembers()
    {
        return members;
    }


    /**
     * Set a member on this entity using a comma delimited String.
     *
     * @param members String contains one or more userids in comma delimited format.
     */
    public void setMembers( String members )
    {
        if ( members != null )
        {
            StringTokenizer tkn = new StringTokenizer( members, "," );
            if ( tkn.countTokens() > 0 )
            {
                while ( tkn.hasMoreTokens() )
                {
                    String member = tkn.nextToken();
                    setMember( member );
                }
            }
        }
    }


    /**
     * Set members onto this entity using a List of userIds.
     *
     * @param members List of type String contains userIds to be associated as members of this group.
     */
    public void setMembers( List<String> members )
    {
        this.members = members;
    }


    /**
     * Add name/value pair to list of properties associated with PermObj.  These values are not constrained by Fortress.
     * Properties are optional.
     *
     * @param key   contains property name and maps to 'ftProps' attribute in 'ftProperties' aux object class.
     * @param value
     */
    public void addProperty( String key, String value )
    {
        Props.Entry entry = new Props.Entry();
        entry.setKey( key );
        entry.setValue( value );
        this.props.getEntry().add( entry );
    }


    /**
     * Get a name/value pair attribute from list of properties associated with PermObj.  These values are not constrained by Fortress.
     * Properties are optional.
     *
     * @param key contains property name and maps to 'ftProps' attribute in 'ftProperties' aux object class.
     * @return value containing name/value pair that maps to 'ftProps' attribute in 'ftProperties' aux object class.
     */
    public String getProperty( String key )
    {
        List<Props.Entry> props = this.props.getEntry();
        Props.Entry keyObj = new Props.Entry();
        keyObj.setKey( key );

        String value = null;
        int indx = props.indexOf( keyObj );
        if ( indx != -1 )
        {
            Props.Entry entry = props.get( props.indexOf( keyObj ) );
            value = entry.getValue();
        }

        return value;
    }


    /**
     * Replace teh collection of name/value pairs to attributes associated with Group entity.  These values are not constrained by Fortress.
     * Properties are optional.
     *
     * @param properties contains collection of name/value pairs and maps to 'ftProps' attribute in 'ftProperties' aux object class.
     */
    public void setProperties( Properties properties )
    {
        if ( properties != null )
        {
            // reset the existing properties stored in this entity.
            props = new Props();

            for ( Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); )
            {
                // This LDAP attr is stored as a name-value pair separated by a ':'.
                String key = ( String ) e.nextElement();
                String val = properties.getProperty( key );
                addProperty( key, val );
            }
        }
    }


    /**
     * Add new collection of name=value pairs to attributes associated with Group.  These values are not constrained by Fortress.
     * Properties are optional.
     *
     * @param properties contains name=value pairs that are comma delmited.
     */
    public void setProperties( String properties )
    {
        setProperties( PropUtil.getProperties( properties, '=', "," ) );
    }


    /**
     * Return the collection of name/value pairs to attributes associated with PermObj.  These values are not constrained by Fortress.
     * Properties are optional.
     *
     * @return Properties contains collection of name/value pairs and maps to 'ftProps' attribute in 'ftProperties' aux object class.
     */
    public Properties getProperties()
    {
        Properties properties = null;
        List<Props.Entry> props = this.props.getEntry();
        if ( props.size() > 0 )
        {
            properties = new Properties();
            for ( Props.Entry entry : props )
            {
                String key = entry.getKey();
                String val = entry.getValue();
                properties.setProperty( key, val );
            }
        }
        return properties;
    }


    public List<String> getPropList()
    {
        List<Props.Entry> props = this.props.getEntry();
        List<String> propList = null;
        if ( props.size() > 0 )
        {
            propList = new ArrayList<>();
            for ( Props.Entry entry : props )
            {
                String key = entry.getKey();
                String val = entry.getValue();
                String prop = key + "=" + val;
                propList.add( prop );
            }
        }
        return propList;
    }


    /**
     * Gets the value of the Props property.  This method is used by Fortress and En Masse and should not be called by external programs.
     *
     * @return {@link Props }
     *
     */
    public Props getProps()
    {
        return props;
    }


    /**
     * Sets the value of the Props property.  This method is used by Fortress and En Masse and should not be called by external programs.
     *
     * @param props
     *     allowed object is
     *     {@link Props }
     *
     */
    public void setProps( Props props )
    {
        this.props = props;
    }


    /**
     * Set if userDn's are loaded in dn format.
     *
     * @return true indicates members are in dn format.
     */
    public boolean isMemberDn()
    {
        return memberDn;
    }


    /**
     * Set to 'true' if members are in dn format.
     *
     * @param memberDn boolean value, set to 'true' if distinguished name (dn) format, 'false' if relative distinguished name (rdn) format.
     */
    public void setMemberDn( boolean memberDn )
    {
        this.memberDn = memberDn;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Group group = ( Group ) o;

        if ( name == null )
        {
            return false;
        }

        return name.equals( group.name );
    }


    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + ( description != null ? description.hashCode() : 0 );
        result = 31 * result + ( protocol != null ? protocol.hashCode() : 0 );
        result = 31 * result + ( members != null ? members.hashCode() : 0 );
        result = 31 * result + ( props != null ? props.hashCode() : 0 );
        return result;
    }


    @Override
    public String toString()
    {
        return "Group{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * VoltDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VoltDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltcore.utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json_voltpatches.JSONException;
import org.json_voltpatches.JSONObject;
import org.json_voltpatches.JSONStringer;

public class InstanceId
{
    private final int m_coord;
    private final long m_timestamp;

    public InstanceId(int coord, long timestamp)
    {
        m_coord = coord;
        m_timestamp = timestamp;
    }

    public InstanceId(JSONObject jsObj) throws JSONException
    {
        m_coord = jsObj.getInt("coord");
        m_timestamp = jsObj.getLong("timestamp");
    }

    public int getCoord()
    {
        return m_coord;
    }

    public long getTimestamp()
    {
        return m_timestamp;
    }

    public long getHash()
    {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putLong(m_timestamp);
        buf.putInt(m_coord);
        buf.flip();

        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return 0;
        }
        md.update(buf);
        byte[] digest = md.digest();
        return ByteBuffer.wrap(digest).getLong();
    }

    public JSONObject serializeToJSONObject() throws JSONException
    {
        JSONStringer stringer = new JSONStringer();
        stringer.object();
        stringer.key("coord").value(m_coord);
        stringer.key("timestamp").value(m_timestamp);
        stringer.endObject();
        return new JSONObject(stringer.toString());
    }
}

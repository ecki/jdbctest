/*
 * ConnectTests.java
 *
 * created at 2013-10-02 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 *
 * License: ASL 2.0
 */
package net.eckenfels.tests.jdbc;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import oracle.jdbc.pool.OracleDataSource;


public class ConnectTests
{
    static class ConnectionInfo
    {
        public long start;
        public long duration;
        public Connection connection;
        public String schema;
        public String instance;
        public String service;
        public String server;
        public int errorcount;
        public String lasterror;

        @Override
        public String toString()
        {
            return ((errorcount>0)?"ERROR":"OK")+" schema=" + schema + " inst=" + instance + " serv=" + service + " on=" + server + ((lasterror!=null)?" " + lasterror:"");
        }

        public void execute(String sql)
        {
            try
            {
                PreparedStatement s = connection.prepareStatement(sql);
                try
                {
                    s.execute();
                } finally {
                    s.close();
                }
            } catch (SQLException ignored) {
                System.out.println("Execute failed " + ignored + " for " + sql);
            }
        }

    }

    private static final int DEFAULT_ITERATIONS = 100;

    private boolean first = true;

    private String user;
    private String pass;
    private String url;

    public ConnectTests(String user, String pass, String url)
    {
        this.user = user;
        this.pass = pass;
        this.url = url;
    }


    public static void main(String... args) throws SQLException, IOException
    {
        if (args.length < 2 ||args.length > 3)
        {
            throw new IllegalArgumentException("Specify Main [-Diterations="+DEFAULT_ITERATIONS+"] <user> <url> [<pass>]");
        }

        System.out.printf("Connection Test jre=%s %s loc=%s tz=%s os=%s/%s%n", System.getProperty("java.version"), System.getProperty("java.vendor"), Locale.getDefault().toString(), TimeZone.getDefault().getID(), System.getProperty("os.name", "N/A"), System.getProperty("os.version"));


        // extract options and arguments
        String user = args[0];
        String url = args[1];

        String pass;
        if (args.length < 2)
        {
            pass = String.valueOf(System.console().readPassword("Enter password for user {}: ", user));
        } else {
            pass = args[2];
        }

        final int iterations = Integer.getInteger("iterations", DEFAULT_ITERATIONS).intValue();


        ConnectTests me = new ConnectTests(user, pass, url);
        me.runTest(null, iterations); // default is accept encryption

        // the following tests may fail, depending on server settings

        me = new ConnectTests(user,pass,url);
        me.runTest("REQUIRED", iterations);

        me = new ConnectTests(user,pass,url);
        me.runTest("REJECTED", iterations);
    }

    @SuppressWarnings("boxing")
    private void runTest(String encryption, int iterations) throws SQLException
    {

        System.out.printf("%nConnecting with user=%s url=%s%n  iterations=%d enc=%s oracle.jdbc.ReadTimeout=%d oracle.net.CONNECT_TIMEOUT=%d%n%n", user, url, Integer.valueOf(iterations), encryption, Integer.getInteger("oracle.jdbc.ReadTimeout", -1), Integer.getInteger("oracle.net.CONNECT_TIMEOUT", -1));

        OracleDataSource ods = new OracleDataSource();

        ods.setURL(url);
        ods.setUser(user);
        ods.setPassword(pass);
        Properties prop = new Properties();
        //if (connTO > 0)
        //    prop.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(connTO));
        //if (readTO > 0)
        //    prop.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(readTO));
        //prop.setProperty("javax.net.ssl.trustStore", new File("client.jks").getAbsolutePath());
        //prop.setProperty("javax.net.ssl.trustStoreType","JKS");
        //prop.setProperty("javax.net.ssl.trustStorePassword","secret1234");

        if ("REJECTED".equalsIgnoreCase(encryption))
        {
            prop.put("oracle.net.encryption_client", "REJECTED");
            prop.put("oracle.net.crypto_checksum_client", "REJECTED");
        }
        else if ("REQUIRED".equalsIgnoreCase(encryption))
        {
            prop.put("oracle.net.encryption_client", "REQUIRED");
            //prop.put("oracle.net.encryption_types_client", "( AES128 )");
            prop.put("oracle.net.crypto_checksum_client", "REQUIRED");
            //prop.put("oracle.net.crypto_checksum_types_client", "( SHA1 )");
        }
        else if ("ACCEPTED".equalsIgnoreCase(encryption))
        {
            // overwrite the defaults
            prop.put("oracle.net.encryption_client", "REQUIRED");
            //prop.put("oracle.net.encryption_types_client", "( AES128 )");
            prop.put("oracle.net.crypto_checksum_client", "REQUIRED");
            //prop.put("oracle.net.crypto_checksum_types_client", "( SHA1 )");
        }
        else if ("REQUESTED".equalsIgnoreCase(encryption))
        {
            prop.put("oracle.net.encryption_client", "REQUESTED");
            //prop.put("oracle.net.encryption_types_client", "( AES128 )");
            prop.put("oracle.net.crypto_checksum_client", "REQUESTED");
            //prop.put("oracle.net.crypto_checksum_types_client", "( SHA1 )");
        }

        ods.setConnectionProperties(prop);
        DataSource ds = ods;

        // first pair of warmup connections

        ConnectionInfo ci = connect(ds);
        fillInstanceInfo(ci);
        System.out.printf("First Connection: %s  time=%.6fms   from=%s prod=%s%n", ci, (ci.duration / 1000000.0), ci.connection.getClass().getProtectionDomain().getCodeSource().toString(), ci.connection.getMetaData().getDatabaseProductName());
        closeConnection(ci);
        if (ci.errorcount > 0)
            return;

        ci = connect(ds);
        fillInstanceInfo(ci);
        System.out.printf("Second Connection: %s  time=%.6fms%n%n", ci, (ci.duration / 1000000.0));
        closeConnection(ci);
        if (ci.errorcount > 0)
            return;

        // now do the iterations (single threaded)

        Map<String, AtomicInteger> hash = new HashMap<String, AtomicInteger>();
        long min = Long.MAX_VALUE;
        long max = 0;
        long sum = 0;

        System.out.println("Starting Iterations...");

        for(int i=0; i<iterations;i++)
        {
            ci = connect(ds);
            fillInstanceInfo(ci);
            closeConnection(ci);

            // aggregate statistics
            sum += ci.duration;
            if (ci.duration < min)
            {
                min = ci.duration;
            }
            if (ci.duration > max)
            {
                max = ci.duration;
            }

            // find a execution variant bucket and increment counter
            // groups service/server/instance/status,error/schema
            String key = ci.toString();

            // first occurrence or later occurrences?
            AtomicInteger count = hash.get(key);
            if (count == null)
            {
                count = new AtomicInteger(1);
                hash.put(key, count);
            }
            else
            {
                count.incrementAndGet();
            }

            // sleep short time to not overload the DB
            sleepMS(15);
        }

        System.out.printf("After %d connects: min=%.6fms avg=%.6fms max=%.6fms%n", Integer.valueOf(iterations), (min/1000000.0),(sum/iterations/1000000.0),(max/1000000.0));

        // print the counts for each occurred variant
        for(Map.Entry<String, AtomicInteger> e : hash.entrySet())
        {
            System.out.printf("%5d %s%n", e.getValue(), e.getKey());
        }
    }

    private void sleepMS(int ms)
    {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void closeConnection(ConnectionInfo ci)
    {
        Connection c;
        if (ci == null || (c = ci.connection) == null)
        {
            return;
        }

        try
        {
            c.close();
        }
        catch (SQLException e) { e.printStackTrace();}
    }

    private void fillInstanceInfo(ConnectionInfo ci)
    {
        try
        {
            Connection c = ci.connection;

            if (c == null)
            {
                return;
            }

            PreparedStatement ps = c.prepareStatement("SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') CURRSCHEMA, SYS_CONTEXT('USERENV', 'INSTANCE') INSTANCE, SYS_CONTEXT('USERENV','SERVICE_NAME') SERVICE_NAME, SYS_CONTEXT('USERENV', 'SERVER_HOST') SERVER_HOST FROM DUAL");
            ResultSet rs = ps.executeQuery();
            rs.next();
            ci.schema = rs.getString("CURRSCHEMA");
            ci.instance = rs.getString("INSTANCE");
            ci.service = rs.getString("SERVICE_NAME");
            ci.server = rs.getString("SERVER_HOST");
            rs.close();
            ps.close();
            /*
            PreparedStatement ps = c.prepareStatement("SELECT @@ServerName");
            ResultSet rs = ps.executeQuery();
            rs.next();
            ci.server = rs.getString(1);
            rs.close();
            ps.close();*/

        }
        catch (SQLException e)
        {
            fillError(ci, e);
        }
    }

    private void fillError(ConnectionInfo ci, SQLException e)
    {
        ci.errorcount++;
        ci.lasterror = e.toString();
        if (first)
        {
            System.out.println("" + e + " " + e.getErrorCode() + " " + e.getSQLState());
            e.printStackTrace();
            first = false;
        }
    }

    private ConnectionInfo connect(DataSource ds)
    {
        // fill time and error into connection info
        ConnectionInfo ci = new ConnectionInfo();

        long start = System.nanoTime();

        try
        {
            ci.connection = ds.getConnection();
        }
        catch (SQLException e)
        {
            fillError(ci, e);
        }
        long duration = System.nanoTime() - start;
        ci.start = start;
        ci.duration = duration;

        return ci;
    }
}




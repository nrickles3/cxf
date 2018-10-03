/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.https.ciphersuites;

import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * A set of tests for TLS ciphersuites
 */
public class CipherSuitesTest extends AbstractBusClientServerTestBase {
    static final boolean UNRESTRICTED_POLICIES_INSTALLED;
    static {
        boolean ok = false;
        try {
            byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            SecretKey key192 = new SecretKeySpec(
                new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
                            "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            ok = true;
        } catch (Exception e) {
            //
        }
        UNRESTRICTED_POLICIES_INSTALLED = ok;
    }

    static final String PORT = allocatePort(CipherSuitesServer.class);
    static final String PORT2 = allocatePort(CipherSuitesServer.class, 2);
    static final String PORT3 = allocatePort(CipherSuitesServer.class, 3);
    static final String PORT4 = allocatePort(CipherSuitesServer.class, 4);
    static final String PORT5 = allocatePort(CipherSuitesServer.class, 5);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(CipherSuitesServer.class, true)
        );
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // Both client + server include AES
    @org.junit.Test
    public void testAESIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES
    @org.junit.Test
    public void testAESIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        updateAddressPort(port, PORT);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include a specific AES CipherSuite (not via a filter)
    @org.junit.Test
    public void testAESIncludedExplicitly() throws Exception {

        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        if (!UNRESTRICTED_POLICIES_INSTALLED) {
            return;
        }
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-explicit-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT4);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client only includes DHE, server excludes it
    @org.junit.Test
    public void testClientDHEServerExcludesIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-dhe-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client only includes DHE, server excludes it
    @org.junit.Test
    public void testClientDHEServerExcludesIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-dhe-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        updateAddressPort(port, PORT);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include DHE
    @org.junit.Test
    public void testDHEIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-dhe-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT2);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include DHE
    @org.junit.Test
    public void testDHEIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-dhe-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        updateAddressPort(port, PORT2);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client only includes ECDHE, server only includes DHE
    @org.junit.Test
    public void testClientECDHEServerDHEIncluded() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT2);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client only includes ECDHE, server only includes DHE
    @org.junit.Test
    public void testClientECDHEServerDHEIncludedAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        updateAddressPort(port, PORT2);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client does not allow NULL
    @org.junit.Test
    public void testClientAESServerNULL() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT3);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client does not allow NULL
    @org.junit.Test
    public void testClientAESServerNULLAsync() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        updateAddressPort(port, PORT3);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client enables a TLS v1.2 CipherSuite
    @org.junit.Test
    public void testAESIncludedTLSv12() throws Exception {
        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-tlsv12.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client enables a TLS v1.2 CipherSuite
    @org.junit.Test
    public void testAESIncludedTLSv12ViaCode() throws Exception {
        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-noconfig.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager = new NoOpX509TrustManager();
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        tlsParams.setSecureSocketProtocol("TLSv1.2");
        tlsParams.setCipherSuites(Collections.singletonList("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"));

        conduit.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client enables a TLS v1.3 CipherSuite
    @org.junit.Test
    public void testAESIncludedTLSv13() throws Exception {
        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        Assume.assumeTrue(JavaUtils.isJava11Compatible());

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-tlsv13.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client enables a TLS v1.3 CipherSuite
    @org.junit.Test
    public void testAESIncludedTLSv13ViaCode() throws Exception {
        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        Assume.assumeTrue(JavaUtils.isJava11Compatible());

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-noconfig.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager = new NoOpX509TrustManager();
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        tlsParams.setSecureSocketProtocol("TLSv1.3");
        tlsParams.setCipherSuites(Collections.singletonList("TLS_AES_128_GCM_SHA256"));

        conduit.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client is TLSv1.1
    @org.junit.Test
    public void testAESIncludedTLSv11() throws Exception {
        // Doesn't work with IBM JDK
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-noconfig.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager = new NoOpX509TrustManager();
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        tlsParams.setSecureSocketProtocol("TLSv1.1");

        conduit.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Both client + server include AES, client is TLSv1.0
    @org.junit.Test
    public void testAESIncludedTLSv10() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-noconfig.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        Client client = ClientProxy.getClient(port);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager = new NoOpX509TrustManager();
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        tlsParams.setSecureSocketProtocol("TLSv1");

        conduit.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Test an expired cert
    @org.junit.Test
    public void testExpiredCert() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CipherSuitesTest.class.getResource("ciphersuites-client-expired-cert.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT5);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on not being able to negotiate a cipher suite");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    private static class NoOpX509TrustManager implements X509TrustManager {

        NoOpX509TrustManager() {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    }
}

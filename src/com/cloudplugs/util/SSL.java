package com.cloudplugs.util;

/*<license>
Copyright 2014 CloudPlugs Inc.

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
</license>*/

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;

/**
 * @brief Tool class for easy manipulation of CA certificates over the SSL network.
 * The developer should avoid a direct usage of this class when connecting to an official CloudPlugs server.
 */
public final class SSL
{
	private SSL() {}

	/**
	 * Disable the peer and host verification when establishing any SSL connection, including HTTPS ones.
	 * This will allow any SSL connection also with untrusted peers in a easy way.
	 * The invocation of this method is discouraged due to security reasons, because it will permit Man-In-The-Middle attacks.
	 */
	public static void trustEveryone() {
		try {
			HttpsURLConnection.setDefaultHostnameVerifier(
				new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				}
			);
			SSLContext ctx = SSLContext.getInstance(DEF_SSL_PROTO);
			ctx.init(null,
				new X509TrustManager[]{
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
					}
				},
				new SecureRandom()
			);
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
		} catch(Exception e) { // should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Allow safe SSL connections to the official CloudPlugs server.
	 * The developer does not need to invoke this method, it will be automatically invoked by other classes of the library.
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static void trustCloudPlugs() throws KeyManagementException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		trustCA(getCA(DEF_CERT));
	}

	/**
	 * Allow safe SSL connections to any server is using the specified certificate.
	 * @param ca the certificate to trust
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static void trustCA(Certificate ca) throws KeyManagementException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		HttpsURLConnection.setDefaultSSLSocketFactory(getSocketFactoryOf(ca));
	}

	/**
	 * Create a new instance of java.security.cert.Certificate by giving its type and its certificate encoded
	 * as a String.
	 * @param ca the certificate to trust
	 * @param type the type of certificate to generate
	 * @return the instance of a java.security.cert.Certificate
	 * @throws CertificateException
	 * @throws UnsupportedEncodingException
	 */
	public static Certificate getCA(String ca, String type) throws CertificateException, UnsupportedEncodingException {
		return getCA(ca.getBytes("UTF-8"), type);
	}

	/**
	 * Like {@link #getCA(String, String)}, but using the default type "X.509".
	 * @param ca the certificate to trust
	 * @return the instance of a java.security.cert.Certificate
	 * @throws CertificateException
	 * @throws UnsupportedEncodingException
	 */
	public static Certificate getCA(String ca) throws CertificateException, UnsupportedEncodingException {
		return getCA(ca, DEF_TYPE);
	}

	/**
	 * Create a new instance of java.security.cert.Certificate by giving its type and its certificate encoded
	 * as a byte array.
	 * @param ca the certificate to trust
	 * @param type the type of certificate to generate
	 * @return the instance of generated java.security.cert.Certificate
	 * @throws CertificateException
	 */
	public static Certificate getCA(byte[] ca, String type) throws CertificateException {
		return CertificateFactory.getInstance(type).generateCertificate(new ByteArrayInputStream(ca));
	}

	/**
	 * Like {@link #getCA(byte[], String)}, but using the default type "X.509".
	 * @param ca the certificate to trust
	 * @return the instance of generated java.security.cert.Certificate
	 * @throws CertificateException
	 */
	public static Certificate getCA(byte[] ca) throws CertificateException {
		return getCA(ca, DEF_TYPE);
	}

	/**
	 * Create a new instance of java.security.cert.Certificate by giving its type and its certificate encoded
	 * readable by the specified java.io.InputStream.
	 * @param ca the java.io.InputStream where reading the certificate to trust
	 * @param type the type of certificate to generate
	 * @return the instance of generated java.security.cert.Certificate
	 * @throws CertificateException
	 */
	public static Certificate getCA(InputStream ca, String type) throws CertificateException {
		return CertificateFactory.getInstance(type).generateCertificate(ca);
	}

	/**
	 * Like {@link #getCA(InputStream, String)}, but using the default type "X.509".
	 * @param ca the java.ioInputStream where reading the certificate to trust
	 * @return the instance of generated java.security.cert.Certificate
	 * @throws CertificateException
	 */
	public static Certificate getCA(InputStream ca) throws CertificateException {
		return getCA(ca, DEF_TYPE);
	}

	/**
	 * Create a new instance of javax.net.ssl.TrustManagerFactory for the specified certificate and entry.
	 * @param ca the certificate to trust
	 * @param entry the certificate entry
	 * @return the trust manager factory related to the specified certificate
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static TrustManagerFactory getTrustManagerFactoryOf(Certificate ca, String entry) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setCertificateEntry(entry, ca);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		return tmf;
	}

	/**
	 * Create a new instance of javax.net.ssl.TrustManagerFactory for the specified certificate authority.
	 * @param ca the certificate to trust
	 * @return the trust manager factory related to the specified certificate authority
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static TrustManagerFactory getTrustManagerFactoryOf(Certificate ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		return getTrustManagerFactoryOf(ca, "ca");
	}

	/**
	 * Create a new instance of javax.net.ssl.SSLSocketFactory will allow safe connections with servers are using
	 * the specified certificate authority.
	 * @param ca the certificate to trust
	 * @return the SSL socket socket factory allows safe connections using <tt>ca</tt>
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static SSLSocketFactory getSocketFactoryOf(Certificate ca) throws KeyManagementException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		TrustManagerFactory tmf = getTrustManagerFactoryOf(ca);
		SSLContext ctx = SSLContext.getInstance(DEF_SSL_PROTO);
		ctx.init(null, tmf.getTrustManagers(), null);
		return ctx.getSocketFactory();
	}

	private static final String DEF_SSL_PROTO = "TLS";
	private static final String DEF_TYPE = "X.509";
	private static final String DEF_CERT =
		"-----BEGIN CERTIFICATE-----\n"+
		"MIIFNjCCBB6gAwIBAgIDEbm1MA0GCSqGSIb3DQEBBQUAMDwxCzAJBgNVBAYTAlVT\n"+
		"MRcwFQYDVQQKEw5HZW9UcnVzdCwgSW5jLjEUMBIGA1UEAxMLUmFwaWRTU0wgQ0Ew\n"+
		"HhcNMTQwNDA2MDcxNTM2WhcNMTUwNDA5MDgxMTM4WjCBvzEpMCcGA1UEBRMgclNB\n"+
		"Y1JkMVF6LVpUeHhteDN5V2J2Ni9vd0xUT1BLSUsxEzARBgNVBAsTCkdUNzcwNTg2\n"+
		"MDMxMTAvBgNVBAsTKFNlZSB3d3cucmFwaWRzc2wuY29tL3Jlc291cmNlcy9jcHMg\n"+
		"KGMpMTQxLzAtBgNVBAsTJkRvbWFpbiBDb250cm9sIFZhbGlkYXRlZCAtIFJhcGlk\n"+
		"U1NMKFIpMRkwFwYDVQQDDBAqLmNsb3VkcGx1Z3MuY29tMIIBIjANBgkqhkiG9w0B\n"+
		"AQEFAAOCAQ8AMIIBCgKCAQEAnH/whqKrZZ6heDspfSCi7diRdWCqMbio2CIkJdJs\n"+
		"qlRXCvnjurQazJEF5Gn/kCgg0j2AgREaI387dverx3euVvDfQxUn5UPq5Ln5YZ3H\n"+
		"h+g5zbp2QN7K5tq8/zA4infbCeLOxBD8oEpVxPhaupZJSAEfF5LWPH9kIceCiAjP\n"+
		"B4vgfjIksawdgHTDDQgfFOuYdNpuR4KN1pSzgQ0hXaNhpeIUbb7tYjHbx3bqaNHx\n"+
		"DGrL8qXHLYRI2In5uMDeOhtajplQs0qPeRXUPo+dHmuO8ZpbSQHmZPrYnTiblwXK\n"+
		"7pkLE33nZzkcJJ1xpfkyoYj3wphNbuU01+oFUwdCqrKTEQIDAQABo4IBuzCCAbcw\n"+
		"HwYDVR0jBBgwFoAUa2k9ahhCSt2PAmU5/TUkhniRFjAwDgYDVR0PAQH/BAQDAgWg\n"+
		"MB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjArBgNVHREEJDAighAqLmNs\n"+
		"b3VkcGx1Z3MuY29tgg5jbG91ZHBsdWdzLmNvbTBDBgNVHR8EPDA6MDigNqA0hjJo\n"+
		"dHRwOi8vcmFwaWRzc2wtY3JsLmdlb3RydXN0LmNvbS9jcmxzL3JhcGlkc3NsLmNy\n"+
		"bDAdBgNVHQ4EFgQUfrVoAMPYOBYViTpKudvm4cTQEMowDAYDVR0TAQH/BAIwADB4\n"+
		"BggrBgEFBQcBAQRsMGowLQYIKwYBBQUHMAGGIWh0dHA6Ly9yYXBpZHNzbC1vY3Nw\n"+
		"Lmdlb3RydXN0LmNvbTA5BggrBgEFBQcwAoYtaHR0cDovL3JhcGlkc3NsLWFpYS5n\n"+
		"ZW90cnVzdC5jb20vcmFwaWRzc2wuY3J0MEwGA1UdIARFMEMwQQYKYIZIAYb4RQEH\n"+
		"NjAzMDEGCCsGAQUFBwIBFiVodHRwOi8vd3d3Lmdlb3RydXN0LmNvbS9yZXNvdXJj\n"+
		"ZXMvY3BzMA0GCSqGSIb3DQEBBQUAA4IBAQCN+Vc2a3x0pkTlBlnj8Pm5GLBMIfaH\n"+
		"r15hALLXcV84qi0hi3TYxvCwDtbsu/25g6aT+O4oM6HVM5hFwh504fip0g/j4PQG\n"+
		"oCwLue25ZwneNovtnbS0L2sQ1bnRHLSb0A17e6XNqpShGTveMuCazzonn1NPojgK\n"+
		"hTiNiXmTIl6Al7gjc09r8kvzt/FscCABrgqstxlCrBFEIsMYeqgAQBSUKvK8HJ5f\n"+
		"kd1ikKofvqReYjkE2DnuOzUAJsIHgB173jE6kYmQc6R2t+lzhyuFb2vnl0CEFBYb\n"+
		"fpPmrG8+km3xnLatd6hDxzFoZjYzMpcAUOHn+HlBmXzJmNergwX6uvGv\n"+
		"-----END CERTIFICATE-----";
}

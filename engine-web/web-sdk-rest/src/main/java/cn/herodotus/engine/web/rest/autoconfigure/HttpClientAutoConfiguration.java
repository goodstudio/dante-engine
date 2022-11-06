/*
 * Copyright (c) 2020-2030 ZHENGGENGWEI(码匠君)<herodotus@aliyun.com>
 *
 * Dante Engine Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Dante Engine 采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改 Dante Engine 源码头部的版权声明。
 * 3.请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://gitee.com/herodotus/dante-engine
 * 5.在修改包名，模块名称，项目代码等时，请注明软件出处 https://gitee.com/herodotus/dante-engine
 * 6.若您的项目无法满足以上几点，可申请商业授权
 */

package cn.herodotus.engine.web.rest.autoconfigure;

import cn.herodotus.engine.web.rest.annotation.ConditionalOnFeignUseHttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: HttpClient 自动配置 </p>
 *
 * @author : gengwei.zheng
 * @date : 2022/5/29 18:46
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnFeignUseHttpClient
public class HttpClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OkHttpAutoConfiguration.class);

    @PostConstruct
    public void postConstruct() {
        log.debug("[Herodotus] |- SDK [Engine Web HttpClient] Auto Configure.");
    }

    private CloseableHttpClient httpClient5;

    @Bean
    @ConditionalOnMissingBean({HttpClientConnectionManager.class})
    public HttpClientConnectionManager hc5ConnectionManager(FeignHttpClientProperties httpClientProperties) {
        return PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(this.httpsSSLConnectionSocketFactory(httpClientProperties.isDisableSslValidation())).setMaxConnTotal(httpClientProperties.getMaxConnections()).setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute()).setConnPoolPolicy(PoolReusePolicy.valueOf(httpClientProperties.getHc5().getPoolReusePolicy().name())).setPoolConcurrencyPolicy(PoolConcurrencyPolicy.valueOf(httpClientProperties.getHc5().getPoolConcurrencyPolicy().name())).setConnectionTimeToLive(TimeValue.of(httpClientProperties.getTimeToLive(), httpClientProperties.getTimeToLiveUnit())).setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.of((long) httpClientProperties.getHc5().getSocketTimeout(), httpClientProperties.getHc5().getSocketTimeoutUnit())).build()).build();
    }

    @Bean
    public CloseableHttpClient httpClient5(HttpClientConnectionManager connectionManager, FeignHttpClientProperties httpClientProperties) {
        this.httpClient5 = HttpClients.custom()
                .disableCookieManagement()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
                        .setRedirectsEnabled(httpClientProperties.isFollowRedirects())
                        .build())
                .build();
        return this.httpClient5;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        log.trace("[Herodotus] |- Bean [Client Http Request Factory for HttpClient] Auto Configure.");
        return httpComponentsClientHttpRequestFactory;
    }

    @PreDestroy
    public void destroy() {
        if (this.httpClient5 != null) {
            this.httpClient5.close(CloseMode.GRACEFUL);
        }
    }

    private LayeredConnectionSocketFactory httpsSSLConnectionSocketFactory(boolean isDisableSslValidation) {
        SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder.create().setTlsVersions(new TLS[]{TLS.V_1_3, TLS.V_1_2});
        if (isDisableSslValidation) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{new HttpClientAutoConfiguration.DisabledValidationTrustManager()}, new SecureRandom());
                sslConnectionSocketFactoryBuilder.setSslContext(sslContext);
            } catch (NoSuchAlgorithmException e) {
                log.warn("[Herodotus] |- Error creating SSLContext for AlgorithmException", e);
            } catch (KeyManagementException e) {
                log.warn("[Herodotus] |- Error creating SSLContext for KeyManagementException", e);
            }
        } else {
            sslConnectionSocketFactoryBuilder.setSslContext(SSLContexts.createSystemDefault());
        }

        return sslConnectionSocketFactoryBuilder.build();
    }

    static class DisabledValidationTrustManager implements X509TrustManager {
        DisabledValidationTrustManager() {
        }

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}

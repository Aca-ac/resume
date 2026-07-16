package com.resume.module.job.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlCheckerTest {

    private static HttpServer server;
    private static String baseUrl;
    private final UrlChecker urlChecker = new UrlChecker();

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });

        server.createContext("/head-405-get-200", exchange -> {
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
            } else {
                exchange.sendResponseHeaders(200, 0);
            }
            exchange.close();
        });

        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", baseUrl + "/ok");
            exchange.sendResponseHeaders(301, 0);
            exchange.close();
        });

        server.createContext("/not-found", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
        });

        server.createContext("/server-error", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });

        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void isUrlAccessible_returns200_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible(baseUrl + "/ok"));
    }

    @Test
    void isUrlAccessible_head405Get200_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible(baseUrl + "/head-405-get-200"));
    }

    @Test
    void isUrlAccessible_redirect301_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible(baseUrl + "/redirect"));
    }

    @Test
    void isUrlAccessible_404_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible(baseUrl + "/not-found"));
    }

    @Test
    void isUrlAccessible_500_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible(baseUrl + "/server-error"));
    }

    @Test
    void isUrlAccessible_nullUrl_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible(null));
    }

    @Test
    void isUrlAccessible_emptyUrl_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible(""));
    }

    @Test
    void isUrlAccessible_blankUrl_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible("   "));
    }

    @Test
    void isUrlAccessible_invalidFormatUrl_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible("not-a-valid-url"));
    }

    @Test
    void isUrlAccessible_timeoutUrl_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible("http://10.255.255.1:81/", 1000));
    }

    @Test
    void isUrlAccessible_withCustomTimeout_validUrl_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible(baseUrl + "/ok", 10000));
    }

    @Test
    void isUrlAccessible_invalidProtocol_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible("ftp://www.example.com/invalid"));
    }

    @Tag("network")
    @Test
    void isUrlAccessible_validLiepinUrl_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible("https://www.liepin.com/job/1982865569.shtml"));
    }

    @Tag("network")
    @Test
    void isUrlAccessible_validZhipinUrl_returnsTrue() {
        assertTrue(urlChecker.isUrlAccessible("https://www.zhipin.com/job_detail/ba209e341fa6f2d90nZ73t61FlJW.html"));
    }

    @Tag("network")
    @Test
    void isUrlAccessible_invalidDomain_returnsFalse() {
        assertFalse(urlChecker.isUrlAccessible("https://www.xyz123invalid.com/nonexistent.html"));
    }
}
package com.openhtmltopdf.swing;

import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.openhtmltopdf.extend.FSUriResolver;

public class DefaultUriResolverTest {
    
    private FSUriResolver resolver = new NaiveUserAgent.DefaultUriResolver();
    
    @Test
    public void testJarHttpUrlResolve() {
        Assert.assertThat(resolver.resolveURI("jar:http://www.foo.com/bar/baz.jar!/", "logo.png"), equalTo("jar:http://www.foo.com/bar/baz.jar!/logo.png"));
    }

    @Test
    public void testJarFileUrlResolve() {
        Assert.assertThat(resolver.resolveURI("jar:file:/E:/example/target/server-1.0.0.war!/WEB-INF/classes!/export/", "logo.jpg"), equalTo("jar:file:/E:/example/target/server-1.0.0.war!/WEB-INF/classes!/export/logo.jpg"));
    }
    
    @Test
    public void testAbsoluteJarFileAsRelativePart() {
        Assert.assertThat(resolver.resolveURI("http://example.com/", "jar:file:/E:/example/target/server-1.0.0.war!/WEB-INF/classes!/export/logo.jpg"), equalTo("jar:file:/E:/example/target/server-1.0.0.war!/WEB-INF/classes!/export/logo.jpg"));
    }
    
    @Test
    @Ignore("Base URIs not working with out a trailing slash")
    public void testNonJarUrlResolveWithoutTrailingSlash() {
    	Assert.assertThat(resolver.resolveURI("http://example.com", "logo.png"), equalTo("http://example.com/logo.png"));
    }
 
    @Test
    public void testNonJarUrlResolveWithTrailingSlash() {
        Assert.assertThat(resolver.resolveURI("http://example.com/", "logo.png"), equalTo("http://example.com/logo.png"));
    }
    
    @Test
    public void testNonJarWithPath() {
        Assert.assertThat(resolver.resolveURI("http://example.com/path/", "logo.png"), equalTo("http://example.com/path/logo.png"));
    }
    
    @Test
    public void testAbsoluteAsPartialUrl() {
        Assert.assertThat(resolver.resolveURI("http://example.com/", "http://sample.com/logo.png"), equalTo("http://sample.com/logo.png"));
    }
    
    @Test
    @Ignore("Should empty urls should resolve to the base rather than null?")
    public void testEmptyUrl() {
        Assert.assertThat(resolver.resolveURI("http://example.com/", ""), equalTo("http://example.com/"));
    }
    
    
}

package org.freya.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class MockWebAppContext extends XmlWebApplicationContext {

    public MockWebAppContext(String webAppRootDir, String servletName, String... configLocations) throws Exception {
        init(webAppRootDir, servletName, configLocations);
    }

    private void init(String webAppRootDir, String servletName, String... configLocations) throws Exception {
        MockServletContext servletContext = new MockServletContext(webAppRootDir, new FileSystemResourceLoader());
        MockServletConfig servletConfig = new MockServletConfig(servletContext, servletName);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this);

        DispatcherServlet dispatcherServlet = new DispatcherServlet();

        setServletConfig(servletConfig);
        setConfigLocations(configLocations);
        addBeanFactoryPostProcessor(new MockBeanFactoryPostProcessor(servletName, dispatcherServlet));
        addApplicationListener(new SourceFilteringListener(this, new ContextRefreshedEventListener(dispatcherServlet)));
        registerShutdownHook();
        refresh();

        dispatcherServlet.init(servletConfig);
    }

    private final class ContextRefreshedEventListener implements ApplicationListener<ContextRefreshedEvent> {

        private final DispatcherServlet dispatcherServlet;

        private ContextRefreshedEventListener(DispatcherServlet dispatcherServlet) {
            this.dispatcherServlet = dispatcherServlet;
        }


        public void onApplicationEvent(ContextRefreshedEvent event) {
            dispatcherServlet.onApplicationEvent(event);
        }
    }

    private final class MockBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

        private final String servletName;
        private final DispatcherServlet dispatcherServlet;

        private MockBeanFactoryPostProcessor(String servletName, DispatcherServlet dispatcherServlet) {
            this.servletName = servletName;
            this.dispatcherServlet = dispatcherServlet;
        }

        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            beanFactory.registerSingleton(servletName, dispatcherServlet);
        }
    }
}

package com.liferay.commerce.frontend.taglib.custom.internal.servlet;

import org.osgi.service.component.annotations.Reference;
import javax.servlet.ServletContext;
import org.osgi.service.component.annotations.Component;

/**
 * @author Roselaine Marques
 */
@Component(immediate = true, service = {})
public class ServletContextUtil {

    private static ServletContext _servletContext;
    @Reference(
            target = "(osgi.web.symbolicname=commerce.frontend.taglib.custom)", unbind = "-"
    )
    protected void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;
    }

    public static ServletContext getServletContext() {
        return _servletContext;
    }
}


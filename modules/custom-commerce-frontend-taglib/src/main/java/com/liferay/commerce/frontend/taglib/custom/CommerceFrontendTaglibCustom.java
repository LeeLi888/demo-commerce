package com.liferay.commerce.frontend.taglib.custom;

import com.liferay.taglib.util.IncludeTag;
import com.liferay.commerce.frontend.taglib.custom.internal.servlet.ServletContextUtil;
import javax.servlet.jsp.PageContext;

/**
 * @author Roselaine Marques
 */
public class CommerceFrontendTaglibCustom extends IncludeTag {
    @Override
    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);

        setServletContext(ServletContextUtil.getServletContext());
    }

    @Override
    protected String getPage() {
        return _PAGE;
    }

    private static final String _PAGE = "/mini-cart/page.jsp";
}
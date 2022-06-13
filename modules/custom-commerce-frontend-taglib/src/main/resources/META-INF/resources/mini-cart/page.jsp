<%@ include file="../init.jsp" %>
<%@ page import="com.liferay.portal.kernel.util.HashMapBuilder" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="liferay-commerce-ui" uri="http://liferay.com/tld/commerce-ui" %>

<%
    Map<String, String> overrideViews = HashMapBuilder
//            .put("Item", "commerce-frontend-taglib-custom@1.0.0/js/OverriddenCartItem")
            .put("ItemsListActions", "commerce-frontend-taglib-custom@1.0.0/js/OverrideCartItemsListActions")
            .put("OrderButton", "commerce-frontend-taglib-custom@1.0.0/js/OverrideOrderButton")
            .build();
%>
<liferay-commerce-ui:mini-cart views="<%=overrideViews%>" />

<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="javax.portlet.MutableRenderParameters" %>
<%@ include file="/init.jsp" %>
<%@ include file="/init-custom.jsp" %>

<style>
    .custom-quote-history-container {
        color: #5c5e5e;
        font-size: 14px;
        line-height: 1.58;
        margin: auto;
        max-width: 1440px;
        padding: 40px;
        padding-bottom: 104px;
        width: 100%;
        padding: 20px 30px;
    }

    .custom-quote-history-header {
        margin: 0px;
        padding: 20px;
        background-color: #fff;
        font-size: 25px;
        font-weight: bold;
        border-color: #e7e7ed;
        border-style: solid;
        border-width: 0.0625rem;
        border-radius: 0.25rem;
    }
</style>

<%
    PortletURL iteratorURL = renderResponse.createRenderURL();
    MutableRenderParameters mutableRenderParameters = iteratorURL.getRenderParameters();
    mutableRenderParameters.setValue("mvcRenderCommandName", ParamUtil.getString(renderRequest, "mvcRenderCommandName"));
    mutableRenderParameters.setValue("commerceOrderId", ParamUtil.getString(renderRequest, "commerceOrderId"));
    mutableRenderParameters.setValue("accountId", ParamUtil.getString(renderRequest, "accountId"));
%>

<div class="custom-quote-history-container">

    <div class="custom-quote-history-header align-items-center row">
        <div class="col-md-6">
            <dl class="commerce-list">
                <dt>
                    Account ID
                </dt>
                <dd>${accountId}</dd>
            </dl>
        </div>

        <div class="col-md-6">
            <dl class="commerce-list">
                <dt>
                    Order Id
                </dt>
                <dd>${commerceOrderId}</dd>
            </dl>
        </div>
    </div>

    <%-- Search container. --%>
    <liferay-ui:search-container
            emptyResultsMessage="no-assignments"
            id="entries"
            delta="10"
            iteratorURL="<%=iteratorURL%>"
            total="${count}">

        <liferay-ui:search-container-results results="${entries}" />

        <liferay-ui:search-container-row
                className="com.liferay.object.model.ObjectEntry"
                modelVar="entry">

            <%@ include file="entry_columns.jspf" %>

        </liferay-ui:search-container-row>

        <%-- Iterator / Paging --%>
        <liferay-ui:search-iterator
                paginate="true"
            markupView="lexicon" />

    </liferay-ui:search-container>

</div>
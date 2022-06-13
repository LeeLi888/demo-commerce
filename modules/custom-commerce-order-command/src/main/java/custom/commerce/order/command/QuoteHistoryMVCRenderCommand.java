package custom.commerce.order.command;

import com.liferay.commerce.constants.CommercePortletKeys;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.service.CommerceOrderService;
import com.liferay.object.admin.rest.resource.v1_0.ObjectRelationshipResource;
import com.liferay.object.internal.petra.sql.dsl.DynamicObjectDefinitionTable;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectEntryService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.object.web.internal.display.context.helper.ObjectRequestHelper;
import com.liferay.object.web.internal.object.definitions.display.context.ViewObjectDefinitionsDisplayContext;
import com.liferay.object.web.internal.object.entries.display.context.ViewObjectEntriesDisplayContext;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.petra.sql.dsl.expression.Expression;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.petra.sql.dsl.spi.query.OrderBy;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchContextFactory;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.search.filter.QueryFilter;
import com.liferay.portal.kernel.search.filter.TermFilter;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.vulcan.aggregation.Aggregation;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.resource.OpenAPIResource;
import custom.commerce.order.constants.CustomCommerceOrderConstans;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;

@Component(
	immediate = true,
	property = {
			"javax.portlet.name=" + CommercePortletKeys.COMMERCE_OPEN_ORDER_CONTENT,
			"mvc.command.name=/commerce_order_content_custom/quote_history"
	},
	service = MVCRenderCommand.class
)
public class QuoteHistoryMVCRenderCommand implements MVCRenderCommand {

	@Override
	public String render(
			RenderRequest renderRequest, RenderResponse renderResponse) {

		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long companyId = themeDisplay.getCompanyId();
		long commerceOrderId = ParamUtil.getLong(renderRequest, "commerceOrderId");
		long accountId = ParamUtil.getLong(renderRequest, "accountId");

		int currentPage = ParamUtil.getInteger(renderRequest, SearchContainer.DEFAULT_CUR_PARAM, SearchContainer.DEFAULT_CUR);
		int delta = ParamUtil.getInteger(renderRequest, SearchContainer.DEFAULT_DELTA_PARAM, SearchContainer.DEFAULT_DELTA);
		int start = ((currentPage > 0) ? (currentPage - 1) : 0) * delta;
		int end = start + delta;

		try {
			CommerceOrder commerceOrder = _commerceOrderService.getCommerceOrder(commerceOrderId);
			ObjectDefinition objectDefinition = _objectDefinitionLocalService.fetchObjectDefinition(companyId, CustomCommerceOrderConstans.QUOTE_HISTORY_OBJECT_DEFINITION_NAME);

			DynamicQuery query = _objectEntryLocalService.dynamicQuery()
					.add(RestrictionsFactoryUtil.eq("externalReferenceCode", String.valueOf(commerceOrder.getCommerceOrderId())))
					.addOrder(OrderFactoryUtil.desc("createDate"));
			List<ObjectEntry> list = _objectEntryLocalService.dynamicQuery(query, start, end);

			DynamicQuery queryForCount = _objectEntryLocalService.dynamicQuery()
					.add(RestrictionsFactoryUtil.eq("externalReferenceCode", String.valueOf(commerceOrder.getCommerceOrderId())));
			long count = _objectEntryLocalService.dynamicQueryCount(queryForCount);

			renderRequest.setAttribute("accountId", accountId);
			renderRequest.setAttribute("commerceOrderId", commerceOrderId);
			renderRequest.setAttribute("entries", list);
			renderRequest.setAttribute("count", count);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "/quote_history/view.jsp";
	}


/*
	private Page<ObjectEntry> _search(long companyId, RenderRequest renderRequest, ObjectDefinition objectDefinition, CommerceOrder commerceOrder) throws Exception {
		int currentPage = ParamUtil.getInteger(renderRequest, SearchContainer.DEFAULT_CUR_PARAM, SearchContainer.DEFAULT_CUR);
		int delta = ParamUtil.getInteger(renderRequest, SearchContainer.DEFAULT_DELTA_PARAM, SearchContainer.DEFAULT_DELTA);

		String scopeKey = objectDefinition.getScope();
		Aggregation aggregation = null;
		DTOConverterContext dtoConverterContext = _getDTOConverterContext(commerceOrder);
		//put orderId into externalReferenceCode for search
		Filter filter = new TermFilter("externalReferenceCode", String.valueOf(commerceOrder.getCommerceOrderId()));
		Pagination pagination = Pagination.of(currentPage, delta);
		String search = null;
		Sort[] sorts = null;

		Page<ObjectEntry> page = _objectEntryManager.getObjectEntries(companyId, objectDefinition, scopeKey, aggregation, dtoConverterContext, filter, pagination, search, sorts);

		return page;
	}
*/

	private DTOConverterContext _getDTOConverterContext(CommerceOrder commerceOrder) {
		return new DefaultDTOConverterContext(
				_dtoConverterRegistry,
				commerceOrder.getCommerceOrderId(),
				LocaleUtil.getSiteDefault(), null, null);
	}
	private String _getApiURL(ObjectDefinition objectDefinition) {
		return "/o" + objectDefinition.getRESTContextPath();
	}

	private void _searchQuoteList(long orderId, long objectDefinitionId, int start, int end) throws PortalException {
		ObjectDefinition objectDefinition = _objectDefinitionLocalService.getObjectDefinition(objectDefinitionId);



		DynamicObjectDefinitionTable dynamicObjectDefinitionTable = new DynamicObjectDefinitionTable(
				objectDefinition,
				_objectFieldLocalService.getObjectFields(
						objectDefinitionId, objectDefinition.getDBTableName()),
				objectDefinition.getDBTableName());

		DSLQuery dslQuery = DSLQueryFactoryUtil.selectDistinct(
				dynamicObjectDefinitionTable.getSelectExpressions()
		).from(
				dynamicObjectDefinitionTable
		).where(
				dynamicObjectDefinitionTable.getColumn("orderId", Long.class)
						.eq(orderId)
		).limit(start, end);
	}

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

	@Reference
	private ObjectEntryService _objectEntryService;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private ObjectEntryManager _objectEntryManager;

	@Reference
	private ModelResourcePermission<ObjectDefinition>
			_objectDefinitionModelResourcePermission;

	@Reference
	private OpenAPIResource _openAPIResource;

	@Reference
	private CommerceOrderService _commerceOrderService;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

}
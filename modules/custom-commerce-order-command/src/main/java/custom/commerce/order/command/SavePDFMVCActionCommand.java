package custom.commerce.order.command;

import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.account.service.CommerceAccountLocalService;
import com.liferay.commerce.constants.CommercePortletKeys;
import com.liferay.commerce.currency.util.CommercePriceFormatter;
import com.liferay.commerce.model.CommerceAddress;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.commerce.model.CommerceOrderType;
import com.liferay.commerce.product.service.CommerceChannelService;
import com.liferay.commerce.report.exporter.CommerceReportExporter;
import com.liferay.commerce.service.CommerceOrderService;
import com.liferay.commerce.service.CommerceOrderTypeService;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.document.library.kernel.util.DLValidator;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Country;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Region;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.RepositoryProvider;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.OpenSearchRegistryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.upload.UniqueFileNameProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;

import custom.commerce.order.constants.CustomCommerceOrderConstans;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true,
		property = {
			"javax.portlet.name=" + CommercePortletKeys.COMMERCE_OPEN_ORDER_CONTENT,
			"mvc.command.name=/commerce_order_content_custom/archive_pdf"
		},
		service = MVCActionCommand.class)
public class SavePDFMVCActionCommand extends BaseMVCActionCommand {

	@Override
	public void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long companyId = themeDisplay.getCompanyId();
		long orderId = ParamUtil.getLong(actionRequest, "commerceOrderId");
		long accountId = ParamUtil.getLong(actionRequest, "accountId");

		ServiceContext serviceContext = null;
		try {
			serviceContext = ServiceContextFactory.getInstance(actionRequest);

			CommerceAccount account = accountLocalService.getCommerceAccount(accountId);
			CommerceOrder commerceOrder = _commerceOrderService.getCommerceOrder(orderId);
			byte[] bytes = getFile(commerceOrder, themeDisplay, serviceContext);
			InputStream inStream = new ByteArrayInputStream(bytes);
			UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(actionRequest);
			long quotesFolderId= createFolderIfDoesNotExist("Quotes", themeDisplay.getScopeGroupId(),
					DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, actionRequest,false);
			long accountFolderId= createFolderIfDoesNotExist(account.getName(), themeDisplay.getScopeGroupId(),
					quotesFolderId, actionRequest,true);
			
			long orderFolderId= createFolderIfDoesNotExist(String.valueOf(orderId),themeDisplay.getScopeGroupId(),
					accountFolderId, actionRequest,false);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm");
			String filename = sdf.format(new Date())+"-"+orderId+ ".pdf";

			long size = bytes.length;
			FileEntry fileEntry = _addFileEntry(filename, orderFolderId, inStream, size, ContentTypes.APPLICATION_PDF, uploadPortletRequest,
					themeDisplay, ServiceContextFactory.getInstance(DLFileEntry.class.getName(), uploadPortletRequest));

			//save to quote history
			long scopeGroupId = 0; //themeDisplay.getScopeGroupId()
			_saveQuoteHistory(themeDisplay.getUserId(), companyId, scopeGroupId, commerceOrder, fileEntry, serviceContext);

//			String fileDownloadURL = _generateFileDownloadURL(fileEntry);

			String redirectURL = ParamUtil.getString(actionRequest, "redirect");
			redirectURL += "?fileEntryId=" + fileEntry.getFileEntryId();

			super.sendRedirect(actionRequest, actionResponse, redirectURL);

		} catch (Throwable e) {
			e.printStackTrace();
			SessionErrors.add(actionRequest, e.getClass(), e.getMessage());
		}
	}

//	private String _generateFileDownloadURL(FileEntry fileEntry) {
//		String url = String.format("/documents/%d/%d/%s/%s?t=%d&download=true",
//				fileEntry.getRepositoryId(),
//				fileEntry.getFolderId(),
//				fileEntry.getFileName(),
//				fileEntry.getUuid(),
//				fileEntry.getModifiedDate().getTime()
//		);
//		return url;
//	}

	private FileEntry _addFileEntry(String fileName, long folderId, InputStream inputStream, long size, String contentType,
			UploadPortletRequest uploadPortletRequest, ThemeDisplay themeDisplay, ServiceContext serviceContext)
			throws PortalException {

		//String  = uploadPortletRequest.getContentType(parameterName);

		_dlValidator.validateFileSize(themeDisplay.getScopeGroupId(), fileName,
				contentType, size);

		String uniqueFileName = _uniqueFileNameProvider.provide(fileName,
				curFileName -> _exists(themeDisplay.getScopeGroupId(), folderId, curFileName));

		//long size = uploadPortletRequest.getSize(parameterName);

		return _dlAppService.addFileEntry(null, themeDisplay.getScopeGroupId(), folderId, uniqueFileName,
				contentType, uniqueFileName, uniqueFileName, fileName,
				StringPool.BLANK, inputStream, size, null, null, serviceContext);
	}

	private boolean _exists(long groupId, long folderId, String fileName) {
		try {
			FileEntry fileEntry = _dlAppService.getFileEntryByFileName(groupId, folderId, fileName);

			if (fileEntry != null) {
				return true;
			}

			return false;
		} catch (PortalException portalException) {

			return false;
		}
	}


	private byte[] getFile(CommerceOrder commerceOrder, ThemeDisplay themeDisplay,  ServiceContext serviceContext) throws Exception {

		//CommerceOrder commerceOrder = _commerceOrderService.getCommerceOrder(orderId);

		CommerceAddress billingAddress = commerceOrder.getBillingAddress();
		CommerceAddress shippingAddress = commerceOrder.getShippingAddress();

		HashMapBuilder.HashMapWrapper<String, Object> hashMapWrapper = new HashMapBuilder.HashMapWrapper<>();

		CommerceAccount commerceAccount = commerceOrder.getCommerceAccount();

		if (billingAddress != null) {
			hashMapWrapper.put("billingAddressCity", billingAddress.getCity()).put("billingAddressCountry", () -> {
				Country country = billingAddress.getCountry();

				if (country == null) {
					return StringPool.BLANK;
				}

				return country.getName(themeDisplay.getLocale());
			}).put("billingAddressName", billingAddress.getName())
					.put("billingAddressPhoneNumber", billingAddress.getPhoneNumber())
					.put("billingAddressRegion", () -> {
						Region region = billingAddress.getRegion();

						if (region == null) {
							return StringPool.BLANK;
						}

						return region.getName();
					}).put("billingAddressStreet1", billingAddress.getStreet1())
					.put("billingAddressStreet2", billingAddress.getStreet2())
					.put("billingAddressStreet3", billingAddress.getStreet3())
					.put("billingAddressZip", billingAddress.getZip());
		}

		List<CommerceOrderItem> commerceOrderItems = commerceOrder.getCommerceOrderItems();

		hashMapWrapper.put("commerceAccountName", commerceAccount.getName())
				.put("commerceOrderId", commerceOrder.getCommerceOrderId())
				.put("commerceOrderItemsSize", commerceOrderItems.size()).put("commerceOrderType", () -> {
					CommerceOrderType commerceOrderType = _commerceOrderTypeService
							.fetchCommerceOrderType(commerceOrder.getCommerceOrderTypeId());

					if (commerceOrderType == null) {
						return StringPool.BLANK;
					}

					return commerceOrderType.getName(themeDisplay.getLanguageId());
				}).put("companyId", commerceAccount.getCompanyId())
				.put("externalReferenceCode",
						(commerceOrder.getExternalReferenceCode() != null) ? commerceOrder.getExternalReferenceCode()
								: StringPool.BLANK)
				.put("locale", themeDisplay.getLocale()).put("logoURL", _getLogoURL(themeDisplay))
				.put("orderDate", (commerceOrder.getOrderDate() == null) ? null : commerceOrder.getOrderDate())
				.put("printedNote",
						(commerceOrder.getPrintedNote() == null) ? StringPool.BLANK : commerceOrder.getPrintedNote())
				.put("purchaseOrderNumber", commerceOrder.getPurchaseOrderNumber()).put("requestedDeliveryDate", () -> {
					if (commerceOrder.getRequestedDeliveryDate() == null) {
						return null;
					}

					Format format = FastDateFormatFactoryUtil.getDate(themeDisplay.getLocale(),
							themeDisplay.getTimeZone());

					return format.format(commerceOrder.getRequestedDeliveryDate());
				});

		if (shippingAddress != null) {
			hashMapWrapper.put("shippingAddressCity", shippingAddress.getCity()).put("shippingAddressCountry", () -> {
				Country country = shippingAddress.getCountry();

				if (country == null) {
					return StringPool.BLANK;
				}

				return country.getName(themeDisplay.getLocale());
			}).put("shippingAmountMoney", commerceOrder.getShippingMoney())
					.put("shippingAddressName", shippingAddress.getName())
					.put("shippingAddressPhoneNumber", shippingAddress.getPhoneNumber())
					.put("shippingAddressRegion", () -> {
						Region region = shippingAddress.getRegion();

						if (region == null) {
							return StringPool.BLANK;
						}

						return region.getName();
					}).put("shippingAddressStreet1", shippingAddress.getStreet1())
					.put("shippingAddressStreet2", shippingAddress.getStreet2())
					.put("shippingAddressStreet3", shippingAddress.getStreet3())
					.put("shippingAddressZip", shippingAddress.getZip())
					.put("shippingDiscountAmount", _commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
							commerceOrder.getShippingDiscountAmount(), themeDisplay.getLocale()));
		}

		hashMapWrapper
				.put("shippingAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingAmount(), themeDisplay.getLocale()))
				.put("shippingDiscountAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountAmount(), themeDisplay.getLocale()))
				.put("shippingDiscountPercentageLevel1", commerceOrder.getShippingDiscountPercentageLevel1())
				.put("shippingDiscountPercentageLevel1WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountPercentageLevel1WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("shippingDiscountPercentageLevel2", commerceOrder.getShippingDiscountPercentageLevel2())
				.put("shippingDiscountPercentageLevel2WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountPercentageLevel2WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("shippingDiscountPercentageLevel3", commerceOrder.getShippingDiscountPercentageLevel3())
				.put("shippingDiscountPercentageLevel3WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountPercentageLevel3WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("shippingDiscountPercentageLevel4", commerceOrder.getShippingDiscountPercentageLevel4())
				.put("shippingDiscountPercentageLevel4WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountPercentageLevel4WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("shippingDiscountWithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingDiscountWithTaxAmount(), themeDisplay.getLocale()))
				.put("shippingWithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getShippingWithTaxAmount(), themeDisplay.getLocale()))
				.put("subtotalDiscountAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountAmount(), themeDisplay.getLocale()))
				.put("subtotalDiscountPercentageLevel1", commerceOrder.getSubtotalDiscountPercentageLevel1())
				.put("subtotalDiscountPercentageLevel1WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountPercentageLevel1WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("subtotalDiscountPercentageLevel2", commerceOrder.getSubtotalDiscountPercentageLevel2())
				.put("subtotalDiscountPercentageLevel2WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountPercentageLevel2WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("subtotalDiscountPercentageLevel3", commerceOrder.getSubtotalDiscountPercentageLevel3())
				.put("subtotalDiscountPercentageLevel3WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountPercentageLevel3WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("subtotalDiscountPercentageLevel4", commerceOrder.getSubtotalDiscountPercentageLevel4())
				.put("subtotalDiscountPercentageLevel4WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountPercentageLevel4WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("subtotalDiscountWithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getSubtotalDiscountWithTaxAmount(), themeDisplay.getLocale()))
				.put("subtotalMoney", commerceOrder.getSubtotalMoney())
				.put("subtotalWithTaxAmountMoney", commerceOrder.getSubtotalWithTaxAmountMoney())
				.put("taxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTaxAmount(), themeDisplay.getLocale()))
				.put("totalDiscountAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountAmount(), themeDisplay.getLocale()))
				.put("totalDiscountPercentageLevel1", commerceOrder.getTotalDiscountPercentageLevel1())
				.put("totalDiscountPercentageLevel1WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountPercentageLevel1WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("totalDiscountPercentageLevel2", commerceOrder.getTotalDiscountPercentageLevel2())
				.put("totalDiscountPercentageLevel2WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountPercentageLevel2WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("totalDiscountPercentageLevel3", commerceOrder.getTotalDiscountPercentageLevel3())
				.put("totalDiscountPercentageLevel3WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountPercentageLevel3WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("totalDiscountPercentageLevel4", commerceOrder.getTotalDiscountPercentageLevel4())
				.put("totalDiscountPercentageLevel4WithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountPercentageLevel4WithTaxAmount(),
								themeDisplay.getLocale()))
				.put("totalDiscountWithTaxAmount",
						_commercePriceFormatter.format(commerceOrder.getCommerceCurrency(),
								commerceOrder.getTotalDiscountWithTaxAmount(), themeDisplay.getLocale()))
				.put("totalMoney", commerceOrder.getTotalMoney())
				.put("totalWithTaxAmountMoney", commerceOrder.getTotalWithTaxAmountMoney());

		FileEntry fileEntry = _dlAppLocalService.fetchFileEntryByExternalReferenceCode(commerceOrder.getGroupId(),
				"ORDER_PRINT_TEMPLATE");

		return _commerceReportExporter.export(commerceOrderItems, fileEntry, hashMapWrapper.build());
	}

	//save quote history to object
	private ObjectEntry _saveQuoteHistory(
			long userId,
			long companyId,
			long groupId,
			CommerceOrder commerceOrder,
			FileEntry fileEntry,
			ServiceContext serviceContext) throws PortalException {

		ObjectDefinition objectDefinition = _objectDefinitionLocalService.fetchObjectDefinition(companyId, CustomCommerceOrderConstans.QUOTE_HISTORY_OBJECT_DEFINITION_NAME);

		long objectDefinitionId = objectDefinition.getObjectDefinitionId();

		long quoteId = _counterLocalService.increment(ObjectEntry.class.getName() + "." + objectDefinitionId);
		long orderId = commerceOrder.getCommerceOrderId();

		Map<String, Serializable> value = new HashMap<>();
		value.put("quoteId", quoteId);
		value.put("orderId", orderId);
		value.put("currency", commerceOrder.getCommerceCurrency().getCommerceCurrencyId());
		value.put("total", commerceOrder.getTotal());
		value.put("totalWithTaxAmount", commerceOrder.getTotalWithTaxAmount());
		value.put("units", commerceOrder.getCommerceOrderItems().size());
		value.put("fileEntryId", fileEntry.getFileEntryId());

		//externalReferenceCode for search
		String externalReferenceCode = String.valueOf(orderId);

		ObjectEntry objectEntry = _objectEntryLocalService.addObjectEntry(
				userId, groupId, objectDefinitionId,
				value, serviceContext);

		objectEntry.setExternalReferenceCode(externalReferenceCode);
		_objectEntryLocalService.updateObjectEntry(objectEntry);

		return objectEntry;
	}

	private String _getLogoURL(ThemeDisplay themeDisplay) throws Exception {
		String logoURL = StringPool.BLANK;

		Company company = themeDisplay.getCompany();

		if (company.isSiteLogo()) {
			Group group = themeDisplay.getScopeGroup();

			if (group == null) {
				return logoURL;
			}

			logoURL = group.getLogoURL(themeDisplay, false);
		} else {
			logoURL = themeDisplay.getCompanyLogo();
		}

		return _portal.getPortalURL(themeDisplay) + logoURL;
	}

	private long createFolderIfDoesNotExist(String folderName, long groupId, long parentFolderId,
			ActionRequest actionRequest,boolean isAccountFolder) throws PortalException {
		DLFolder folder = _dlFolderLocalService.fetchFolder(groupId, parentFolderId, folderName);
		if (folder != null) {
			return folder.getFolderId();
		} else {
			long folderId = _dlFolderLocalService
					.addFolder(PortalUtil.getUser(actionRequest).getUserId(), groupId,
							groupId, false,
							parentFolderId, folderName, "", false, ServiceContextFactory.getInstance(actionRequest))
					.getFolderId();
			if(isAccountFolder) {
				settingFolderPermission((ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY), String.valueOf(folderId));
			}
			return folderId;
		}
	}
	private void settingFolderPermission(ThemeDisplay themeDisplay,String  primKey) throws PortalException {
		
	    Role accountAdmin=RoleLocalServiceUtil.getRole(themeDisplay.getCompanyId(), "Account Administrator");
	    Role accountMember=RoleLocalServiceUtil.getRole(themeDisplay.getCompanyId(), "Account Member");
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountAdmin.getRoleId()  ,ActionKeys.ACCESS);
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountMember.getRoleId()  ,ActionKeys.ACCESS);
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountAdmin.getRoleId()  ,ActionKeys.VIEW);
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountMember.getRoleId()  ,ActionKeys.VIEW);
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountAdmin.getRoleId()  ,ActionKeys.UPDATE);
		ResourcePermissionLocalServiceUtil.addResourcePermission(themeDisplay.getCompanyId(), DLFolder.class.getName(), (int)themeDisplay.getScopeGroupId(), primKey,accountMember.getRoleId()  ,ActionKeys.UPDATE);

	}
	
	@Reference
	private CommerceChannelService _commerceChannelService;

	@Reference
	private CommerceOrderService _commerceOrderService;

	@Reference
	private CommerceOrderTypeService _commerceOrderTypeService;

	@Reference
	private CommercePriceFormatter _commercePriceFormatter;

	@Reference
	private CommerceReportExporter _commerceReportExporter;

	@Reference
	private CompanyService _companyService;

	@Reference
	private DLAppLocalService _dlAppLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private DLFolderLocalService _dlFolderLocalService;

	@Reference
	private RepositoryProvider repositoryProvider;
	
	@Reference
	private CommerceAccountLocalService accountLocalService;

	@Reference
	private DLValidator _dlValidator;
	@Reference
	private UniqueFileNameProvider _uniqueFileNameProvider;
	@Reference
	private DLAppService _dlAppService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

	@Reference
	private ObjectEntryManager _objectEntryManager;

	@Reference
	private CounterLocalService _counterLocalService;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

}

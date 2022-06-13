<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="javax.portlet.MutableRenderParameters" %>
<%@ page import="com.liferay.portal.kernel.repository.model.FileEntry" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.Charset" %>
<%@ page import="com.liferay.portlet.documentlibrary.lar.FileEntryUtil" %>
<%@ include file="/init.jsp" %>
<%@ include file="/init-custom.jsp" %>

<style>
    .custom-quote-history-container {
        color: #5c5e5e;
        font-size: 14px;
        line-height: 1.58;
        margin: auto;
        max-width: 1440px;
        padding-bottom: 104px;
        width: 100%;
        height: 100%;
        padding: 0px;
        overflow: hidden;
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
    .preview-file-document {
        width: 100%;
        height: 100%;
        border: none;
    }
    .preview_container {
        height: 100%;
    }
</style>

<%

    long fileEntryId = (long)request.getAttribute("fileEntryId");
    FileEntry fileEntry = FileEntryUtil.fetchByPrimaryKey(fileEntryId);

    String downloadUrl = String.format("/documents/%d/%d/%s/%s?t=%d&download=true",
            fileEntry.getRepositoryId(),
            fileEntry.getFolderId(),
            URLEncoder.encode(fileEntry.getFileName(), Charset.forName("UTF-8")),
            fileEntry.getUuid(),
            fileEntry.getModifiedDate().getTime()
    );

    String previewURL = String.format("/documents/%d/%d/%s/%s?t=%d",
            fileEntry.getRepositoryId(),
            fileEntry.getFolderId(),
            URLEncoder.encode(fileEntry.getFileName(), Charset.forName("UTF-8")),
            fileEntry.getUuid(),
            fileEntry.getModifiedDate().getTime()
    );

    String modalTitle = String.format("%s",
//            fileEntry.getRepositoryId(),
//            fileEntry.getFolderId(),
            fileEntry.getFileName()
    );
%>

<script>
    window.addEventListener("load", function() {
        let pfs = window.parent.frames;
        for(var i=0;i<pfs.length;i++) {
            if(pfs[i] == window) {
                let modal = window.parent.document.querySelector('[name="' + pfs[i].name + '"]').closest('.modal');
                modal.querySelector('.modal-title').textContent = "<%=modalTitle%>";
            }
        }
    });
</script>

<div class="custom-quote-history-container">

    <div class="preview_container">
        <iframe class="preview-file-document preview-file-document-fit" src="<%=previewURL%>" >
        </iframe>
    </div>


</div>
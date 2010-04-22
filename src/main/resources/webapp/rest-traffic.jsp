<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<html>
<head>
<title>${requestScope.config.serverName}</title>
<style type="text/css" media="screen"><!--
tr.odd {background: #DDF;}
--></style>
</head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">${requestScope.config.serverName}</span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/listUsers.jsp">Users &amp; Sessions</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/traffic.jsp">SSO Traffic</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #EEF; padding: 3 8 5 8px;"><a href="/admin/rest-traffic.jsp">Rest Traffic</a></span>
 <!-- 
 take out apps tab until we fix to render site matchers and nested app-end-points accordingly 
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
  -->
</div>
<!-- TABS END -->
<div style="padding: 0 10 10 10px;">
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Watch Rest traffic. Refresh the browser to view captured traffic.</div>

<div style="font-size: medium; padding: 3px">
<span style="font-weight: bold;">Traffic:</span>
<span style="padding: 0 5 0 10px;">
<c:choose>
 <c:when test="${requestScope.config.trafficRecorder.recordingRest}">
  <a href="/admin/action/recording/stop-rest">Stop Recording</a>
 </c:when>
 <c:otherwise><a href="/admin/action/recording/start-rest">Start Recording</a></c:otherwise>
</c:choose>
</span>
<span style="padding: 0 5 0 5px;"><a href="/admin/action/recording/clear-rest">Clear</a></span>
</div>

<table>
<c:forEach items="${requestScope.config.trafficRecorder.restHits}" var="rhit" varStatus="loop">
<c:set var="rowType" scope="page"><c:choose><c:when test="${loop.count mod 2 == 1}">odd</c:when><c:otherwise>even</c:otherwise></c:choose></c:set>
<tr class="${rowType}">
<td style="vertical-align: top;"><span style="padding-right: 8">${rhit.path}</span></td> 
<td>
<table cellpadding="0" cellspacing="0">
<tr><td style="vertical-align: top;">
<span style="font-style: italic; color: green;">response:</span>
<span style="font-weight: bold; padding-right: 4">
<c:choose>
<c:when test="${rhit.code < 300}"><span style="color: #3A3;">${rhit.code}</span></c:when>
<c:when test="${rhit.code >= 300 && rhit.code < 400}"><span style="color: gray;">${rhit.code}</span></c:when>
<c:when test="${rhit.code != 404 && (rhit.code >= 400 && rhit.code < 500)}"><span style="color: purple;">${rhit.code}</span></c:when>
<c:when test="${rhit.code >= 500 || rhit.code == 404}"><span style="color: red;">${rhit.code}</span></c:when>
</c:choose></span>
</td><td>${jsputils.crlfToBr[rhit.response]}</td>
</tr>
<c:forEach items="${rhit.properties}" var="entry">
<tr><td><span style="float: right; font-style: italic; color: green;">${entry.key}:</span></td><td><span style="padding-left: 5; padding-right: 4">${entry.value}</span></td></tr>
</c:forEach>
</table>
</td>
</tr>
</c:forEach>
</table>
</div>
</body>
</html>
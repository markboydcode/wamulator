<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head><title>${requestScope.config.serverName}</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">${requestScope.config.serverName}</span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/listUsers.jsp">Users &amp; Sessions</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #EEF; padding: 3 8 5 8px;"><a href="/admin/traffic.jsp">SSO Traffic</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/rest-traffic.jsp">Rest Traffic</a></span>
 <!-- 
 take out apps tab until we fix to render site matchers and nested app-end-points accordingly 
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
  -->
</div>
<!-- TABS END -->
<div style="padding: 0 10 10 10px;">
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Watch SSO traffic. Refresh the browser to view the last ${requestScope.config.maxEntries} requests captured.</div>

<div style="font-size: medium; padding: 3px">
<span style="font-weight: bold;">SSO Traffic:</span>
<span style="padding: 0 5 0 10px;">
<c:choose>
 <c:when test="${requestScope.config.trafficRecorder.recording}">
  <a href="/admin/action/recording/stop">Stop Recording</a>
 </c:when>
 <c:otherwise><a href="/admin/action/recording/start">Start Recording</a></c:otherwise>
</c:choose>
</span>
<span style="padding: 0 5 0 5px;"><a href="/admin/action/recording/clear">Clear</a></span>
</div>

<table>
<c:forEach items="${requestScope.config.trafficRecorder.timestampSortedHits}" var="hit">
<tr>
<td style='white-space: nowrap;'>${hit.longTimestamp}</td>
<td style='white-space: nowrap;'>${hit.connId}</td>
<td>${hit.username}</td>
<td><c:choose><c:when test="${hit.isProxyCode}"><span title="Response code made by proxy" style="color: blue; cursor: default;">P</span></c:when><c:otherwise>-</c:otherwise></c:choose></td>
<td><span style="font-weight: bold">
<c:choose>
<c:when test="${hit.code < 300}"><span style="color: #3A3;">${hit.code}</span></c:when>
<c:when test="${hit.code >= 300 && hit.code < 400}"><span style="color: gray;">${hit.code}</span></c:when>
<c:when test="${hit.code != 404 && (hit.code >= 400 && hit.code < 500)}"><span style="color: purple;">${hit.code}</span></c:when>
<c:when test="${hit.code >= 500 || hit.code == 404}"><span style="color: red;">${hit.code}</span></c:when>
</c:choose></span></td>
<td>${hit.method}</td>
<c:choose>
    <c:when test="${requestScope.config.debugLoggingEnabled}"><td><span style="color: blue"><a href="logs/${hit.connId}.log" target='?newtab?'>${hit.uri}</a></span></td></c:when>
    <c:otherwise><td><span style="color: blue">${hit.uri}</span></td></c:otherwise>

</c:choose>
</tr>
</c:forEach>
</table>
</div>
</body>
</html>
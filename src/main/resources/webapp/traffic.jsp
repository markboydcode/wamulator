<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head><title>Console: ${requestScope.config.serverName}</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">Console: ${requestScope.config.serverName}</span>
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
<td title="request timestamp" style='white-space: nowrap; cursor: default;'>${hit.timestamp}</td>
<td title="simulator connection id" style='white-space: nowrap; cursor: default;'>${hit.connId}</td>
<td title="user" style='white-space: nowrap; cursor: default;'><c:choose>
<c:when test="${hit.username == '???'}"><span title="no simulator cookie" style="cursor: default;">${hit.username}</span></c:when><c:otherwise><span title="cookie user" style="cursor: default;">${hit.username}</span></c:otherwise></c:choose></td>
<td><c:choose><c:when test="${hit.isProxyCode}"><span title="simulator response code" style="color: blue; cursor: default;">P</span></c:when><c:otherwise><span title="server response code" style="color: black; cursor: default;">-</span></c:otherwise></c:choose></td>
<td><c:choose><c:when test="${hit.trafficType == '?'}"><span title="unclassified traffic" style="color: blue; cursor: default;">?</span></c:when><c:when test="${hit.trafficType == '!'}"><span title="non by-site traffic" style="color: red; cursor: default;">!</span></c:when><c:otherwise><span title="by-site traffic" style="color: black; cursor: default;">-</span></c:otherwise></c:choose></td>
<td><span style="font-weight: bold">
<c:choose>
<c:when test="${hit.code < 300}"><span title='${hit.httpMsg}' style="color: #3A3; cursor: default;">${hit.code}</span></c:when>
<c:when test="${hit.code >= 300 && hit.code < 400}"><span title='${hit.httpMsg}' style="color: gray; cursor: default;">${hit.code}</span></c:when>
<c:when test="${hit.code != 404 && (hit.code >= 400 && hit.code < 500)}"><span title='${hit.httpMsg}' style="color: purple; cursor: default;">${hit.code}</span></c:when>
<c:when test="${hit.code >= 500 || hit.code == 404}"><span title='${hit.httpMsg}' style="color: red; cursor: default;">${hit.code}</span></c:when>
</c:choose></span></td>
<td title="http method" style='cursor: default;'>${hit.method}</td>
<td title="host header" style='cursor: default;'>${hit.hostHdr}</td>
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
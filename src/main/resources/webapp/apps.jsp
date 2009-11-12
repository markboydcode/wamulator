<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">SSO App Wrap Shim</span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/selectUser.jsp">Users &amp; Sessions</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/traffic.jsp">Traffic</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #EEF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
</div>
<!-- TABS END -->
<div style="padding: 0 10 10 10px;">
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Registered Applications</div>
<div style="font-size: medium; padding: 3px"><span style="font-weight: bold;">URL Transformations:</span></div>
<table cellspacing="0" cellpadding="0" style="padding: 8px 0 0 0">
<tr>
<th><span style="padding: 0 0 0 3px;">Canonical</span></th>
<th><span style="padding: 0 0 0 10px;">Application</span></th>
<th><span style="padding: 0 0 0 10px;">Port</span></th>
</tr>
<c:forEach items="${requestScope.config.trafficManager.applications}" var="app">
<tr>
<td><span style="padding: 0 0 0 3px;">${app.canonicalContextRoot}</span</td>
<td><span style="padding: 0 0 0 10px;">${app.applicationContextRoot}</span></td>
<td><span style="padding: 0 0 0 10px;">${app.endpointPort}</span></td>
</tr>
</c:forEach>
</table>

<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Un-Enforced URLs</div>
<table cellspacing="0" cellpadding="0" style="padding: 8px 0 0 0">
<c:forEach items="${requestScope.config.appManager.unenforcedUrls}" var="url">
<tr>
<td><span style="padding: 0 0 0 3px;">${url}</span</td>
</tr>
</c:forEach>
</table>
</div>
</body>
</html>
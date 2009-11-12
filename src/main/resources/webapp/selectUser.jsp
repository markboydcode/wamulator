<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<c:set var="gotoQueryParm" scope="page" value=""/>
<c:set var="gotoUriEnc" scope="page" value=""/>
<c:if test="${not empty(param.goto)}">
<c:set var="gotoUriEnc" scope="page" value="${param.goto}"/>
<c:set var="gotoQueryParm" scope="page" value="?goto=${jsputils.encode[param.goto]}"/>
</c:if>
<html>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">SSO App Wrap Shim</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;">
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px"><span>Select a user to start their session</span>
<span style="padding: 0 0 0 10px"><c:if test="${not empty(gotoUriEnc)}"><a href="${gotoUriEnc}">Return to ${gotoUriEnc}</a></c:if></span>
</div>
<table><tr><td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Users:</div>
<table>
<c:forEach items="${requestScope.config.userManager.users}" var="user">
<tr><td><c:if test="${user.username == requestScope.currentUserName}"><IMG src="pointer.png"/></c:if></td>
<td><a href="/ui/set-user/${user.username}${gotoQueryParm}">${user.username}</a></td></tr>
</c:forEach>
</table>
</td>
<td valign="top">
<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Headers Injected</div>
<table>
<c:forEach items="${requestScope.currentUser.headers}" var="hdr">
<tr><td><span style="padding: 0 5px 0 20px;">${hdr.name}:</span></td><td>${hdr.value}</td></tr>
</c:forEach>
</table>
</td>
<td valign="top">
<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Permitted URIs</div>
<table>
<c:forEach items="${requestScope.currentUser.permissions}" var="perm">
<tr><td><span style="padding: 0 5px 0 20px;">${perm.value}</span></td><td>${perm.name}</td></tr>
</c:forEach>
</table>
</td></tr></table>
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Select a session to hijack and return to that user's session</div>
<div style="font-size: medium; font-weight: bold; padding: 3px">Active Sessions:</div>
<table>
<c:forEach items="${requestScope.config.sessionManager.sessions}" var="session">
<tr><td><c:if test="${session.token == requestScope.currentToken}"><IMG src="pointer.png"/></c:if></td>
<td><a href="/ui/set-session/${session.token}">${session.token}</a></td>
<td>${session.remainingSeconds}</td>
<td><a href="/ui/terminate-session/${session.token}"><img src="delete.gif" style="border: none"/></a></td></tr>
</c:forEach>
</table>
</div>
</body>
</html>
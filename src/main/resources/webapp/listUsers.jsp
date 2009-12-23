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
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">SSO App Wrap Shim</span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #EEF; padding: 3 8 5 8px;"><a href="listUsers.jsp">Users &amp; Sessions</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="traffic.jsp">SSO Traffic</a></span>
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/rest-traffic.jsp">Rest Traffic</a></span>
 <!-- 
 take out apps tab until we fix to render site matchers and nested app-end-points accordingly 
 <span style="padding-right: 10px"> </span>
 <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
  -->
</div>
<!-- TABS END -->
<div style="padding: 10 10 10 10px;">
<table><tr><td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3 3 3 0px">Users:</div>
<table>
<c:forEach items="${requestScope.config.userManager.users}" var="user">
<tr><td><a href="/ui/set-user/${user.username}${gotoQueryParm}">${user.username}</a></td></tr>
</c:forEach>
</table>
</td>
<td valign="top">
<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Headers Injected</div>
<!-- change to dynamically inject here via json ajax with jquery -->
<table>
<c:forEach items="${requestScope.currentUser.headers}" var="hdr">
<tr><td><span style="padding: 0 5px 0 20px;">${hdr.name}:</span></td><td>${hdr.value}</td></tr>
</c:forEach>
</table>
<!-- end of proposed change to dynamically inject here via json ajax -->
</td>
</tr></table>
<div style="font-size: medium; font-weight: bold; padding: 6 3 3 3px">Active Sessions:</div><table>
<c:forEach items="${requestScope.config.sessionManager.sessions}" var="session">
<tr><td>${session.token}</td>
<td>${session.remainingSeconds}</td>
<td><a href="/ui/terminate-session/${session.token}"><img src="delete.gif" style="border: none"/></a></td></tr>
</c:forEach>
</table>
</div>
</body>
</html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<html>
<head><title>Console: ${requestScope.config.serverName}</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- TABS -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">Console: ${requestScope.config.serverName}</span>
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
<tr>
<td><c:if test="${user.username == requestScope.selectedUserName}"><IMG src="pointer.png"/></c:if></td>
<td><a href="?username=${user.username}">${user.username}</a></td>
</tr>
</c:forEach>
</table>
</td>
<td valign="top">
<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Headers Injected</div>
<!-- change to dynamically inject here via json ajax with jquery -->
<table>
<c:forEach items="${requestScope.selectedUser.headers}" var="hdr">
<tr><td><c:choose><c:when test="${jsputils.isSsoDefinedHeader[hdr.name]}"><span style="padding: 0 5px 0 20px;">${hdr.name}:</span></c:when><c:otherwise><span title='Not an SSO Injected Header' style="padding: 0 5px 0 20px; background-color: rgb(255,180,180)">${hdr.name}:</span></c:otherwise></c:choose></td><td><c:out value='${hdr.value}'/></td></tr>
</c:forEach>
</table>
<!-- end of proposed change to dynamically inject here via json ajax -->
<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Attributes</div>
<table>
<c:forEach items="${requestScope.selectedUser.attributes}" var="att">
<tr><td><span style="padding: 0 5px 0 20px;">${att.name}:</span></td><td><c:out value='${att.value}'/></td></tr>
</c:forEach>
</table>
</td>
</tr></table>
<div style="font-size: medium; font-weight: bold; padding: 6 3 3 3px">Active Sessions:</div>
<table>
<c:forEach items="${requestScope.config.sessionManager.cookieDomains}" var="domain">
<tr><td>&nbsp;</td>
<td>Domain:</td>
<td> <strong>${domain}</strong></td>
</tr>
<c:forEach items="${jsputils.domainSessions[domain]}" var="session">
<tr><td><c:if test="${session.token == requestScope.currentToken}"><IMG src="pointer.png"/></c:if></td>
<td><a href="/admin/action/set-session/${session.token}">${session.token}</a></td>
<td>${session.remainingSeconds}</td>
</tr>
</c:forEach>
</c:forEach>
</table>
</div>
</body>
</html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<c:set var="gotoQueryParm" scope="page" value=""/>
<c:set var="gotoUriEnc" scope="page" value=""/>
<c:if test="${not empty(param.goto)}">
<c:set var="gotoUriEnc" scope="page" value="${param.goto}"/>
<c:set var="gotoQueryParm" scope="page" value="?goto=${jsputils.encode[param.goto]}"/>
</c:if>
<html>
<head><title>Console: ${requestScope.config.serverName}</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">Console: ${requestScope.config.serverName}</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;">
<div style="font-style: italic; color: green; padding: 12px 3px 10px 3px">
<div>Select a user to start their session or...</div>
<div>Enter username and password if you like typing. :-)</div>
<span><c:if test="${not empty(gotoUriEnc)}"><a href="${gotoUriEnc}">Return to ${gotoUriEnc}</a></c:if></span>
</div>
<table border='0' padding='0' cellpadding='0'>
 <tr><td style='vertical-align: top;'>
<form id='loginForm' action='/admin/action/set-user' method='post' >
 <div id="login">
  <fieldset>
   <dl>
    <dt><label for="username">User Name</label></dt>
    <dd>
     <input type="text" name="username" value="" id="username" tabindex="1" autofocus />
    </dd>
   </dl>
   <dl>
    <dt><label for="password">Password</label></dt>
    <dd>
     <input type="password" name="password" value="" id="password" tabindex="2"/>
    </dd>
   </dl>
   <dl>
    <dd><input class="button" type="submit" name="" value="Sign in" id="submit" tabindex="3"/></dd>
   </dl>
  </fieldset>
 </div>
</form>
</td><td style='vertical-align: top;'>
<table><tr><td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Users:</div>
<table>
<c:forEach items="${requestScope.config.userManager.users}" var="user">
<tr><td><c:if test="${user.username == requestScope.currentUserName}"><IMG src="pointer.png"/></c:if></td>
<td><a href="/admin/action/set-user/${user.username}${gotoQueryParm}">${user.username}</a></td></tr>
</c:forEach>
</table>
</td>
</tr></table>
</td>
</tr>
</table>
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Select a session to hijack and return to that user's session</div>
<div style="font-size: medium; font-weight: bold; padding: 3px">Active Sessions:</div>
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
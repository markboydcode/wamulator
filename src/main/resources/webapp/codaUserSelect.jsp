<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html; charset=utf-8" %>
<%@ page session="false" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<c:set var="gotoQueryParm" scope="page" value=""/>
<c:set var="gotoUriEnc" scope="page" value=""/>
<c:if test="${not empty(param.goto)}">
<c:set var="gotoUriEnc" scope="page" value="${param.goto}"/>
<c:set var="gotoQueryParm" scope="page" value="${jsputils.encode[param.goto]}"/>
</c:if>
<html>
<head>
<title>Console: ${requestScope.config.serverName}</title>
<script type="text/javascript">

  window.signin = function(username) {
	  document.signinForm.username.value = username;
	  document.signinForm.submit();
  };
</script>

</head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">Console: ${requestScope.config.serverName}</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;  text-align:left;">
<form name="signinForm" action="/auth/ui/authenticate" method="post">
    <input type="hidden" name="goto" value="${gotoQueryParm}"/>
<c:if test="${param['page-error'] == 'user-not-found'}">
<span style="padding; background:#EFDFDF none repeat scroll 0 0; border:1px solid #E0ACA6; 
color:#C23232; display:block; font-family:'Lucida Grande','Lucida Sans Unicode',sans-serif; 
font-size:12px; margin-bottom:0; margin-top:10px; width: 250px;
padding:15px 20px; text-align:left;">The username you provided is not valid.</span>
</c:if>
<c:if test="${param['page-error'] == 'no-user-source-specified'}">
<span style="padding; background:#EFDFDF none repeat scroll 0 0; border:1px solid #E0ACA6; 
color:#C23232; display:block; font-family:'Lucida Grande','Lucida Sans Unicode',sans-serif; 
font-size:12px; margin-bottom:0; margin-top:10px; width: 250px;
padding:15px 20px; text-align:left;">For this sign-in page a <i>source</i> attribute must be specified for the <i>&lt;users&gt;</i> configuration element.</span>
</c:if>
<c:if test="${param['page-error'] == 'error-accessing-ext-source'}">
<span style="padding; background:#EFDFDF none repeat scroll 0 0; border:1px solid #E0ACA6; 
color:#C23232; display:block; font-family:'Lucida Grande','Lucida Sans Unicode',sans-serif; 
font-size:12px; margin-bottom:0; margin-top:10px; width: 250px;
padding:15px 20px; text-align:left;">Unable to access user source. See log for details.</span>
</c:if>

        <p>
            <label for="username">Coda Username</label>
            <input type="text" name="username" id="username" tabindex="1" autocomplete="on"/>
        </p>
        <p>
            <input type="submit" name="submitBtn" id="submitBtn" value="Sign In" tabindex="2"/>
        </p>
</form>
<div style="font-style: italic; color: green; padding: 12px 3px 3px 3px">Select a user to start their session</div>
<table><tr><td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Users:</div>
<table>
<c:forEach items="${requestScope.config.userManager.users}" var="user">
<tr><td><c:if test="${user.username == requestScope.currentUserName}"><IMG src="pointer.png"/></c:if></td>
<td><a onclick="signin('${user.username}'); return false;" href="#">${user.username}</a></td></tr>
</c:forEach>
</table>
</td>
</tr></table>
</div>
</div>
</body>
</html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<c:set var="gotoQueryParm" scope="page" value=""/>
<c:set var="gotoUriEnc" scope="page" value=""/>
<html>
<head><title>${requestScope.config.serverName}</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">${requestScope.config.serverName}</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;  text-align:left;">
<form name="signinForm" action="action/set-user" method="post">
    <input type="hidden" name="goto" value="${param.goto}"/>
<span style="padding; background:#EFDFDF none repeat scroll 0 0; border:1px solid #E0ACA6; 
color:#C23232; display:block; font-family:'Lucida Grande','Lucida Sans Unicode',sans-serif; 
font-size:12px; margin-bottom:0; margin-top:10px; width: 250px;
padding:15px 20px; text-align:left;">The username you provided is not valid.</span>
        <p>
            <label for="username">Coda Username</label>
            <input type="text" name="coda-user" id="username" tabindex="1" autocomplete="on"/>
        </p>
        <p>
            <input type="submit" name="submit" id="submit" value="Sign In" tabindex="2"/>
        </p>
</form>
</div>
</body>
</html>
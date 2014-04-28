<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%
    response.addHeader("Cache-Control", "no-cache");
    response.addHeader("Cache-Control", "no-store");
    response.addHeader("Cache-Control", "must-revalidate");
    response.addHeader("Cache-Control", "private");
    response.addHeader("Pragma", "no-cache");
%>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils"/>
<c:set var="gotoQueryParm" scope="page" value=""/>
<c:set var="gotoUriEnc" scope="page" value=""/>
<c:if test="${not empty(param['goto'])}">
	<c:set var="gotoUriEnc" scope="page" value="${param['goto']}"/>
	<c:set var="gotoQueryParm" scope="page" value="?goto=${jsputils.encode[param['goto']]}"/>
</c:if>
<c:set var="formAction" scope="page" value="/admin/action/set-user"/>
<c:if test="${not empty(requestScope.config.loginAction)}">
	<c:set var="formAction" scope="page" value="${requestScope.config.loginAction}"/>
</c:if>


<!DOCTYPE html>
<html>
<head>
    <title>Sign in</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge" /> <!-- Make IE play nice -->
    <meta name="viewport" content="width=device-width, user-scalable=yes"/>
    <link rel="stylesheet" href="css/project.css?v=2_2" type="text/css" media="all"/>
    <!--[if lt IE 9]><link rel="stylesheet" href="css/project-ie.css?v=2_2" type="text/css" media="all"/><![endif]-->

	<script src="javascript/jquery-1.8.2.min.js" type="text/javascript" charset="utf-8"></script>
    <script src="javascript/page.js?v=2_2" type="text/javascript" charset="utf-8"></script>
</head>

<body>
    <div class="header-container">
        <div class="header">
            <div id="logo" lang="en" title="The Church of Jesus Christ of Latter-day Saints"></div>
            <div class="app-logo" title="WAMulator - Single Sign On">
                <div class="app-logo-main">WAMulator</div>
                <div class="app-logo-sub">SINGLE SIGN ON</div>
            </div>
        </div>
    </div>

	<div class="centeredContent" style="z-index: 100;">
		<div id="signin-header">Sign in</div>
		<div id="language-header">
	    	<div id="languageMenu">
				<div id="changeLanguageDrop" class="normal rounded-corner-TL rounded-corner-TR" >
				    <span style="display: inline-block; text-indent: 10em;  overflow: hidden; width: 16px; height: 16px; top: 2px; position: relative; background:url('images/ico-language-16.png') no-repeat 0 0;">&nbsp;</span>&nbsp;<c:out value="English" />&nbsp;<span id="downarrow" style=" text-indent: 10em;  overflow: hidden; width: 10px; height: 10px; display: inline-block; position: relative; top: 2px;">&nbsp;</span>
				</div>
				
				<div id="languageModal" class="gone rounded-corner-TL rounded-corner-BL rounded-corner-BR" role="alert" aria-atomic="false" aria-live="polite" aria-hidden="false" aira-describedby="???TODO???">	
				</div>
			</div>
		</div>
	</div>

	<div class="centeredContent" style="z-index: 1700;">
		<div id="error-header">
			<div id="errorToolTip">
				<div id="cancelError" title="Close">
					<div id="cancelErrorImage" title="Close"></div>
				</div>
                <div id="emptyCredsMsg" style="display:none;">
                    Your user name or password is incorrect. Please ensure that you are entering the correct information.
                </div>
		        <div id="scriptValidationErroMsg"><div id="scriptValidationErroMsgPadding"><div id="scriptValidationErroMsgText">
			        <c:choose>
				    	<c:when test="${param['page-error'] == 'user-not-found'}">The username you provided is not valid.</c:when>
					    <c:when test="${param['page-error'] == 'no-user-source-specified'}">For this sign-in page a <i>source</i> attribute must be specified for the <i>&lt;users&gt;</i> configuration element.</c:when>
					    <c:when test="${param['page-error'] == 'error-accessing-ext-source'}">Unable to access user source. See log for details.</c:when>
					    <c:when test="${param['page-error'] == 'error-must-use-http-post'}">Must use http POST when authenticating.</c:when>
					    <c:when test="${param['page-error'] == 'failed-authentication'}">Incorrect Username or Password.</c:when>
			        </c:choose>
		        </div></div></div>
			</div>
		</div>
	</div>
	
	<div id="contentWrapper">
	    <form id="loginForm" class="form" action="${formAction}${gotoQueryParm}" method="post">
			<div id="content1">
				<div id="login" class="centeredContent">

					<div id="usernameContainer">
						<div class="label-container"><label for="username">LDS Account User Name</label></div>
						<input class="inputText" type="text" name="username" id="username" tabindex="1" />
						<a href="#" class="forgot-link">Forgot your user name?</a>
					</div>
					<div id="passwordContainer">
						<div class="label-container"><label for="password">Password</label></div>
						<input class="inputText" type="password" name="password" id="password" tabindex="2" />
						<a href="#" class="forgot-link">Forgot your password?</a>
					</div>
					<p style="text-align:left;"><strong>Note:</strong> Does not work as direct ldap authentication; only works with users you have configured in the WAMulator.</p>
				</div>
				<span id="closeLegalViewer" class="hidden" title="Close"></span>
				<div id="legalViewer" class="hidden">
					<div style="padding-right: 10px;">
						<div id="rights" class="invisible"></div>
						<div id="privacy" class="invisible"></div>
					</div>
				</div>				
			</div>
			<div id="content2">
				<div class="centeredContent">
					<div id="submitButton" class="formActions">
					    <input class="button" type="submit" id="submit" tabindex="3" value="Sign in" />
					</div>
					<div id="or" class="formActions">
						-&nbsp;or choose a user&nbsp;-
					</div>
					<table id="user-table">
                        <tr>
                            <td colspan="2"><input type="text" placeholder="Filter Users" id="user-filter"/></td>
                        </tr>
						<c:forEach items="${requestScope.config.userManager.users}" var="user">
							<tr>
								<td><c:if test="${user.username == requestScope.currentUserName}"><IMG src="pointer.png"/></c:if></td>
								<td><a href="${formAction}/${user.username}${gotoQueryParm}" class="user">${user.username}</a></td>
							</tr>
						</c:forEach>
					</table>
					<table style="margin:0 auto;">
						<c:forEach items="${requestScope.config.sessionManager.cookieDomains}" var="domain">
							<tr>
								<td>&nbsp;</td>
								<td>Domain:</td>
								<td> <strong>${domain}</strong></td>
							</tr>
							<c:forEach items="${jsputils.domainSessions[domain]}" var="session">
								<tr>
									<td><c:if test="${session.token == requestScope.currentToken}"><IMG src="pointer.png"/></c:if></td>
									<td><a href="/admin/action/set-session/${session.token}">${session.token}</a></td>
									<td>${session.remainingSeconds}</td>
								</tr>
							</c:forEach>
						</c:forEach>
					</table>
		    	</div>
			</div>
	    </form>
	</div>

	<div id="footer">
		<c:choose>
			<c:when test='${empty requestScope.config.consoleTitle}'>Console: ${requestScope.config.serverName}</c:when>
			<c:otherwise>${requestScope.config.consoleTitle}</c:otherwise>
		</c:choose>
		<br/>
		<c:if test="${not empty(gotoUriEnc)}"><a href="${gotoUriEnc}">Return to ${gotoUriEnc}</a></c:if>
		<br/>
		© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> by Intellectual Reserve, Inc. All rights reserved.
	</div>

</body>
</html>
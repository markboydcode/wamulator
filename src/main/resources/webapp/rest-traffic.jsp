<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html; charset=utf-8"%>
<%@ page session="false"%>
<jsp:useBean id="jsputils" scope="application" class="org.lds.sso.appwrap.ui.JspUtils" />
<html>
<head>
	<title>
		<c:choose>
			<c:when test='${empty requestScope.config.consoleTitle}'>Console: ${requestScope.config.serverName}</c:when>
			<c:otherwise>${requestScope.config.consoleTitle}</c:otherwise>
		</c:choose>
	</title>
	<link href="css/console.css" rel="stylesheet" type="text/css"/>
</head>
<body>
	<!-- TABS -->
	<div class="tabs">
		<span class="version">Console: ${requestScope.config.serverName}</span>
		<span class="menu"><a href="/admin/listUsers.jsp">Users &amp; Sessions</a></span>
		<span class="menu"><a href="/admin/traffic.jsp">SSO Traffic</a></span>
		<span class="menu active"><a href="/admin/rest-traffic.jsp">Rest Traffic</a></span>
		<!-- 
 			take out apps tab until we fix to render site matchers and nested app-end-points accordingly 
 			<span style="padding-right: 10px"> </span>
 			<span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
  		-->
	</div>
	<!-- TABS END -->
	<div class="content">
		<div class="info">Active Rest Interfaces and their policy-domains:</div>
		<table border='1'>
			<tr>
				<th>Policy Domain</th>
				<th>Location</th>
			</tr>
			<c:forEach items="${requestScope.config.trafficRecorder.restInstances}" var="rInst">
				<tr>
					<td>${rInst.policyDomain}</td>
					<td><a href="${rInst.cookiePath}">${rInst.resolvedUrlBase}${rInst.policyDomain}/</a></td>
				</tr>
			</c:forEach>
		</table>
		<div class="info">Watch Rest traffic. Refresh the browser to view captured traffic.</div>
		<div class="controls">
			<span class="title">Traffic:</span>
			<span class="itme">
				<c:choose>
					<c:when test="${requestScope.config.trafficRecorder.recordingRest}">
						<a href="/admin/action/recording/stop-rest">Stop Recording</a>
					</c:when>
					<c:otherwise>
						<a href="/admin/action/recording/start-rest">Start Recording</a>
					</c:otherwise>
				</c:choose>
			</span>
			<span class="item"><a href="/admin/action/recording/clear-rest">Clear</a></span>
		</div>

		<table>
			<!-- 
			<tr>
				<th>Timestamp</th>
				<th>Connection ID</th>
				<th>User Cookie</th>
				<th colspan="3">Response Code</th>
				<th>HTTP Method</th>
				<th>HTTP/HTTPS</th>
				<th>Host Header</th>
				<th>HTTP/HTTPS</th>
				<th>Requested Path</th>
			</tr>
			 -->
			<c:forEach items="${requestScope.config.trafficRecorder.restHits}" var="rhit" varStatus="loop">
				<c:set var="rowType" scope="page">
					<c:choose>
						<c:when test="${loop.count mod 2 == 1}">odd</c:when>
						<c:otherwise>even</c:otherwise>
					</c:choose>
				</c:set>
				<tr class="${rowType}">
					<td style="vertical-align: top;"><span style="padding-right: 8">${rhit.path}</span></td>
					<td>
						<table cellpadding="0" cellspacing="0">
							<tr>
								<td style="vertical-align: top;">
									<span style="font-style: italic; color: green;">response:</span>
									<span style="font-weight: bold; padding-right: 4">
										<c:choose>
											<c:when test="${rhit.code < 300}">
												<span style="color: #3A3;">${rhit.code}</span>
											</c:when>
											<c:when test="${rhit.code >= 300 && rhit.code < 400}">
												<span style="color: gray;">${rhit.code}</span>
											</c:when>
											<c:when test="${rhit.code != 404 && (rhit.code >= 400 && rhit.code < 500)}">
												<span style="color: purple;">${rhit.code}</span>
											</c:when>
											<c:when test="${rhit.code >= 500 || rhit.code == 404}">
												<span style="color: red;">${rhit.code}</span>
											</c:when>
										</c:choose>
									</span>
								</td>
								<td>${jsputils.crlfToBr[rhit.response]}</td>
							</tr>
							<c:forEach items="${rhit.properties}" var="entry">
								<tr>
									<td><span style="float: right; font-style: italic; color: green;">${entry.key}:</span></td>
									<td><span style="padding-left: 5; padding-right: 4">${entry.value}</span></td>
								</tr>
							</c:forEach>
						</table>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>
</body>
</html>
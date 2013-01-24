<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html; charset=utf-8"%>
<%@ page session="false"%>
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
		<span class="menu active"><a href="/admin/traffic.jsp">SSO Traffic</a></span>
		<span class="menu"><a href="/admin/rest-traffic.jsp">Rest Traffic</a></span>
		<!-- 
 			take out apps tab until we fix to render site matchers and nested app-end-points accordingly 
 			<span style="padding-right: 10px"> </span>
 			<span class="menu"><a href="/admin/apps.jsp">Applications</a></span>
  		-->
	</div>
	<!-- TABS END -->
	<div class="content">
		<div class="info">Watch SSO traffic. Refresh the browser to view the last ${requestScope.config.maxEntries} requests captured.</div>

		<div class="controls">
			<span class="title">SSO Traffic:</span>
			<span class="item">
				<c:choose>
					<c:when test="${requestScope.config.trafficRecorder.recording}">
						<a href="/admin/action/recording/stop">Stop Recording</a>
					</c:when>
					<c:otherwise>
						<a href="/admin/action/recording/start">Start Recording</a>
					</c:otherwise>
				</c:choose>
			</span>
			<span class="item"><a href="/admin/action/recording/clear">Clear</a></span>
		</div>

		<table>
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
			<c:forEach items="${requestScope.config.trafficRecorder.timestampSortedHits}" var="hit">
				<tr>
					<td title="request timestamp">${hit.timestamp}</td>
					<td title="simulator connection id">${hit.connId}</td>
					<td title="user">
						<c:choose>
							<c:when test="${hit.username == '???'}">
								<span title="no simulator cookie">${hit.username}</span>
							</c:when>
							<c:otherwise>
								<span title="cookie user">${hit.username}</span>
							</c:otherwise>
						</c:choose>
					</td>
					<td>
						<c:choose>
							<c:when test="${hit.isProxyCode}">
								<span title="simulator response code" style="color: blue;">P</span>
							</c:when>
							<c:otherwise>
								<span title="server response code" style="color: black;">-</span>
							</c:otherwise>
						</c:choose>
					</td>
					<td>
						<c:choose>
							<c:when test="${hit.trafficType == '?'}">
								<span title="unclassified traffic" style="color: blue;">?</span>
							</c:when>
							<c:when test="${hit.trafficType == '!'}">
								<span title="non by-site traffic" style="color: red;">!</span>
							</c:when>
							<c:otherwise>
								<span title="by-site traffic" style="color: black;">-</span>
							</c:otherwise>
						</c:choose>
					</td>
					<td>
						<span style="font-weight: bold">
							<c:choose>
								<c:when test="${hit.code < 300}">
									<span title='${hit.httpMsg}' style="color: #3A3;">${hit.code}</span>
								</c:when>
								<c:when test="${hit.code >= 300 && hit.code < 400}">
									<span title='${hit.httpMsg}' style="color: gray;">${hit.code}</span>
								</c:when>
								<c:when test="${hit.code != 404 && (hit.code >= 400 && hit.code < 500)}">
									<span title='${hit.httpMsg}' style="color: purple;">${hit.code}</span>
								</c:when>
								<c:when test="${hit.code >= 500 || hit.code == 404}">
									<span title='${hit.httpMsg}' style="color: red;">${hit.code}</span>
								</c:when>
							</c:choose>
						</span>
					</td>
					<td title="http method" class="center">${hit.method}</td>
					<td title="http/https" class="center">
						<c:choose>
							<c:when test="${hit.clientSecure}">
								<IMG title="https in" src="lock.png" />
							</c:when>
							<c:otherwise>
								<IMG title="http in" src="bullet_white.png" />
							</c:otherwise>
						</c:choose>
					</td>
					<td title="host header">${hit.hostHdr}</td>
					<td title="http/https" class="center">
						<c:choose>
							<c:when test="${hit.serverSecure}">
								<IMG title="https in" src="lock_go.png" />
							</c:when>
							<c:otherwise>
								<IMG title="http in" src="bullet_white.png" />
							</c:otherwise>
						</c:choose>
					</td>
					<td>
						<c:choose>
							<c:when test="${requestScope.config.debugLoggingEnabled}">
								<span><a href="logs/${hit.connId}.log" target='?newtab?'>${hit.uri}</a></span>
							</c:when>
							<c:otherwise>
								<span>${hit.uri}</span>
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>
</body>
</html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html; charset=utf-8" %>
<%@ page session="false" %>
<html>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">${requestScope.config.serverName}</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;">
<table>
<tr>
<td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Method: ${requestScope.method}</div>
</td>
</tr>
<tr>
<td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Headers Seen by Server</div>
<table border='1'>
<c:forEach items="${headerValues}" var="hdrVals">
 <c:forEach items="${hdrVals.value}" var="hdrVal">
<tr><td>${hdrVals.key}</td><td>${hdrVal}</td></tr>
 </c:forEach>
</c:forEach>
</table>
</td>
</tr>
<tr>
<td>
<div style="font-size: medium; font-weight: bold; padding: 3px">Parameters Seen by Server</div>
<table border='1'>
<c:forEach items="${param}" var="prm">
<tr><td>${prm.key}</td><td>${prm.value}</td></tr>
</c:forEach>
</table>
</td>
</tr>
</table>
</div>
</body>
</html>
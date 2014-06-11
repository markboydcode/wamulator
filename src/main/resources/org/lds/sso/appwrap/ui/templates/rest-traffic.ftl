<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page contentType="text/html; charset=utf-8"%>
<%@ page session="false"%>
<html>
<#include "header-fragment.ftl"/>
<body>
<#include "tabs-fragment.ftl"/>
	<div class="content">
		<div class="info">Active Rest Interfaces and their policy-domains:</div>
		<table border='1'>
			<tr>
				<th>Policy Domain</th>
				<th>Location</th>
			</tr>
			<#list config.trafficRecorder.restInstances as rInst>
				<tr>
					<td>${rInst.policyDomain}</td>
					<td><a href="${rInst.cookiePath}">${rInst.resolvedUrlBase}${rInst.policyDomain}/</a></td>
				</tr>
			</#list>
		</table>
		<div class="info">Watch Rest traffic. Refresh the browser to view captured traffic.</div>
		<div class="controls">
			<span class="title">Traffic:</span>
			<span class="itme">
                <#if config.trafficRecorder.recordingRest>
                    <a href="${config.wamulatorServiceUrlBase}/action/recording/stop-rest">Stop Recording</a>
                <#else>
                    <a href="${config.wamulatorServiceUrlBase}/action/recording/start-rest">Start Recording</a>
                </#if>
			</span>
			<span class="item"><a href="${config.wamulatorServiceUrlBase}/action/recording/clear-rest">Clear</a></span>
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
			<#list config.trafficRecorder.restHits as rhit>
                <#if item_index % 2 == 1>
                    <#assign rowType="odd"/>
                <#else>
                    <#assign rowType="even"/>
                </#if>
				<tr class="${rowType}">
					<td style="vertical-align: top;"><span style="padding-right: 8">${rhit.path}</span></td>
					<td>
						<table cellpadding="0" cellspacing="0">
							<tr>
								<td style="vertical-align: top;">
									<span style="font-style: italic; color: green;">response:</span>
									<span style="font-weight: bold; padding-right: 4">
                                        <#if rhit.code < 300>
                                            <span style="color: #3A3;">${rhit.code}</span>
                                        <#elseif (rhit.code >= 300) && (rhit.code < 400)>
                                            <span style="color: gray;">${rhit.code}</span>
                                        <#elseif (rhit.code != 404) && (rhit.code >= 400) && (rhit.code < 500)>
                                            <span style="color: purple;">${rhit.code}</span>
                                        <#elseif (rhit.code >= 500) || (rhit.code == 404)>
                                            <span style="color: red;">${rhit.code}</span>
                                        </#if>
									</span>
								</td>
								<td>${jsputils.crlfToBr[rhit.response]}</td>
							</tr>
							<#list rhit.properties as entry>
								<tr>
									<td><span style="float: right; font-style: italic; color: green;">${entry.key}:</span></td>
									<td><span style="padding-left: 5; padding-right: 4">${entry.value}</span></td>
								</tr>
							</#list>
						</table>
					</td>
				</tr>
			</#list>
		</table>
	</div>
</body>
</html>
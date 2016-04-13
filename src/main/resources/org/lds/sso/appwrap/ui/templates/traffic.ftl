<html>
<#include "header-fragment.ftl"/>
<body>
<#include "tabs-fragment.ftl"/>
	<div class="content">
		<div class="info">Watch SSO traffic. Refresh the browser to view the last ${config.maxEntries} requests captured.</div>

		<div class="controls">
			<span class="title">SSO Traffic:</span>
			<span class="item">
                <#if config.trafficRecorder.recording>
                    <a href="${config.wamulatorServiceUrlBase}/action/recording/stop">Stop Recording</a>
                <#else>
                    <a href="${config.wamulatorServiceUrlBase}/action/recording/start">Start Recording</a>
                </#if>
			</span>
			<span class="item"><a href="${config.wamulatorServiceUrlBase}/action/recording/clear">Clear</a></span>
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
			<#list config.trafficRecorder.timestampSortedHits as hit>
				<tr>
					<td title="request timestamp">${hit.timestamp}</td>
					<td title="simulator connection id">${hit.connId}</td>
					<td title="user">
                        <#if hit.username == '???'>
                            <span title="no simulator cookie">${hit.username}</span>
                        <#else>
                            <span title="cookie user">${hit.username}</span>
                        </#if>
					</td>
					<td>
                        <#if hit.isProxyCode()>
                            <span title="simulator response code" style="color: blue;">P</span>
                        <#else>
                            <span title="server response code" style="color: black;">-</span>
                        </#if>
					</td>
					<td>
                        <#if hit.trafficType == '?'>
                            <span title="unclassified traffic" style="color: blue;">?</span>
                        <#elseif hit.trafficType == '!'>
                            <span title="non by-site traffic" style="color: red;">!</span>
                        <#else>
                            <span title="by-site traffic" style="color: black;">-</span>
                        </#if>
					</td>
					<td>
						<span style="font-weight: bold">
                            <#if hit.code < 300>
                                <span title='${hit.httpMsg}' style="color: #3A3;">${hit.code}</span>
                            <#elseif (hit.code >= 300) && (hit.code < 400)>
                                <span title='${hit.httpMsg}' style="color: gray;">${hit.code}</span>
                            <#elseif (hit.code != 404) && (hit.code >= 400 && hit.code < 500)>
                                <span title='${hit.httpMsg}' style="color: purple;">${hit.code}</span>
                            <#elseif (hit.code >= 500) || (hit.code == 404)>
                                <span title='${hit.httpMsg}' style="color: red;">${hit.code}</span>
                            </#if>
						</span>
					</td>
					<td title="http method" class="center">${hit.method}</td>
					<td title="http/https" class="center">
                        <#if hit.clientSecure>
                            <IMG title="https in" src="lock.png" />
                        <#else>
                            <IMG title="http in" src="bullet_white.png" />
                        </#if>
					</td>
					<td title="host header">${hit.hostHdr!"n/a"}</td>
					<td title="http/https" class="center">
                        <#if hit.serverSecure>
                            <IMG title="https in" src="lock_go.png" />
                        <#else>
                            <IMG title="http in" src="bullet_white.png" />
                        </#if>
					</td>
					<td>
                        <#if config.debugLoggingEnabled>
                            <span><a href="${config.wamulatorServiceUrlBase}/logs/${hit.connId!"n/a"}.log" target='?newtab?'>${hit.uri!"n/a"}</a></span>
                        <#else>
                            <span>${hit.uri!"n/a"}</span>
                        </#if>
					</td>
				</tr>
			</#list>
		</table>
	</div>
</body>
</html>
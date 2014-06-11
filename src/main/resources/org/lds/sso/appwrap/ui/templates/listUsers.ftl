<html>
<#include "header-fragment.ftl"/>
<body>
    <#include "tabs-fragment.ftl"/>
	<div class="content">
		<table>
			<tr>
				<td valign="top">
					<div style="font-size: medium; font-weight: bold; padding: 3 3 3 0px">Users:</div>
					<table>
						<#list config.userManager.users as user>
							<tr>
								<td>
                                    <#if (user.username == selectedUserName!"")>
										<IMG src="pointer.png" />
									</#if>
								</td>
								<td><a href="?username=${user.username}">${user.username}</a></td>
							</tr>
						</#list>
					</table>
				</td>
				<td valign="top">
					<div style="font-style: italic; color: green; padding: 3px 3px 3px 20px">Attributes</div>
                    <#if selectedUser??>
                    <table>
						<#list selectedUser.attributes as att>
							<tr>
								<td><span style="padding: 0 5px 0 20px;">${att.name}:</span></td>
								<td>${att.value}</td>
							</tr>
						</#list>
					</table>
                    </#if>
				</td>
			</tr>
		</table>
		<div style="font-size: medium; font-weight: bold; padding: 6 3 3 3px">Active Sessions:</div>
		<table>
			<#list config.sessionManager.cookieDomains as domain>
				<tr>
					<td>&nbsp;</td>
					<td>Domain:</td>
					<td><strong>${domain}</strong></td>
				</tr>
				<#list jsputils.domainSessions["${domain}"] as session>
					<tr>
						<td><#if (session.token == currentToken)??>
								<IMG src="pointer.png" />
							</#if></td>
						<td><a href="${config.wamulatorServiceUrlBase}/action/set-session/${session.token}">${session.token}</a></td>
						<td>${session.remainingSeconds}</td>
						<td><a href="${config.wamulatorServiceUrlBase}/action/terminate-session/${session.token}"><img src="delete.gif" style="border: none" /></a></td>
					</tr>
				</#list>
			</#list>
		</table>
	</div>
</body>
</html>
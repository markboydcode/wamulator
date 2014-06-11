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
				    <span style="display: inline-block; text-indent: 10em;  overflow: hidden; width: 16px; height: 16px; top: 2px; position: relative; background:url('images/ico-language-16.png') no-repeat 0 0;">&nbsp;</span>&nbsp;English&nbsp;<span id="downarrow" style=" text-indent: 10em;  overflow: hidden; width: 10px; height: 10px; display: inline-block; position: relative; top: 2px;">&nbsp;</span>
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
                    <#if pageError == 'user-not-found'>The username you provided is not valid.
                    <#elseif pageError == 'no-user-source-specified'>For this sign-in page a <i>source</i> attribute must be specified for the <i>&lt;users&gt;</i> configuration element.
                    <#elseif pageError == 'error-accessing-ext-source'>Unable to access user source. See log for details.
                    <#elseif pageError == 'error-must-use-http-post'>Must use http POST when authenticating.
                    <#elseif pageError == 'failed-authentication'>Incorrect Username or Password.
                    </#if>
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
					<p style="text-align:left;"><strong>Note:</strong> Does not work as direct ldap authentication; only works with user stores configured in the WAMulator.</p>
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
						<#list config.userManager.users as user>
							<tr>
								<td><#if user.username == currentUserName!""><IMG src="pointer.png"/></#if></td>
								<td><a href="${formAction}/${user.username}${gotoQueryParm}" class="user">${user.username}</a></td>
							</tr>
						</#list>
					</table>
					<table style="margin:0 auto;">
						<#list config.sessionManager.cookieDomains as domain>
							<tr>
								<td>&nbsp;</td>
								<td>Domain:</td>
								<td> <strong>${domain}</strong></td>
							</tr>
							<#list jsputils.domainSessions[domain] as session>
								<tr>
									<td><#if session.token == currentToken!""><IMG src="pointer.png"/></#if></td>
									<td><a href="/admin/action/set-session/${session.token}">${session.token}</a></td>
									<td>${session.remainingSeconds}</td>
								</tr>
							</#list>
						</#list>
					</table>
		    	</div>
			</div>
	    </form>
	</div>

	<div id="footer">
		${consoleTitle}
		<br/>
		<#if gotoUriEnc != ""><a href="${gotoUriEnc}">Return to ${gotoUriEnc}</a></#if>
		<br/>
		© ${currentYear} by Intellectual Reserve, Inc. All rights reserved.
	</div>

</body>
</html>
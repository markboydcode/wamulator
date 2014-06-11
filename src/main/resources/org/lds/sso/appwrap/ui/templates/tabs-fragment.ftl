<!-- TABS -->
<div class="tabs">
    <span class="version">Console: ${config.serverName()}</span>
    <span class="menu <#if page == "list-users">active</#if>"><a href="listUsers">Users &amp; Sessions</a></span>
    <span class="menu <#if page == "sso-traffic">active</#if>"><a href="traffic">SSO Traffic</a></span>
    <span class="menu <#if page == "rest-traffic">active</#if>"><a href="rest-traffic">Rest Traffic</a></span>
    <!--
         take out apps tab until we fix to render site matchers and nested app-end-points accordingly
         <span style="padding-right: 10px"> </span>
         <span style="color: black; background-color: #DDF; padding: 3 8 5 8px;"><a href="/admin/apps.jsp">Applications</a></span>
      -->
</div>
<!-- TABS END -->

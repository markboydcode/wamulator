<html><head><title>Header Helper</title></head>
<body style="background-color: #EEF; margin: 0px; padding: 0px;">
<!-- masthead -->
<div style="background-color: white; padding-left: 15px; padding-top: 10px; padding-bottom: 5px;">
 <span style="color: black; font-weight: bolder; font-size: large;">${config.serverName()}</span>
</div>
<!-- masthead END -->
<div style="padding: 0 10 10 10px;">
<table>
<tr>
<td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Method: ${method}</div>
</td>
</tr>
<tr>
<td  valign="top">
<div style="font-size: medium; font-weight: bold; padding: 3px">Headers Seen by Server</div>
<table border='1'>
<#list headers.keySet() as name>
 <#list headers["${name}"] as hdrVal>
<tr><td>${name}</td><td>${hdrVal}</td></tr>
 </#list>
</#list>
</table>
</td>
</tr>
<tr>
<td>
<div style="font-size: medium; font-weight: bold; padding: 3px">Parameters Seen by Server</div>
<table border='1'>
<#list params.keySet() as parm>
    <#list params["${parm}"] as val>
<tr><td>${parm}</td><td>${val}</td></tr>
    </#list>
</#list>
</table>
</td>
</tr>
</table>
</div>
</body>
</html>
<h1>${jobj.type}</h1>
<h2>Aspects</h2>
<ul>
<#list jobj.aspects as aspect>
    <li>${aspect}</li>
</#list>
</li>
</ul>
<h2>Properties</h2>
<table>
  <tr>
    <th style="{ border: 1px black solid }">Name</th>
    <th style="{ border: 1px black solid }">Type</th>
    <th style="{ border: 1px black solid }">Indexed</th>
    <th style="{ border: 1px black solid }">Mandatory</th>
    <th style="{ border: 1px black solid }">Faceted</th>
  </tr>
<#list jobj.props?keys?sort as key>
  <#assign prop=jobj.props[key] >
  <tr>
    <td style="{ border: 1px black solid }">${key}</td>
    <td style="{ border: 1px black solid }">${prop.Type}</td>
    <td style="{ border: 1px black solid }">${prop.Indexed?string("yes", "no")}</td>
    <td style="{ border: 1px black solid }">${prop.Mandatory?string("yes", "no")}</td>
    <td style="{ border: 1px black solid }">${prop.Faceted}</td>
  </tr>
</#list>  
</table>
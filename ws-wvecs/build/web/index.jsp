<%-- 
    Document   : index
    Created on : 11 Jun, 2017, 2:29:39 AM
    Author     : dwaipayan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Word Vector Lookup</title>
    </head>
    <body>
        <h1>Word Vector Lookup</h1>
        <form action="${pageContext.request.contextPath}/WVecRetrieverServlet">
            Word - <input type="text" value='${requestScope.Word}' > 
            <br>
            Vector - <input type="text" value='${requestScope.Vector}' readonly>
        </form>
    </body>
</html>
